package com.th.ipqcmbiz.service.face.impl;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.FaceInfoDO;
import com.th.ipqcmbiz.mapper.face.FaceMapper;
import com.th.ipqcmbiz.service.face.FaceService;
import com.th.ipqcmbiz.utils.face.FaceRecognitionUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * @ClassName FaceServiceImpl
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/3/25 14:16
 * @Version 1.0
 */
@Service
@Slf4j
public class FaceServiceImpl implements FaceService {

    @Resource
    private FaceMapper faceMapper;

    @Resource
    private FaceRecognitionUtil faceRecognitionUtil;


    // IP摄像头配置
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


    // 摄像头/人脸检测核心对象
    private VideoCapture cap;
    private CascadeClassifier detector;
    private Mat frame;

    // 采集状态变量
    private String userName;
    private int progress = 0;
    private long lastTime = 0;
    private String direction = "请正视摄像头";
    private int step = 0;
    private final int perDir = 2;
    private final String[] dirs = {"正视", "左转", "右转", "抬头", "低头"};
    // 采集完成标识
    private boolean collectCompleted = false;


    @Override
    public List<FaceInfoDO> selectAllFace() {
        return faceMapper.selectAllFace();
    }


    // 初始化核心对象（PostConstruct：Spring Bean初始化后执行）
    @PostConstruct
    public void init() {
        log.info("开始初始化人脸采集核心对象...");
        // 加载OpenCV核心库
        try {
            OpenCV.loadLocally();
            log.info("OpenCV核心库加载成功");
        } catch (Exception e) {
            log.error("OpenCV核心库加载失败！", e);
            throw new RuntimeException("OpenCV加载失败：" + e.getMessage());
        }

        // 初始化采集状态
        resetCollectState();
        log.info("采集状态重置完成");

        // 1. 初始化摄像头
        try {
            cap = new VideoCapture(0);
            if (!cap.isOpened()) {
                log.error("摄像头初始化失败：无法打开默认摄像头（设备号0）");
                throw new RuntimeException("无法打开摄像头，请检查设备是否正常/是否被其他程序占用！");
            }
            log.info("摄像头初始化成功（设备号0）");
        } catch (Exception e) {
            log.error("摄像头初始化异常", e);
            throw new RuntimeException("摄像头初始化异常：" + e.getMessage());
        }

        // 2. 初始化人脸检测器
        try {
            ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
            detector = new CascadeClassifier(resource.getFile().getAbsolutePath());
            log.info("人脸检测器初始化成功");
        } catch (Exception e) {
            log.error("人脸检测器初始化异常", e);
            throw new RuntimeException("人脸检测器初始化异常：" + e.getMessage());
        }

        // 3. 初始化帧容器
        try {
            frame = new Mat();
            log.info("帧容器Mat初始化成功");
        } catch (Exception e) {
            log.error("帧容器初始化异常", e);
            throw new RuntimeException("帧容器初始化异常：" + e.getMessage());
        }

        log.info("人脸采集核心对象初始化全部完成");
    }

    // 销毁资源（PreDestroy：Spring Bean销毁前执行）
    @PreDestroy
    public void destroy() {
        releaseCameraResource();
    }

    // 释放摄像头资源
    public void releaseCameraResource() {
        log.info("开始释放摄像头资源...");
        if (cap != null && cap.isOpened()) {
            cap.release(); // 释放摄像头
            log.info("摄像头已释放");
        }
        if (frame != null && !frame.empty()) {
            frame.release(); // 释放Mat内存
            log.info("帧容器已释放");
        }
        // 释放后置空，避免重复操作
        cap = null;
        frame = null;
    }

    // 重置采集状态（支持重新采集）
    private void resetCollectState() {
        userName = null;
        progress = 0;
        lastTime = 0;
        direction = "请正视摄像头";
        step = 0;
        collectCompleted = false;
    }

