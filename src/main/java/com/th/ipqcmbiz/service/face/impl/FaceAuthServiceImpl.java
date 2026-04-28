package com.th.ipqcmbiz.service.face.impl;

// ========== 导入：统一返回结果格式 ==========

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.FaceFeatureDO;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.FaceEnrollVO;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import com.th.ipqcmbiz.mapper.face.FaceFeatureMapper;
import com.th.ipqcmbiz.service.face.FaceAuthService;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人脸认证服务实现类
 *
 * 核心功能：
 * 1. 人脸录入 - 拍照模式和视频模式两种
 * 2. 人脸识别 - 从视频流或图片识别人脸
 * 3. 质量评估 - 评估人脸图片的光照、对比度等
 *
 * 使用模型：
 * - YuNet: 人脸检测模型，输出人脸边界框和置信度
 * - SFace: 人脸识别模型，输出128维特征向量
 */
@Service
@Slf4j
public class FaceAuthServiceImpl implements FaceAuthService {

    // ========== 配置项：人脸检测模型路径（YuNet） ==========
    // 默认路径：classpath:face_models/face_detection_yunet_2023mar.onnx
    @Value("${face.model.detection:classpath:face_models/face_detection_yunet_2023mar.onnx}")
    private String detectionModelPath;

    // ========== 配置项：人脸识别模型路径（SFace） ==========
    // 默认路径：classpath:face_models/face_recognition_sface_2021dec.onnx
    @Value("${face.model.recognition:classpath:face_models/face_recognition_sface_2021dec.onnx}")
    private String recognitionModelPath;

    // ========== 配置项：默认录入人脸数量 ==========
    // 用户需要录入8张人脸才能完成录入
    @Value("${face.enrollment.face-count:8}")
    private int defaultFaceCount;

    // ========== 配置项：人脸识别相似度阈值 ==========
    // 相似度超过0.4则认为匹配成功
    @Value("${face.recognition.threshold:0.4}")
    private float defaultThreshold;

    // ========== 配置项：人脸质量阈值 ==========
    // 质量分数低于0.5的人脸图片会被拒绝录入
    @Value("${face.enrollment.quality-threshold:0.5}")
    private float qualityThreshold;

    // ========== Haar级联人脸检测器 ==========
    private CascadeClassifier faceDetector;

    // ========== 模型网络：SFace人脸识别网络（ONNX，需从文件加载） ==========
    private Net recognizerNet;

    // ========== JavaCV帧转换器：用于视频流处理 ==========
    private Java2DFrameConverter frameConverter;

    // ========== 拍照录入会话存储（线程安全） ==========
    // Key: enrollId, Value: EnrollSession
    // 使用ConcurrentHashMap支持高并发访问
    private final Map<String, EnrollSession> enrollSessions = new ConcurrentHashMap<>();

    // ========== 视频录入会话存储（线程安全） ==========
    // Key: enrollId, Value: VideoEnrollSession
    // 视频录入和拍照录入使用不同的会话类型
    private final Map<String, VideoEnrollSession> videoEnrollSessions = new ConcurrentHashMap<>();

    // ==================== 拍照录入会话数据结构 ====================
    // 用于存储用户拍照录入过程中的临时数据
    // 与VideoEnrollSession的区别：拍照模式需要用户手动拍照，存储人脸图片
    private static class EnrollSession {
        String enrollId;              // 会话唯一标识
        String userId;                // 用户标识
        int required;                 // 需要录入的人脸数量（默认8张）
        List<float[]> features;        // 检测到的人脸特征向量列表
        List<byte[]> faceImages;       // 对应的人脸图片（JPEG字节数组）
        List<Float> qualities;        // 对应人脸的质量分数
        long startTime;               // 会话创建时间

        // 构造函数：初始化会话
        EnrollSession(String enrollId, String userId, int required) {
            this.enrollId = enrollId;
            this.userId = userId;
            this.required = required;
            this.features = new ArrayList<>();
            this.faceImages = new ArrayList<>();
            this.qualities = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
        }
    }

