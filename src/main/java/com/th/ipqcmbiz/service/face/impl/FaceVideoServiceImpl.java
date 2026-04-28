package com.th.ipqcmbiz.service.face.impl;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.face.CameraInfo;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.th.ipqcmbiz.entity.vo.FaceFrameInfo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 本地摄像头视频服务实现
 * 使用OpenCV VideoCapture从本机摄像头（设备索引1）获取视频流
 */
@Service
@Slf4j
public class FaceVideoServiceImpl implements FaceVideoService {

    /** OpenCV VideoCapture对象，用于从摄像头读取帧 */
    private VideoCapture capture;

    /** 最新的一帧JPEG数据（已标注人脸框），其他线程可快速获取 */
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();

    /** 拉流线程是否运行中 */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 摄像头配置信息（分辨率、帧率等） */
    private final AtomicReference<CameraInfo> cameraInfo = new AtomicReference<>(new CameraInfo(0, 0, 0, "", false));

    /** 后台拉流线程，持续从摄像头读取帧并缓存 */
    private Thread grabThread;

    /** 是否已初始化（懒加载模式，首次使用时初始化） */
    private volatile boolean initialized = false;

    /** 初始化锁，防止并发重复初始化 */
    private final Object initLock = new Object();

    /** Haar级联人脸检测器 */
    private CascadeClassifier detector;

    /** 目标输出分辨率宽度 */
    private static final int TARGET_WIDTH = 640;

    /** 目标输出分辨率高度 */
    private static final int TARGET_HEIGHT = 480;

    /** JPEG压缩质量 0-100，越高质量越好但越大 */
    private static final int JPEG_QUALITY = 70;

    /** 人脸检测跳帧间隔（每N帧检测1次，降低CPU开销） */
    private static final int DETECT_INTERVAL = 3;

    /** 帧计数器 */
    private int frameCount = 0;

    /** 上一帧是否已检测过人脸（检测结果缓存） */
    private boolean lastFrameHasFace = false;

