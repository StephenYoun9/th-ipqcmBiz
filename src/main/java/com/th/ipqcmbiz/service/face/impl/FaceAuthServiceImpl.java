package com.th.ipqcmbiz.service.face.impl;

// ========== 导入：统一返回结果格式 ==========

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.FaceFeatureDO;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.FaceEnrollVO;
import com.th.ipqcmbiz.entity.vo.FaceRecognizeVO;
import com.th.ipqcmbiz.entity.vo.FaceVideoEnrollVO;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import com.th.ipqcmbiz.mapper.face.FaceFeatureMapper;
import com.th.ipqcmbiz.service.face.FaceAuthService;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
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

    // ========== 配置项：FFmpeg路径 ==========
    @Value("${ffmpeg.path:C:/yxm/ffmpeg-n8.1-latest-win64-gpl-8.1/ffmpeg-n8.1-latest-win64-gpl-8.1/bin/ffmpeg.exe}")
    private String ffmpegPath;

    // ========== 配置项：摄像头RTSP配置 ==========
    @Value("${camera.ip}")
    private String cameraIp;
    @Value("${camera.port:554}")
    private int cameraPort;
    @Value("${camera.username}")
    private String cameraUsername;
    @Value("${camera.password}")
    private String cameraPassword;
    @Value("${camera.stream-path}")
    private String streamPath;

    // ========== 模型网络：YuNet人脸检测网络 ==========
    private Net detectorNet;

    // ========== 模型网络：SFace人脸识别网络 ==========
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
            this.startTime = System.currentTimeMillis();
            this.totalFrames = 0;
            this.detectedFaces = 0;
        }
    }

    // ==================== 模型初始化 ====================
    // 在服务创建后自动执行，加载YuNet和SFace模型
    @PostConstruct
    public void init() {
        try {
            log.info("初始化人脸识别模型...");

            // 加载OpenCV本地库（必须先调用）
            nu.pattern.OpenCV.loadShared();

            // 从classpath加载YuNet人脸检测模型
            ClassPathResource detectorResource = new ClassPathResource("face_models/face_detection_yunet_2023mar.onnx");
            try (InputStream is = detectorResource.getInputStream()) {
                // 读取模型文件为字节数组
                byte[] modelData = is.readAllBytes();
                // 将字节数组转换为OpenCV的MatOfByte格式
                MatOfByte matOfByte = new MatOfByte(modelData);
                // 从ONNX格式加载检测网络
                detectorNet = Dnn.readNetFromONNX(matOfByte);
            }

            // 从classpath加载SFace人脸识别模型
            ClassPathResource recognizerResource = new ClassPathResource("face_models/face_recognition_sface_2021dec.onnx");
            try (InputStream is = recognizerResource.getInputStream()) {
                byte[] modelData = is.readAllBytes();
                MatOfByte matOfByte = new MatOfByte(modelData);
                recognizerNet = Dnn.readNetFromONNX(matOfByte);
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
            UserInfoDO user = UserInfoDO.builder().userId(session.userId).faceEnrolled("1").build();
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

    // ==================== 视频模式：开始视频录入会话 ====================
    // 创建VideoEnrollSession，开启视频录入流程
    @Override
    public String startVideoEnroll(String userId) {
        // 生成唯一的会话ID
        String enrollId = UUID.randomUUID().toString();
        // 创建视频录入会话
        VideoEnrollSession session = new VideoEnrollSession(enrollId, userId);
        // 存入Map
        videoEnrollSessions.put(enrollId, session);
        log.info("开始视频录入会话: enrollId={}, userId={}", enrollId, userId);
        return enrollId;
    }

    // ==================== 视频模式：添加视频帧 ====================
    // 每收到一帧就调用此方法，检测人脸、评估质量、提取特征
    @Override
    public FaceVideoEnrollVO addVideoFrame(String enrollId, String userId, int frameIndex, String imageData) {
        // 创建返回对象
        FaceVideoEnrollVO vo = new FaceVideoEnrollVO();
        vo.setEnrollId(enrollId);
        vo.setUserId(userId);
        vo.setFrameIndex(frameIndex);

        // 查找会话
        VideoEnrollSession session = videoEnrollSessions.get(enrollId);
        if (session == null) {
            vo.setMessage("录入会话不存在或已过期");
            return vo;
        }

        // userId校验
        if (!session.userId.equals(userId)) {
            vo.setMessage("用户ID不匹配");
            return vo;
        }

        // 总帧数+1（无论是否检测到人脸）
        session.totalFrames++;

        try {
            // 步骤1：Base64解码图片
            BufferedImage image = decodeBase64Image(imageData);
            if (image == null) {
                vo.setMessage("图片解析失败");
                return vo;
            }

            // 步骤2：YuNet人脸检测
            BufferedImage face = detectAndCropFace(image);
            if (face == null) {
                vo.setDetected(false);
                vo.setMessage("未检测到人脸");
                return vo;
            }

            // 步骤3：质量评估
            float quality = assessQuality(face);
            vo.setQuality(quality);

            // 质量不达标，不存储
            if (quality < qualityThreshold) {
                vo.setDetected(false);
                vo.setMessage("人脸质量不足");
                return vo;
            }

            // 步骤4：SFace特征提取
            float[] feature = extractFeature(face);
            if (feature == null) {
                vo.setDetected(false);
                vo.setMessage("特征提取失败");
                return vo;
            }

            // 步骤5：存储到会话
            session.features.add(feature);
            session.qualities.add(quality);
            session.detectedFaces++;

            // 设置返回信息
            vo.setDetected(true);
            vo.setDetectedFaces(session.detectedFaces);
            vo.setTotalFrames(session.totalFrames);
            vo.setMessage("已检测到人脸，质量: " + String.format("%.0f%%", quality * 100));

            log.debug("视频帧处理: enrollId={}, frameIndex={}, detected={}, quality={}",
                    enrollId, frameIndex, true, quality);

        } catch (Exception e) {
            log.error("处理视频帧失败: {}", e.getMessage(), e);
            vo.setMessage("处理失败: " + e.getMessage());
        }

        return vo;
    }

    // ==================== 视频模式：完成录入 ====================
    // 从检测到的所有人脸中选取质量最好的8张存入数据库
    @Override
    public Result completeVideoEnroll(String enrollId) {
        // 移除会话
        VideoEnrollSession session = videoEnrollSessions.remove(enrollId);
        if (session == null) {
            return Result.error(400, "录入会话不存在或已过期");
        }

        // 没有检测到任何有效人脸
        if (session.features.isEmpty()) {
            return Result.error(400, "未检测到有效人脸，请确保光线充足且正对摄像头");
        }

        // 检测到的人脸不足8张
        if (session.features.size() < 8) {
            return Result.error(400, "检测到的人脸不足8张，请重试。当前检测到: " + session.features.size() + " 张");
        }

        try {
            // 质量排序筛选
            // 创建索引列表[0, 1, 2, ..., n-1]
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < session.features.size(); i++) {
                indices.add(i);
            }
            // 按质量降序排序索引
            indices.sort((a, b) -> Float.compare(session.qualities.get(b), session.qualities.get(a)));

            // 取质量最好的8个
            List<float[]> topFeatures = new ArrayList<>();
            List<Float> topQualities = new ArrayList<>();
            for (int i = 0; i < 8 && i < indices.size(); i++) {
                int idx = indices.get(i);
                topFeatures.add(session.features.get(idx));
                topQualities.add(session.qualities.get(idx));
            }

            // 获取Mapper
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userInfoMapper = SpringContext.getBean(UserInfoMapper.class);

            // 批量存储到数据库
            for (int i = 0; i < topFeatures.size(); i++) {
                FaceFeatureDO faceFeature = new FaceFeatureDO();
                faceFeature.setId(System.currentTimeMillis() + i);
                faceFeature.setUserId(session.userId);
                faceFeature.setFaceIndex(i + 1);
                faceFeature.setFeatureVector(floatsToBytes(topFeatures.get(i)));
                // 视频模式不存储人脸图片，faceImage为null
                faceFeature.setQualityScore(topQualities.get(i));
                faceFeature.setThreshold(defaultThreshold);
                faceFeature.setCreateTime(new Date());
                mapper.insert(faceFeature);
            }

            // 更新用户状态
            UserInfoDO user = UserInfoDO.builder().userId(session.userId).faceEnrolled("1").build();
            userInfoMapper.updateFaceEnrolled(user);

            log.info("视频人脸录入完成: userId={}, detected={}, saved=8",
                    session.userId, session.detectedFaces);
            return Result.success("人脸录入完成，共检测到 " + session.detectedFaces + " 张，选取质量最高的 8 张存入数据库");

        } catch (Exception e) {
            log.error("保存视频人脸失败: {}", e.getMessage(), e);
            return Result.error(500, "保存失败: " + e.getMessage());
        }
    }

    // ==================== 视频录制模式（后端FFmpeg录制） ====================
    // 使用FFmpeg从RTSP流录制5秒视频，然后从视频中提取人脸进行录入
    @Override
    public Result recordVideoEnroll(String userId) {
        String videoPath = null;
        FFmpegFrameGrabber grabber = null;

        try {
            // 构建RTSP URL
            String rtspUrl = String.format("rtsp://%s:%s@%s:%d%s",
                    cameraUsername, cameraPassword, cameraIp, cameraPort, streamPath);
            log.info("开始录制视频: userId={}, rtspUrl={}", userId, rtspUrl);

            // 创建临时视频文件路径
            String tempDir = System.getProperty("java.io.tmpdir");
            videoPath = tempDir + "/face_video_" + System.currentTimeMillis() + ".mp4";

            // 使用JavaCV FFmpegFrameGrabber录制视频
            grabber = new FFmpegFrameGrabber(rtspUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            grabber.setAudioChannels(0);  // 不录制音频
            grabber.start();

            // 录制5秒视频
            long startTime = System.currentTimeMillis();
            int frameCount = 0;
            long totalFrames = 0;

            // 使用JavaCV录制
            Java2DFrameConverter converter = new Java2DFrameConverter();
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);

            List<BufferedImage> frames = new ArrayList<>();

            while (System.currentTimeMillis() - startTime < 5000) {
                try {
                    Frame frame = grabber.grabImage();
                    if (frame != null && frame.image != null) {
                        BufferedImage img = converter.convert(frame);
                        if (img != null) {
                            frames.add(img);
                            frameCount++;
                        }
                    }
                    totalFrames++;
                    // 稍微sleep一下避免CPU 100%
                    Thread.sleep(10);
                } catch (Exception e) {
                    log.debug("录制帧异常: {}", e.getMessage());
                }
            }

            grabber.stop();
            grabber.release();
            grabber = null;

            log.info("录制完成: userId={}, 捕获帧数={}, 有效帧数={}", userId, totalFrames, frameCount);

            if (frames.isEmpty()) {
                return Result.error(400, "录制失败：未能获取到视频帧");
            }

            // 处理录制的帧，提取人脸
            List<float[]> allFeatures = new ArrayList<>();
            List<Float> allQualities = new ArrayList<>();
            int detectedCount = 0;

            for (BufferedImage img : frames) {
                BufferedImage face = detectAndCropFace(img);
                if (face != null) {
                    float quality = assessQuality(face);
                    if (quality >= qualityThreshold) {
                        float[] feature = extractFeature(face);
                        if (feature != null) {
                            allFeatures.add(feature);
                            allQualities.add(quality);
                            detectedCount++;
                        }
                    }
                }
            }

            log.info("人脸检测完成: userId={}, 检测到有效人脸={}", userId, detectedCount);

            if (allFeatures.isEmpty()) {
                return Result.error(400, "未检测到有效人脸，请确保光线充足且正对摄像头");
            }

            if (allFeatures.size() < 8) {
                return Result.error(400, "检测到的人脸不足8张（" + allFeatures.size() + "张），请重试");
            }

            // 按质量排序，选取最好的8张
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < allFeatures.size(); i++) {
                indices.add(i);
            }
            indices.sort((a, b) -> Float.compare(allQualities.get(b), allQualities.get(a)));

            List<float[]> topFeatures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                topFeatures.add(allFeatures.get(indices.get(i)));
            }

            // 存入数据库
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userInfoMapper = SpringContext.getBean(UserInfoMapper.class);

            for (int i = 0; i < topFeatures.size(); i++) {
                FaceFeatureDO faceFeature = new FaceFeatureDO();
                faceFeature.setId(System.currentTimeMillis() + i);
                faceFeature.setUserId(userId);
                faceFeature.setFaceIndex(i + 1);
                faceFeature.setFeatureVector(floatsToBytes(topFeatures.get(i)));
                faceFeature.setQualityScore(allQualities.get(indices.get(i)));
                faceFeature.setThreshold(defaultThreshold);
                faceFeature.setCreateTime(new Date());
                mapper.insert(faceFeature);
            }

            // 更新用户状态
            UserInfoDO user = UserInfoDO.builder().userId(userId).faceEnrolled("1").build();
            userInfoMapper.updateFaceEnrolled(user);

            log.info("视频人脸录入完成: userId={}, detected={}, saved=8", userId, detectedCount);
            return Result.success("人脸录入完成，共检测到 " + detectedCount + " 张，选取质量最高的 8 张存入数据库");

        } catch (Exception e) {
            log.error("录制视频人脸录入失败: {}", e.getMessage(), e);
            return Result.error(500, "录制失败: " + e.getMessage());
        } finally {
            // 确保资源释放
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception ignored) {}
            }
            // 清理临时视频文件
            if (videoPath != null) {
                try {
                    File f = new File(videoPath);
                    if (f.exists()) {
                        f.delete();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==================== 人脸识别登录 ====================
    // 从视频流获取帧，检测人脸并与数据库中的人脸比对
    @Override
    public FaceRecognizeVO recognize(String userId) {
        FaceRecognizeVO vo = new FaceRecognizeVO();
        vo.setThreshold(defaultThreshold);

        try {
            // 获取当前视频帧
            BufferedImage frame = getCurrentFrame();
            if (frame == null) {
                vo.setMessage("无法获取摄像头画面");
                return vo;
            }

            // 检测人脸
            BufferedImage face = detectAndCropFace(frame);
            if (face == null) {
                vo.setMessage("未检测到人脸");
                return vo;
            }

            // 提取特征
            float[] queryFeature = extractFeature(face);
            if (queryFeature == null) {
                vo.setMessage("特征提取失败");
                return vo;
            }

            // 获取Mapper
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userInfoMapper = SpringContext.getBean(UserInfoMapper.class);

            // 加载人脸数据
            List<FaceFeatureDO> faceList;
            if (userId != null && !userId.isEmpty()) {
                // 指定用户：只加载该用户的人脸
                faceList = mapper.selectByUserId(userId);
            } else {
                // 未指定：加载所有用户的人脸
                faceList = mapper.selectAll();
            }

            if (faceList.isEmpty()) {
                vo.setMessage("无人脸数据，请先录入");
                return vo;
            }

            // 与每个人脸比对，找最相似的
            float maxSimilarity = 0;
            String matchedUserId = null;
            String matchedUserName = null;

            for (FaceFeatureDO faceFeature : faceList) {
                // 计算余弦相似度
                float similarity = cosineSimilarity(queryFeature, faceFeature.getFeatureVector());
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    matchedUserId = faceFeature.getUserId();
                }
            }

            // 判断是否匹配成功
            if (maxSimilarity > defaultThreshold) {
                // 匹配成功，获取用户信息
                UserInfoDO user = userInfoMapper.queryUserById(matchedUserId);
                if (user != null) {
                    matchedUserName = user.getUserName();
                }
                vo.setMatched(true);
                vo.setUserId(matchedUserId);
                vo.setUserName(matchedUserName);
                vo.setSimilarity(maxSimilarity);
                vo.setMessage("识别成功，相似度: " + String.format("%.2f", maxSimilarity * 100) + "%");
            } else {
                vo.setMatched(false);
                vo.setSimilarity(maxSimilarity);
                vo.setMessage("未匹配到用户，相似度: " + String.format("%.2f", maxSimilarity * 100) + "%");
            }

            log.info("人脸识别: userId={}, matched={}, similarity={}", userId, vo.isMatched(), maxSimilarity);

        } catch (Exception e) {
            log.error("人脸识别失败: {}", e.getMessage(), e);
            vo.setMessage("识别失败: " + e.getMessage());
        }

        return vo;
    }

    // ==================== 获取录入状态 ====================
    // 查询指定用户已录入的人脸数量
    @Override
    public FaceEnrollVO getEnrollStatus(String userId) {
        FaceEnrollVO vo = new FaceEnrollVO();
        vo.setUserId(userId);

        try {
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            // 查询该用户已录入的人脸数量
            int count = mapper.countByUserId(userId);
            vo.setCaptured(count);
            vo.setRequired(defaultFaceCount);
            vo.setProgress((int) (count * 100.0 / defaultFaceCount));

            if (count >= defaultFaceCount) {
                vo.setCompleted(true);
                vo.setMessage("已录入 " + count + " 张");
            } else {
                vo.setMessage("已录入 " + count + "/" + defaultFaceCount + " 张");
            }

        } catch (Exception e) {
            log.error("查询录入状态失败: {}", e.getMessage());
            vo.setMessage("查询失败");
        }

        return vo;
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

    // ==================== 检测并裁剪人脸（YuNet模型） ====================
    // 输入原始图片，输出裁剪后的人脸图片
    @Override
    public BufferedImage detectAndCropFace(BufferedImage image) {
        if (image == null) {
            return null;
        }

        try {
            // 转换为OpenCV Mat
            Mat mat = bufferedImageToMat(image);

            // BGR转RGB（OpenCV原生格式是BGR，需要转成RGB给模型用）
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

            // 创建DNN输入blob
            // YuNet输入：160x160，缩放因子1/255
            // swapRB=false因为我们已经手动做了BGR→RGB转换，不需要再swap
            Mat blob = Dnn.blobFromImage(rgbMat, 1.0 / 255.0, new Size(160, 160), new Scalar(0, 0, 0), false, false);
            detectorNet.setInput(blob);

            // 前向传播获取检测结果
            Mat detection = detectorNet.forward();

            log.debug("YuNet输出 shape: [{}, {}, {}, {}], type: {}",
                    detection.dims(), detection.size(0), detection.size(1), detection.size(2), detection.type());

            // 遍历所有检测结果，找置信度最高的
            float maxConf = 0;
            float[] bestFace = null;

            // YuNet输出：[1, 1, N, 15]，N是检测数量
            int numDetections = detection.size(2);
            log.debug("检测到 {} 个人脸候选", numDetections);

            // 将4D tensor展平为1D数组，然后按检测分组读取
            // [1, 1, N, 15] -> [N*15] 元素
            Mat flat = detection.reshape(1);
            float[] allData = new float[numDetections * 15];
            flat.get(0, 0, allData);

            for (int i = 0; i < numDetections; i++) {
                float[] data = new float[15];
                // 每15个连续元素为一个检测的结果
                System.arraycopy(allData, i * 15, data, 0, 15);

                // data[0-3]是[x, y, w, h]相对坐标，data[4]是置信度
                float conf = data[4];
                log.debug("第{}个检测, conf={}, bbox=[{}, {}, {}, {}]",
                        i, conf, data[0], data[1], data[2], data[3]);

                if (conf > maxConf) {
                    maxConf = conf;
                    bestFace = data.clone();
                }
            }

            log.debug("最大置信度: {}", maxConf);

            // 只处理置信度>0.3的检测结果（降低阈值以便检测到更多人脸）
            if (bestFace != null && maxConf > 0.3) {
                // data[0-3]是[x, y, w, h]，都是相对坐标（0-1）
                float x = bestFace[0];
                float y = bestFace[1];
                float w = bestFace[2];
                float h = bestFace[3];

                log.debug("人脸位置(相对值): x={}, y={}, w={}, h={}", x, y, w, h);
                log.debug("原始图像尺寸: cols={}, rows={}", mat.cols(), mat.rows());

                // 相对坐标转换为绝对像素坐标
                int imgWidth = mat.cols();
                int imgHeight = mat.rows();
                int faceLeft = (int) (x * imgWidth);
                int faceTop = (int) (y * imgHeight);
                int faceWidth = (int) (w * imgWidth);
                int faceHeight = (int) (h * imgHeight);

                log.debug("人脸位置(绝对像素): left={}, top={}, width={}, height={}",
                        faceLeft, faceTop, faceWidth, faceHeight);

                // 添加20%边界margin
                int margin = (int) (Math.max(faceWidth, faceHeight) * 0.2);
                int ix = Math.max(0, faceLeft - margin);
                int iy = Math.max(0, faceTop - margin);
                int iw = Math.min(imgWidth - ix, faceWidth + margin * 2);
                int ih = Math.min(imgHeight - iy, faceHeight + margin * 2);

                log.debug("裁剪区域(加margin后): x={}, y={}, w={}, h={}", ix, iy, iw, ih);

                // 裁剪人脸区域
                Rect faceRect = new Rect(ix, iy, iw, ih);
                Mat faceMat = new Mat(mat, faceRect);

                // 缩放到112x112
                Mat resized = new Mat();
                Imgproc.resize(faceMat, resized, new Size(112, 112));

                // 转换为BufferedImage
                BufferedImage face = matToBufferedImage(resized);

                // 释放OpenCV资源
                mat.release();
                rgbMat.release();
                blob.release();
                detection.release();
                faceMat.release();
                resized.release();

                return face;
            } else {
                log.debug("未检测到人脸或置信度太低");
            }

            // 释放资源
            mat.release();
            rgbMat.release();
            blob.release();
            detection.release();

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

    // ==================== 使用上传图片进行识别 ====================
    // 与recognize的区别：图片数据由前端上传
    @Override
    public FaceRecognizeVO recognizeWithImage(String userId, String imageData) {
        FaceRecognizeVO vo = new FaceRecognizeVO();
        vo.setThreshold(defaultThreshold);

        try {
            // Base64解码
            BufferedImage image = decodeBase64Image(imageData);
            if (image == null) {
                vo.setMessage("图片解析失败");
                return vo;
            }

            // 检测人脸
            BufferedImage face = detectAndCropFace(image);
            if (face == null) {
                vo.setMessage("未检测到人脸");
                return vo;
            }

            // 提取特征
            float[] queryFeature = extractFeature(face);
            if (queryFeature == null) {
                vo.setMessage("特征提取失败");
                return vo;
            }

            // 获取Mapper
            FaceFeatureMapper mapper = SpringContext.getBean(FaceFeatureMapper.class);
            UserInfoMapper userInfoMapper = SpringContext.getBean(UserInfoMapper.class);

            // 加载人脸数据
            List<FaceFeatureDO> faceList;
            if (userId != null && !userId.isEmpty()) {
                faceList = mapper.selectByUserId(userId);
            } else {
                faceList = mapper.selectAll();
            }

            if (faceList.isEmpty()) {
                vo.setMessage("无人脸数据，请先录入");
                return vo;
            }

            // 遍历比对
            float maxSimilarity = 0;
            String matchedUserId = null;
            String matchedUserName = null;

            for (FaceFeatureDO faceFeature : faceList) {
                float similarity = cosineSimilarity(queryFeature, faceFeature.getFeatureVector());
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    matchedUserId = faceFeature.getUserId();
                }
            }

            // 判断是否匹配
            if (maxSimilarity > defaultThreshold) {
                UserInfoDO user = userInfoMapper.queryUserById(matchedUserId);
                if (user != null) {
                    matchedUserName = user.getUserName();
                }
                vo.setMatched(true);
                vo.setUserId(matchedUserId);
                vo.setUserName(matchedUserName);
                vo.setSimilarity(maxSimilarity);
                vo.setMessage("识别成功，相似度: " + String.format("%.2f", maxSimilarity * 100) + "%");
            } else {
                vo.setMatched(false);
                vo.setSimilarity(maxSimilarity);
                vo.setMessage("未匹配到用户，相似度: " + String.format("%.2f", maxSimilarity * 100) + "%");
            }

            log.info("人脸识别(图片): userId={}, matched={}, similarity={}", userId, vo.isMatched(), maxSimilarity);

        } catch (Exception e) {
            log.error("人脸识别失败: {}", e.getMessage(), e);
            vo.setMessage("识别失败: " + e.getMessage());
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
}