    // ==================== 视频录入会话数据结构 ====================
    // 与拍照模式区别：
    // 1. 不需要required字段（视频录入固定50帧，最后选最好的8张）
    // 2. 不存储faceImages（视频模式不保存人脸图片）
    // 3. 额外统计totalFrames和detectedFaces
    private static class VideoEnrollSession {
        String enrollId;              // 会话唯一标识
        String userId;                // 用户标识
        List<float[]> features;        // 检测到的人脸特征列表（只有质量达标的才存入）
        List<Float> qualities;        // 对应每张人脸的质量分数
        long startTime;               // 会话创建时间
        int totalFrames;              // 已录制的总帧数
        int detectedFaces;            // 检测到有效人脸的帧数

        VideoEnrollSession(String enrollId, String userId) {
            this.enrollId = enrollId;
            this.userId = userId;
            this.features = new ArrayList<>();
            this.qualities = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
            this.totalFrames = 0;
            this.detectedFaces = 0;
        }
    }

    // ==================== 模型初始化 ====================
    // 在服务创建后自动执行，加载Haar检测器和SFace模型
    @PostConstruct
    public void init() {
        try {
            log.info("初始化人脸识别模型...");

            // 加载OpenCV本地库（必须先调用）
            nu.pattern.OpenCV.loadLocally();

            // 加载Haar级联人脸检测器
            ClassPathResource haarResource = new ClassPathResource("haarcascade_frontalface_default.xml");
            faceDetector = new CascadeClassifier(haarResource.getFile().getAbsolutePath());
            log.info("Haar级联检测器加载成功");

            // 从临时文件加载SFace人脸识别模型（ONNX必须从真实文件路径加载）
            ClassPathResource recognizerResource = new ClassPathResource("face_models/face_recognition_sface_2021dec.onnx");
            File tempModel = new File(System.getProperty("java.io.tmpdir"), "sface_" + System.currentTimeMillis() + ".onnx");
            try (InputStream is = recognizerResource.getInputStream()) {
                java.nio.file.Files.copy(is, tempModel.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                recognizerNet = Dnn.readNetFromONNX(tempModel.getAbsolutePath());
                log.info("SFace模型加载成功: {}", tempModel.getAbsolutePath());
            } finally {
                tempModel.deleteOnExit();
            }

            // 创建JavaCV帧转换器（用于后续视频流处理）
            frameConverter = new Java2DFrameConverter();

            log.info("人脸识别模型初始化完成");
        } catch (Exception e) {
            log.error("人脸识别模型初始化失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 开始拍照录入会话 ====================
    // 创建一个新的录入会话，返回enrollId供后续使用
    @Override
    public String startEnroll(String userId, int faceCount) {
        // 生成唯一的会话ID
        String enrollId = UUID.randomUUID().toString();
        // 创建会话对象，使用传入的脸数量或默认值8
        EnrollSession session = new EnrollSession(enrollId, userId, faceCount > 0 ? faceCount : defaultFaceCount);
        // 将会话存入Map
        enrollSessions.put(enrollId, session);
        log.info("开始录入会话: enrollId={}, userId={}, required={}", enrollId, userId, session.required);
        return enrollId;
    }

    // ==================== 拍照模式：捕获当前帧进行录入 ====================
    // 从后端视频流抓取当前帧，检测人脸并录入
    @Override
    public FaceEnrollVO captureForEnroll(String enrollId, String userId) {
        // 创建返回对象
        FaceEnrollVO vo = new FaceEnrollVO();
        vo.setEnrollId(enrollId);
        vo.setUserId(userId);

        // 根据enrollId查找会话
        EnrollSession session = enrollSessions.get(enrollId);
        // 会话不存在或已过期
        if (session == null) {
            vo.setMessage("录入会话不存在或已过期");
            return vo;
        }

        // userId不匹配（防止跨用户数据混入）
        if (!session.userId.equals(userId)) {
            vo.setMessage("用户ID不匹配");
            return vo;
        }

        // 已录入数量已达到要求，无需继续录入
        if (session.features.size() >= session.required) {
            vo.setCompleted(true);
            vo.setCaptured(session.features.size());
            vo.setRequired(session.required);
            vo.setProgress(100);
            vo.setMessage("录入已完成");
            return vo;
        }

        try {
            // 从视频服务获取当前帧
            BufferedImage frame = getCurrentFrame();
            if (frame == null) {
                vo.setMessage("无法获取摄像头画面");
                return vo;
            }

            // 使用YuNet检测人脸并裁剪
            BufferedImage face = detectAndCropFace(frame);
            if (face == null) {
                vo.setMessage("未检测到人脸，请调整位置");
                return vo;
            }

            // 评估人脸质量
            float quality = assessQuality(face);
            // 质量不达标则拒绝录入
            if (quality < qualityThreshold) {
                vo.setMessage("人脸质量不足，请确保光线充足且正对摄像头");
                vo.setQuality(quality);
                return vo;
            }

            // 使用SFace提取128维特征向量
            float[] feature = extractFeature(face);
            if (feature == null) {
                vo.setMessage("特征提取失败");
                return vo;
            }

            // 将裁剪后的人脸图片转为JPEG字节数组用于存储
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(face, "jpg", baos);
            baos.flush();
            byte[] faceImage = baos.toByteArray();
            baos.close();

            // 将特征、图像、质量分数存入会话
            session.features.add(feature);
            session.faceImages.add(faceImage);
            session.qualities.add(quality);

            // 设置返回的进度信息
            vo.setCaptured(session.features.size());
            vo.setRequired(session.required);
            vo.setProgress((int) (session.features.size() * 100.0 / session.required));
            vo.setQuality(quality);
            vo.setMessage("已捕获 " + vo.getCaptured() + "/" + vo.getRequired());

            // 已录入足够数量，标记完成
            if (vo.getCaptured() >= vo.getRequired()) {
                vo.setCompleted(true);
            }

            log.info("人脸捕获: enrollId={}, captured={}/{}, quality={}",
                    enrollId, vo.getCaptured(), vo.getRequired(), quality);

        } catch (Exception e) {
            log.error("捕获人脸失败: {}", e.getMessage(), e);
            vo.setMessage("捕获失败: " + e.getMessage());
        }

        return vo;
    }

    // ==================== 拍照模式：完成录入 ====================
    // 结束录入会话，将所有人脸存入数据库
    @Override
    public Result completeEnroll(String enrollId) {
        // 从Map中移除会话（原子操作）
        EnrollSession session = enrollSessions.remove(enrollId);
        if (session == null) {
            return Result.error(400, "录入会话不存在或已过期");
        }

        // 检查是否捕获到有效人脸
        if (session.features.isEmpty()) {
            return Result.error(400, "未捕获到有效人脸");
        }

        try {
            // 获取数据库Mapper
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userInfoMapper = SpringContext.getBean(UserInfoMapper.class);

            int index = 1;
            // 遍历所有捕获的人脸
            for (int i = 0; i < session.features.size(); i++) {
                // 创建人脸特征数据对象
                FaceFeatureDO faceFeature = new FaceFeatureDO();
                // 使用时间戳+索引作为主键（实际应使用自增ID）
                faceFeature.setId(System.currentTimeMillis() + i);
                faceFeature.setUserId(session.userId);
                faceFeature.setFaceIndex(index++);
                // 将float数组转换为byte数组存储
                faceFeature.setFeatureVector(floatsToBytes(session.features.get(i)));
                // 存储裁剪后的人脸图片
                faceFeature.setFaceImage(session.faceImages.get(i));
                faceFeature.setQualityScore(session.qualities.get(i));
                faceFeature.setThreshold(defaultThreshold);
                faceFeature.setCreateTime(new Date());

                // 插入数据库
                mapper.insert(faceFeature);
            }

            // 更新用户的人脸录入标志
            UserInfoDO user = UserInfoDO.builder().userId(session.userId).faceRegistered("Y").build();
            userInfoMapper.updateFaceEnrolled(user);

            log.info("人脸录入完成: userId={}, count={}", session.userId, session.features.size());
            return Result.success("人脸录入完成，共录入 " + session.features.size() + " 张");

        } catch (Exception e) {
            log.error("保存人脸失败: {}", e.getMessage(), e);
            return Result.error(500, "保存失败: " + e.getMessage());
        }
    }

    // ==================== 拍照模式：取消录入 ====================
    // 移除会话，清理临时数据（不会删除已存入数据库的数据）
    @Override
    public void cancelEnroll(String enrollId) {
        enrollSessions.remove(enrollId);
        log.info("取消录入: enrollId={}", enrollId);
    }

    // ==================== 提取人脸特征（SFace模型） ====================
    // 输入裁剪后的人脸图片，输出128维特征向量
    @Override
    public float[] extractFeature(BufferedImage image) {
        // 参数校验
        if (image == null || recognizerNet == null) {
            return null;
        }

        try {
            // 将BufferedImage转换为OpenCV Mat
            Mat mat = bufferedImageToMat(image);

            // BGR转RGB（OpenCV原生格式是BGR，需要转成RGB给模型用）
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

            // 创建DNN输入blob
            // SFace输入：112x112，缩放因子1/128，无均值减法
            // swapRB=false因为我们已经手动做了BGR→RGB转换，不需要再swap
            Mat blob = Dnn.blobFromImage(rgbMat, 1.0 / 128.0, new Size(112, 112), new Scalar(0, 0, 0), false, false);
            recognizerNet.setInput(blob);

            // 前向传播获取特征
            Mat featureMat = recognizerNet.forward();

            // L2归一化
            Core.normalize(featureMat, featureMat);

            // 将Mat转换为float数组
            float[] feature = new float[(int) (featureMat.total())];
            featureMat.get(0, 0, feature);

            // 释放OpenCV资源
            mat.release();
            rgbMat.release();
            blob.release();
            featureMat.release();

            return feature;

        } catch (Exception e) {
            log.error("特征提取失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 检测并裁剪人脸（Haar级联） ====================
    // 输入原始图片，输出裁剪后的人脸图片
    @Override
    public BufferedImage detectAndCropFace(BufferedImage image) {
        if (image == null) {
            return null;
        }

        try {
            Mat mat = bufferedImageToMat(image);
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faces = new MatOfRect();
            if (faceDetector != null) {
                faceDetector.detectMultiScale(gray, faces, 1.15, 2, 0, new Size(80, 80), new Size(400, 400));
            }

            List<Rect> faceList = faces.toList();
            if (faceList.isEmpty()) {
                mat.release();
                gray.release();
                faces.release();
                return null;
            }

            Rect r = faceList.get(0);
            int margin = (int) (Math.max(r.width, r.height) * 0.2);
            int ix = Math.max(0, r.x - margin);
            int iy = Math.max(0, r.y - margin);
            int iw = Math.min(mat.cols() - ix, r.width + margin * 2);
            int ih = Math.min(mat.rows() - iy, r.height + margin * 2);

            Rect faceRect = new Rect(ix, iy, iw, ih);
            Mat faceMat = new Mat(mat, faceRect);
            Mat resized = new Mat();
            Imgproc.resize(faceMat, resized, new Size(112, 112));

            BufferedImage face = matToBufferedImage(resized);

            mat.release();
            gray.release();
            faces.release();
            faceMat.release();
            resized.release();

            return face;

        } catch (Exception e) {
            log.error("人脸检测失败: {}", e.getMessage(), e);
        }

        return null;
    }

    // ==================== 评估人脸质量 ====================
    // 基于亮度和对比度评估人脸质量（0-1）
    @Override
    public float assessQuality(BufferedImage face) {
        if (face == null) {
            return 0;
        }

        try {
            Mat mat = bufferedImageToMat(face);
            if (mat == null) return 0;

            // 转为灰度图
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            if (gray.empty()) {
                mat.release();
                return 0;
            }

            // 计算平均亮度（灰度均值）
            Scalar mean = Core.mean(gray);
            double brightness = mean.val[0];

            // 计算标准差（对比度指标）
            MatOfDouble stdDevMat = new MatOfDouble();
            MatOfDouble meanMat = new MatOfDouble();
            Core.meanStdDev(gray, meanMat, stdDevMat);
            double stddev = stdDevMat.get(0, 0)[0];

            // 亮度得分：理想值128，越接近越好
            // 公式：1 - |mean - 128| / 128
            float brightnessScore = (float) Math.max(0, 1 - Math.abs(brightness - 128) / 128);

            // 对比度得分：标准差越大对比度越高
            // 公式：stddev / 64（最高1.0）
            float contrastScore = (float) Math.min(1, stddev / 64);

            // 综合得分：亮度权重40%，对比度权重60%
            float quality = (float) (brightnessScore * 0.4 + contrastScore * 0.6);

            // 释放资源
            mat.release();
            gray.release();
            stdDevMat.release();
            meanMat.release();

            return quality;

        } catch (Exception e) {
            return 0.5f;
        }
    }

    // ==================== 获取当前视频帧（从FaceVideoService） ====================
    private BufferedImage getCurrentFrame() {
        try {
            FaceVideoService videoService = SpringContext.getBean(FaceVideoService.class);
            if (videoService == null) {
                log.warn("FaceVideoService 为空");
                return null;
            }
            // 获取最新的JPEG帧
            byte[] jpeg = videoService.getLatestJpeg();
            if (jpeg == null || jpeg.length == 0) {
                log.debug("当前无视频帧");
                return null;
            }
            // 将JPEG字节数组转为BufferedImage
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(jpeg));
            log.debug("获取到帧，大小: {} bytes", jpeg.length);
            return img;
        } catch (Exception e) {
            log.error("获取当前帧失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 余弦相似度计算（byte数组版本） ====================
    private float cosineSimilarity(float[] a, byte[] b) {
        float[] bArr = bytesToFloats(b);
        return cosineSimilarity(a, bArr);
    }

    // ==================== 余弦相似度计算（float数组版本） ====================
    // 公式：cosine = dot(a,b) / (|a| * |b|)
    private float cosineSimilarity(float[] a, float[] b) {
        // 维度不同返回0
        if (a.length != b.length) {
            return 0;
        }

        // 计算点积和各向量的模
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        // 加上极小值防止除零
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10));
    }

    // ==================== byte数组转float数组 ====================
    // 用于从数据库读取featureVector
    private float[] bytesToFloats(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
        return floats;
    }

    // ==================== float数组转byte数组 ====================
    // 用于存储featureVector到数据库
    private byte[] floatsToBytes(float[] floats) {
        byte[] bytes = new byte[floats.length * 4];
        ByteBuffer.wrap(bytes).asFloatBuffer().put(floats);
        return bytes;
    }

    // ==================== BufferedImage转Mat ====================
    private Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) return null;
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int type = image.getType();
            // 如果类型未知，默认为TYPE_3BYTE_BGR
            if (type == 0) type = BufferedImage.TYPE_3BYTE_BGR;

            // 创建Mat（高度、宽度、类型）
            Mat mat = new Mat(height, width, CvType.CV_8UC3);
            // 获取像素数据
            byte[] pixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            // 填充Mat
            mat.put(0, 0, pixels);
            return mat;
        } catch (Exception e) {
            log.error("BufferedImage to Mat failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Mat转BufferedImage ====================
    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null) return null;
        try {
            int width = mat.cols();
            int height = mat.rows();
            byte[] pixels = new byte[width * height * 3];
            mat.get(0, 0, pixels);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            image.getRaster().setDataElements(0, 0, width, height, pixels);
            return image;
        } catch (Exception e) {
            log.error("Mat to BufferedImage failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 服务销毁时清理资源 ====================
    @PreDestroy
    public void destroy() {
        log.info("FaceAuthService 销毁");
    }

    // ==================== 拍照模式：使用上传图片进行录入 ====================
    // 与captureForEnroll的区别：图片数据由前端上传
    @Override
    public FaceEnrollVO captureForEnrollWithImage(String enrollId, String userId, String imageData) {
        FaceEnrollVO vo = new FaceEnrollVO();
        vo.setEnrollId(enrollId);
        vo.setUserId(userId);

        EnrollSession session = enrollSessions.get(enrollId);
        if (session == null) {
            vo.setMessage("录入会话不存在或已过期");
            return vo;
        }

        if (!session.userId.equals(userId)) {
            vo.setMessage("用户ID不匹配");
            return vo;
        }

        if (session.features.size() >= session.required) {
            vo.setCompleted(true);
            vo.setCaptured(session.features.size());
            vo.setRequired(session.required);
            vo.setProgress(100);
            vo.setMessage("录入已完成");
            return vo;
        }

        try {
            // Base64解码图片
            BufferedImage image = decodeBase64Image(imageData);
            if (image == null) {
                vo.setMessage("图片解析失败");
                return vo;
            }

            // 检测人脸
            BufferedImage face = detectAndCropFace(image);
            if (face == null) {
                vo.setMessage("未检测到人脸，请调整位置");
                return vo;
            }

            // 评估质量
            float quality = assessQuality(face);
            if (quality < qualityThreshold) {
                vo.setMessage("人脸质量不足，请确保光线充足且正对摄像头");
                vo.setQuality(quality);
                return vo;
            }

            // 提取特征
            float[] feature = extractFeature(face);
            if (feature == null) {
                vo.setMessage("特征提取失败");
                return vo;
            }

            // 存储人脸图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(face, "jpg", baos);
            baos.flush();
            byte[] faceImage = baos.toByteArray();
            baos.close();

            // 存入会话
            session.features.add(feature);
            session.faceImages.add(faceImage);
            session.qualities.add(quality);

            // 设置返回信息
            vo.setCaptured(session.features.size());
            vo.setRequired(session.required);
            vo.setProgress((int) (session.features.size() * 100.0 / session.required));
            vo.setQuality(quality);
            vo.setMessage("已捕获 " + vo.getCaptured() + "/" + vo.getRequired());

            if (vo.getCaptured() >= vo.getRequired()) {
                vo.setCompleted(true);
            }

            log.info("人脸捕获(图片): enrollId={}, captured={}/{}, quality={}",
                    enrollId, vo.getCaptured(), vo.getRequired(), quality);

        } catch (Exception e) {
            log.error("捕获人脸失败: {}", e.getMessage(), e);
            vo.setMessage("捕获失败: " + e.getMessage());
        }

        return vo;
    }


    // ==================== Base64图片解码 ====================
    // 将Base64字符串解码为BufferedImage
    private BufferedImage decodeBase64Image(String imageData) {
        if (imageData == null || imageData.isEmpty()) {
            return null;
        }
        try {
            String base64 = imageData;
            // 处理标准Data URL格式：data:image/jpeg;base64,/9j/4AAQ...
            if (base64.contains(",")) {
                base64 = base64.split(",")[1];
            }
            // Base64解码
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bais);
            bais.close();
            return image;
        } catch (Exception e) {
            log.error("Base64图片解码失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Result deleteFaceData(String userId) {
        try {
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userMapper = SpringContext.getBean(UserInfoMapper.class);

            mapper.deleteByUserId(userId);
            UserInfoDO user = UserInfoDO.builder().userId(userId).faceRegistered("N").build();
            userMapper.updateFaceEnrolled(user);

            log.info("删除用户人脸数据: userId={}", userId);
            return Result.success("人脸数据已删除，可重新采集");
        } catch (Exception e) {
            log.error("删除人脸数据失败: {}", e.getMessage(), e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
}