    @Override
    public BufferedImage getFrame(String userId, String name) {
        // 空值校验：如果核心对象未初始化，尝试重试初始化
        if (cap == null || detector == null || frame == null) {
            log.warn("核心对象未初始化，尝试自动重试初始化...");
            try {
                releaseCameraResource(); // 先释放旧资源
                init(); // 重新初始化
            } catch (Exception e) {
                log.error("自动重试初始化失败，无法继续采集", e);
                return null;
            }
            // 重试后仍为空，直接返回
            if (cap == null || detector == null || frame == null) {
                log.error("重试初始化后核心对象仍为null，返回空帧");
                return null;
            }
        }

        // 采集完成后不再处理帧
        if (collectCompleted) {
            return null;
        }

        if (userName == null) {
            userName = name;
        }

        // 读取摄像头帧（非阻塞，失败则返回null）
        boolean readSuccess = cap.read(frame);
        if (!readSuccess || frame.empty()) {
            return null;
        }

        // 灰度化（人脸检测需灰度图）
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        // 直方图均衡化（提升检测效果）
        Imgproc.equalizeHist(gray, gray);

        // 检测人脸
        MatOfRect faceRects = new MatOfRect();
        detector.detectMultiScale(
                gray,
                faceRects,
                1.1,    // 缩放因子
                5,      // 邻域数
                0,      // 检测标志
                new Size(80, 80),  // 最小人脸尺寸
                new Size(300, 300) // 最大人脸尺寸
        );

        // 绘制人脸框 + 采集人脸特征
        for (Rect rect : faceRects.toArray()) {
            // 绘制绿色人脸框（BGR格式）
            Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);

            // 进度未完成 + 间隔2秒采集一次
            if (progress < 10 && System.currentTimeMillis() - lastTime > 2000) {
                // 截取人脸区域并缩放为100x100
                Mat faceROI = new Mat(gray, rect);
                Mat faceResized = new Mat();
                Imgproc.resize(faceROI, faceResized, new Size(100, 100));

                // 转换为字节数组
                byte[] faceFeature = new byte[(int) (faceResized.total() * faceResized.channels())];
                faceResized.get(0, 0, faceFeature);

                // 保存到数据库
                save(userId, userName, faceFeature);

                // 更新进度和提示
                progress++;
                lastTime = System.currentTimeMillis();
                if (progress % perDir == 0) {
                    step++;
                }
                if (step < dirs.length) {
                    direction = "请" + dirs[step];
                } else {
                    direction = "采集完成";
                }

                // 释放临时Mat内存
                faceROI.release();
                faceResized.release();

                // 进度达到10时，标记采集完成并释放摄像头
                if (progress >= 10) {
                    collectCompleted = true;
                    direction = "采集完成，正在释放摄像头...";
                    // 异步释放（避免阻塞帧返回）
                    new Thread(this::releaseCameraResource).start();
                    log.info("人脸采集完成，进度：{}，已触发摄像头释放", progress);
                }
            }
        }

        // 绘制提示文字（蓝色，字体大小1.2，线宽2）
        Imgproc.putText(
                frame,
                direction,
                new Point(20, 40),          // 文字位置
                Imgproc.FONT_HERSHEY_SIMPLEX, // 字体
                1.2,                        // 字体大小
                new Scalar(255, 0, 0),      // 颜色（BGR）
                2                           // 线宽
        );

        // 释放灰度图内存
        gray.release();
        faceRects.release();

        // 转换为BufferedImage返回
        return matToBufImg(frame);
    }

    // 对外暴露的释放摄像头接口（供前端主动调用）
    @Override
    public void releaseCamera() {
        log.info("接收到前端主动释放摄像头请求");
        collectCompleted = true;
        releaseCameraResource();
        resetCollectState();
    }

    @Override
    public String getDirection() {
        // 返回真实的采集提示
        return direction;
    }

    @Override
    public int getProgress() {
        // 返回真实的采集进度（0-10）
        return progress;
    }

    /**
     * 保存人脸特征到数据库
     */
    private void save(String userId, String name, byte[] faceFeature) {
        try {
            FaceInfoDO faceInfo = FaceInfoDO.builder()
                    .userId(userId)
                    .userName(name)
                    .faceFeature(faceFeature)
                    .direction(direction)
                    .build();
            faceMapper.insertFaceWithName(faceInfo);
            // 清空人脸缓存
            faceRecognitionUtil.clearFaceCache();
        } catch (Exception e) {
            log.error("保存人脸特征失败", e); // 替换printStackTrace为日志
        }
    }

    /**
     * Mat转换为BufferedImage（优化：避免临时文件，直接内存流）
     */
    private BufferedImage matToBufImg(Mat mat) {
        if (mat.empty()) {
            return null;
        }
        try {
            // 方案1：内存流转换（无临时文件，推荐）
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, matOfByte);
            byte[] byteArray = matOfByte.toArray();
            return ImageIO.read(new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            log.error("Mat转换BufferedImage失败", e);
            return null;
        }
    }

    @Override
    public Result reinitCamera() {
        try {
            // 先释放旧资源
            releaseCameraResource();
            // 重新初始化
            init();
            return Result.success("摄像头重新初始化成功");
        } catch (Exception e) {
            return Result.error(500, "摄像头重新初始化失败");
        }
    }
}