    /** 最新检测到的人脸坐标 */
    private final AtomicReference<Rect> latestFaceRect = new AtomicReference<>();

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            log.info("OpenCV加载成功");
        } catch (Exception e) {
            log.error("OpenCV加载失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化Haar级联人脸检测器
     */
    @PostConstruct
    public void initDetector() {
        try {
            nu.pattern.OpenCV.loadLocally();
            ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
            detector = new CascadeClassifier(resource.getFile().getAbsolutePath());
            log.info("Haar级联人脸检测器加载成功");
        } catch (Exception e) {
            log.error("Haar检测器加载失败: {}", e.getMessage());
        }
    }

    /**
     * 确保摄像头已启动（懒加载）
     * 首次调用getLatestJpeg时自动触发
     */
    private void ensureCameraStarted() {
        if (initialized && isRunning.get()) {
            return;
        }
        synchronized (initLock) {
            if (initialized && isRunning.get()) {
                return;
            }
            try {
                initCapture();
                startGrabThread();
                initialized = true;
            } catch (Exception e) {
                log.error("摄像头启动失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 初始化摄像头连接
     * 使用本机设备索引1的摄像头（CAP_DSHOW后端，Windows推荐）
     */
    private void initCapture() throws Exception {
        log.info("正在打开摄像头（设备索引1, CAP_DSHOW）...");
        capture = new VideoCapture(1 + org.opencv.videoio.Videoio.CAP_DSHOW);
        log.info("VideoCapture对象创建完成，isOpened={}", capture.isOpened());

        if (!capture.isOpened()) {
            throw new RuntimeException("无法打开摄像头（设备索引1），请检查摄像头是否连接");
        }

        // 优化参数：缓冲区只留1帧，减少延迟
        capture.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1);
        capture.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 25);

        // 获取摄像头原始分辨率
        double width = capture.get(3);
        double height = capture.get(4);
        double fps = capture.get(5);

        log.info("摄像头初始化成功 | 原始分辨率: {}x{} | 帧率: {}", (int) width, (int) height, fps);
        cameraInfo.set(new CameraInfo(TARGET_WIDTH, TARGET_HEIGHT, fps, TARGET_WIDTH + "x" + TARGET_HEIGHT, true));
    }

    /**
     * 启动后台拉流线程
     * 持续从摄像头读取帧，检测人脸并绘制矩形框，压缩为JPEG并缓存到latestFrame
     */
    private void startGrabThread() {
        isRunning.set(true);
        grabThread = new Thread(() -> {
            log.info("视频拉流线程启动");
            Mat frame = new Mat();
            Mat resized = new Mat();
            MatOfByte buf = new MatOfByte();
            MatOfInt compressionParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, JPEG_QUALITY);

            long lastFrameTime = System.currentTimeMillis();

            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (capture == null || !capture.isOpened()) {
                        Thread.sleep(1000);
                        reconnect();
                        continue;
                    }

                    if (!capture.read(frame) || frame.empty()) {
                        Thread.sleep(5);
                        continue;
                    }

                    frameCount++;
                    Rect faceRect = null;
                    if (frameCount % DETECT_INTERVAL == 1) {
                        faceRect = detectAndDrawFaces(frame);
                        lastFrameHasFace = (faceRect != null);
                        if (faceRect != null) {
                            latestFaceRect.set(faceRect);
                        }
                    } else if (lastFrameHasFace) {
                        faceRect = latestFaceRect.get();
                        if (faceRect != null) {
                            Imgproc.rectangle(frame, new Point(faceRect.x, faceRect.y),
                                    new Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                                    new Scalar(0, 255, 0), 2);
                        }
                    }

                    Imgproc.resize(frame, resized, new Size(TARGET_WIDTH, TARGET_HEIGHT));
                    Imgcodecs.imencode(".jpg", resized, buf, compressionParams);
                    byte[] jpeg = buf.toArray();

                    if (jpeg.length > 5000) {
                        latestFrame.set(jpeg);
                        lastFrameTime = System.currentTimeMillis();
                    }

                    if (System.currentTimeMillis() - lastFrameTime > 5000) {
                        reconnect();
                        lastFrameTime = System.currentTimeMillis();
                    }

                } catch (Exception e) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }

            try { frame.release(); } catch (Exception ignored) {}
            try { resized.release(); } catch (Exception ignored) {}
            try { buf.release(); } catch (Exception ignored) {}

            log.info("视频拉流线程停止");
        }, "video-grab-thread");

        grabThread.setDaemon(true);
        grabThread.setPriority(Thread.MAX_PRIORITY);
        grabThread.start();
    }

    /**
     * 使用Haar级联检测人脸并在帧上绘制绿色矩形框
     * @return 检测到的人脸矩形（未检测到返回null）
     */
    private Rect detectAndDrawFaces(Mat frame) {
        if (detector == null || frame.empty()) {
            return null;
        }

        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faces = new MatOfRect();
            detector.detectMultiScale(gray, faces, 1.15, 2, 0, new Size(80, 80), new Size(400, 400));

            Rect firstFace = null;
            if (!faces.empty()) {
                java.util.List<Rect> faceList = faces.toList();
                firstFace = faceList.get(0);
                for (Rect r : faceList) {
                    Imgproc.rectangle(frame, new Point(r.x, r.y),
                            new Point(r.x + r.width, r.y + r.height),
                            new Scalar(0, 255, 0), 2);
                }
            }

            faces.release();
            gray.release();
            return firstFace;

        } catch (Exception e) {
            log.debug("人脸检测异常: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 重连摄像头
     * 最多尝试3次
     */
    private synchronized void reconnect() {
        for (int i = 0; i < 3; i++) {
            try {
                releaseCapture();
                Thread.sleep(2000);
                initCapture();
                log.info("摄像头重连成功");
                return;
            } catch (Exception e) {
                log.error("摄像头重连失败 ({}): {}", i + 1, e.getMessage());
            }
        }
        log.error("摄像头重连失败，已达最大重试次数");
    }

    /**
     * 释放摄像头资源
     */
    private void releaseCapture() {
        if (capture != null) {
            try {
                capture.release();
            } catch (Exception ignored) {}
            capture = null;
        }
    }

    @Override
    public byte[] getLatestJpeg() {
        ensureCameraStarted();
        return latestFrame.get();
    }

    @Override
    public FaceFrameInfo getLatestFrameWithInfo() {
        ensureCameraStarted();
        byte[] jpeg = latestFrame.get();
        if (jpeg == null || jpeg.length == 0) {
            return null;
        }

        FaceFrameInfo info = new FaceFrameInfo();
        info.setJpegBase64(java.util.Base64.getEncoder().encodeToString(jpeg));
        info.setHasFace(lastFrameHasFace);

        Rect rect = latestFaceRect.get();
        if (rect != null && lastFrameHasFace) {
            FaceFrameInfo.FaceRect face = new FaceFrameInfo.FaceRect(rect.x, rect.y, rect.width, rect.height);
            info.setFace(face);
        }

        return info;
    }

    @Override
    public void releaseCamera() {
        initialized = false;
        isRunning.set(false);
        if (grabThread != null) {
            grabThread.interrupt();
            try {
                grabThread.join(3000);
            } catch (Exception ignored) {}
            grabThread = null;
        }
        releaseCapture();
        latestFrame.set(null);
        cameraInfo.set(new CameraInfo(0, 0, 0, "", false));
        log.info("摄像头资源已释放");
    }

    @PreDestroy
    public void destroy() {
        releaseCamera();
    }

    @Override
    public Result reinitCamera() {
        try {
            releaseCamera();
            Thread.sleep(500);
            initCapture();
            startGrabThread();
            initialized = true;
            log.info("摄像头重启成功");
            return Result.success("摄像头重启成功");
        } catch (Exception e) {
            log.error("摄像头重启失败: {}", e.getMessage());
            return Result.error(500, "重启失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isRunning() {
        return initialized && isRunning.get();
    }

    @Override
    public CameraInfo getCameraInfo() {
        CameraInfo info = cameraInfo.get();
        info.setRunning(isRunning());
        return info;
    }
}