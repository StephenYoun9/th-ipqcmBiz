package com.th.ipqcmbiz.service.toolrecognition.impl;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.ToolDetection;
import com.th.ipqcmbiz.entity.vo.ToolFrameInfo;
import com.th.ipqcmbiz.service.toolrecognition.ToolVideoService;
import com.th.ipqcmbiz.service.toolrecognition.YoloRecognitionUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 工具视频服务实现
 * 使用OpenCV VideoCapture获取视频流，YOLO进行工具识别
 */
@Service
@Slf4j
public class ToolVideoServiceImpl implements ToolVideoService {

    /** 摄像头设备编号 */
    @Value("${tool.camera.device-id:1}")
    private int cameraDeviceId;

    /** 识别置信度阈值 */
    @Value("${tool.recognition.confidence:0.75}")
    private float confidenceThreshold;

    /** YOLO识别工具类 */
    @Autowired
    private YoloRecognitionUtil yoloUtil;

    /** OpenCV VideoCapture对象 */
    private VideoCapture capture;

    /** 最新JPEG数据（已标注检测框） */
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();

    /** 最新检测结果 */
    private final AtomicReference<List<ToolDetection>> latestDetections = new AtomicReference<>(new ArrayList<>());

    /** 拉流线程是否运行中 */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 后台拉流线程 */
    private Thread grabThread;

    /** 是否已初始化 */
    private volatile boolean initialized = false;

    /** 初始化锁 */
    private final Object initLock = new Object();

    /** 目标输出分辨率 */
    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 480;

    /** JPEG压缩质量 */
    private static final int JPEG_QUALITY = 70;

    /** 检测跳帧间隔 */
    private static final int DETECT_INTERVAL = 4;

    /** 帧计数器 */
    private int frameCount = 0;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            log.info("OpenCV加载成功");
        } catch (Exception e) {
            log.error("OpenCV加载失败: {}", e.getMessage());
        }
    }

    /**
     * 确保摄像头已启动
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
     * 初始化摄像头
     */
    private void initCapture() throws Exception {
        log.info("正在打开摄像头（设备索引{} + CAP_DSHOW={}）...", cameraDeviceId, Videoio.CAP_DSHOW);

        Exception lastException = null;
        for (int i = 0; i < 3; i++) {
            try {
                releaseCapture();
                Thread.sleep(1000);

                int deviceIndex = cameraDeviceId + Videoio.CAP_DSHOW;
                log.info("第{}次尝试: 创建VideoCapture({})", i + 1, deviceIndex);
                capture = new VideoCapture(deviceIndex);

                if (capture.isOpened()) {
                    log.info("VideoCapture创建成功，已打开");
                    break;
                }

                log.warn("摄像头打开失败，第{}次重试...", i + 1);
                lastException = new RuntimeException("无法打开摄像头" + cameraDeviceId + "，请检查摄像头连接");
            } catch (Exception e) {
                lastException = e;
                log.warn("摄像头打开异常，第{}次重试: {}", i + 1, e.getMessage());
                releaseCapture();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("摄像头初始化被中断");
                }
            }
        }

        if (capture == null || !capture.isOpened()) {
            throw lastException != null ? lastException :
                new RuntimeException("无法打开摄像头" + cameraDeviceId + "，请检查摄像头连接");
        }

        capture.set(Videoio.CAP_PROP_BUFFERSIZE, 1);
        capture.set(Videoio.CAP_PROP_FPS, 25);

        double width = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double height = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);

        log.info("摄像头初始化成功 | 分辨率: {}x{} | 帧率: {}", (int) width, (int) height, fps);
    }

    /**
     * 启动后台拉流线程
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

            while (isRunning.get()) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("检测到中断信号，准备退出线程");
                        break;
                    }

                    if (capture == null || !capture.isOpened()) {
                        Thread.sleep(1000);
                        if (isRunning.get()) {
                            reconnect();
                        }
                        continue;
                    }

                    if (!capture.read(frame) || frame.empty()) {
                        Thread.sleep(5);
                        continue;
                    }

                    if (!isRunning.get()) {
                        break;
                    }

                    frameCount++;

                    // 每隔DETECT_INTERVAL帧进行一次识别
                    List<ToolDetection> detections = new ArrayList<>();
                    if (frameCount % DETECT_INTERVAL == 1) {
                        detections = yoloUtil.detect(frame, confidenceThreshold);
                        latestDetections.set(detections);
                    } else {
                        detections = latestDetections.get();
                    }

                    // 绘制检测框
                    for (ToolDetection det : detections) {
                        Scalar color = new Scalar(0, 255, 0);
                        Imgproc.rectangle(frame,
                                new Point(det.getX1(), det.getY1()),
                                new Point(det.getX2(), det.getY2()),
                                color, 2);

                        // 绘制标签背景
                        String label = det.getLabel() + " " + String.format("%.2f", det.getConfidence());
                        int[] baseline = new int[1];
                        Size textSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseline);

                        Imgproc.rectangle(frame,
                                new Point(det.getX1(), det.getY1() - textSize.height - 5),
                                new Point(det.getX1() + textSize.width, det.getY1()),
                                color, -1);

                        Imgproc.putText(frame, label,
                                new Point(det.getX1(), det.getY1() - 5),
                                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0), 1);
                    }

                    // 缩放并编码为JPEG
                    Imgproc.resize(frame, resized, new Size(TARGET_WIDTH, TARGET_HEIGHT));
                    Imgcodecs.imencode(".jpg", resized, buf, compressionParams);
                    byte[] jpeg = buf.toArray();

                    if (jpeg.length > 5000) {
                        latestFrame.set(jpeg);
                        lastFrameTime = System.currentTimeMillis();
                    }

                    // 超时重连
                    if (System.currentTimeMillis() - lastFrameTime > 5000) {
                        reconnect();
                        lastFrameTime = System.currentTimeMillis();
                    }

                } catch (Exception e) {
                    log.debug("帧处理异常: {}", e.getMessage());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }

            try {
                frame.release();
                resized.release();
                buf.release();
            } catch (Exception ignored) {
            }

            log.info("视频拉流线程停止");
        }, "tool-video-grab-thread");

        grabThread.setDaemon(true);
        grabThread.setPriority(Thread.MAX_PRIORITY);
        grabThread.start();
    }

    /**
     * 重连摄像头
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
     * 释放摄像头
     */
    private void releaseCapture() {
        if (capture != null) {
            try {
                capture.release();
            } catch (Exception ignored) {
            }
            capture = null;
        }
    }

    @Override
    public byte[] getLatestJpeg() {
        ensureCameraStarted();
        return latestFrame.get();
    }

    @Override
    public ToolFrameInfo getLatestFrameWithInfo() {
        ensureCameraStarted();
        byte[] jpeg = latestFrame.get();
        if (jpeg == null || jpeg.length == 0) {
            return null;
        }

        ToolFrameInfo info = new ToolFrameInfo();
        info.setJpegBase64(Base64.getEncoder().encodeToString(jpeg));

        List<ToolDetection> detections = latestDetections.get();
        info.setDetections(detections);
        info.setHasDetection(!detections.isEmpty());

        // 提取去重的工具名称列表
        List<String> tools = detections.stream()
                .map(ToolDetection::getLabel)
                .distinct()
                .collect(Collectors.toList());
        info.setDetectedTools(tools);

        return info;
    }

    @Override
    public void releaseCamera() {
        log.info("releaseCamera called: initialized={}, isRunning={}, grabThread={}",
                initialized, isRunning.get(), grabThread);
        initialized = false;
        isRunning.set(false);
        if (grabThread != null) {
            log.info("Interrupting grabThread, isAlive={}", grabThread.isAlive());
            grabThread.interrupt();
            try {
                grabThread.join(2000);
                log.info("grabThread.join(2000) done, isAlive={}", grabThread.isAlive());
            } catch (Exception e) {
                log.warn("join exception: {}", e.getMessage());
            }
            if (grabThread.isAlive()) {
                log.warn("grabThread still alive, forcing capture release to unblock read()");
                releaseCapture();
                try {
                    grabThread.join(3000);
                    log.info("grabThread.join(3000) after capture release done, isAlive={}", grabThread.isAlive());
                } catch (Exception e) {
                    log.warn("join exception: {}", e.getMessage());
                }
            }
            grabThread = null;
        } else {
            log.info("grabThread is null");
        }
        releaseCapture();
        latestFrame.set(null);
        latestDetections.set(new ArrayList<>());
        log.info("摄像头资源已释放");
    }

    @Override
    public Result reinitCamera() {
        try {
            releaseCamera();
            Thread.sleep(5000);
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

    @PreDestroy
    public void destroy() {
        releaseCamera();
    }
}
