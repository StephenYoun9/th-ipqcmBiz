package com.th.ipqcmbiz.service.face.impl;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.face.CameraInfo;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class FaceVideoServiceImpl implements FaceVideoService {

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

    private VideoCapture capture;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicReference<CameraInfo> cameraInfo = new AtomicReference<>(new CameraInfo(0, 0, 0, "", false));
    private Thread grabThread;
    private volatile boolean initialized = false;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            log.info("OpenCV加载成功");
        } catch (Exception e) {
            log.error("OpenCV加载失败: {}", e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        log.info("视频流组件初始化 (OpenCV VideoCapture)");
        try {
            initCapture();
            startGrabThread();
            initialized = true;
            log.info("摄像头初始化成功");
        } catch (Exception e) {
            log.error("摄像头初始化失败: {}", e.getMessage(), e);
        }
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
            log.error("摄像头重启失败: {}", e.getMessage(), e);
            return Result.error(500, "重启失败: " + e.getMessage());
        }
    }

    private void initCapture() throws Exception {
        String rtspUrl = String.format("rtsp://%s:%s@%s:%d%s",
                cameraUsername, cameraPassword, cameraIp, cameraPort, streamPath);
        log.info("RTSP地址: {}", rtspUrl);

        capture = new VideoCapture(rtspUrl);

        if (!capture.isOpened()) {
            throw new RuntimeException("无法打开RTSP流: " + rtspUrl);
        }

        capture.set(org.opencv.videoio.Videoio.CAP_PROP_BUFFERSIZE, 1);
        capture.set(org.opencv.videoio.Videoio.CAP_PROP_FPS, 25);

        double width = capture.get(3);
        double height = capture.get(4);
        double fps = capture.get(5);

        log.info("摄像头初始化成功 | 原始分辨率: {}x{} | 帧率: {}", (int) width, (int) height, fps);
        cameraInfo.set(new CameraInfo(960, 540, fps, "960x540", true));
    }

    private void startGrabThread() {
        isRunning.set(true);
        grabThread = new Thread(() -> {
            log.info("视频拉流线程启动");
            Mat frame = new Mat();
            Mat resized = new Mat();
            MatOfByte buf = new MatOfByte();
            MatOfInt compressionParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 60);
            long frameCount = 0;
            long lastLogTime = System.currentTimeMillis();
            long lastFrameTime = System.currentTimeMillis();

            int targetWidth = 960;
            int targetHeight = 540;

            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (capture == null || !capture.isOpened()) {
                        log.warn("VideoCapture已关闭，尝试重连...");
                        Thread.sleep(1000);
                        reconnect();
                        continue;
                    }

                    if (!capture.read(frame) || frame.empty()) {
                        Thread.sleep(5);
                        continue;
                    }

                    Imgproc.resize(frame, resized, new Size(targetWidth, targetHeight));
                    Imgcodecs.imencode(".jpg", resized, buf, compressionParams);
                    byte[] jpeg = buf.toArray();

                    if (jpeg.length > 5000) {
                        latestFrame.set(jpeg);
                        frameCount++;
                        lastFrameTime = System.currentTimeMillis();
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastLogTime > 5000) {
                        long elapsed = now - lastLogTime;
                        if (elapsed > 0) {
                            log.info("帧率: {} fps, 帧大小: {} bytes, 延迟: {}ms",
                                    frameCount * 1000 / elapsed,
                                    latestFrame.get() != null ? latestFrame.get().length : 0,
                                    now - lastFrameTime);
                        }
                        frameCount = 0;
                        lastLogTime = now;
                    }

                    if (now - lastFrameTime > 5000 && frameCount == 0) {
                        log.warn("5秒无新帧，尝试重连...");
                        reconnect();
                        lastFrameTime = now;
                    }

                } catch (Exception e) {
                    log.debug("抓取帧异常: {}", e.getMessage());
                    try { Thread.sleep(10); } catch (InterruptedException ignored) { break; }
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

    private synchronized void reconnect() {
        for (int i = 0; i < 3; i++) {
            try {
                releaseCapture();
                Thread.sleep(2000);
                initCapture();
                log.info("OpenCV重连成功");
                return;
            } catch (Exception e) {
                log.error("OpenCV重连失败 ({}): {}", i + 1, e.getMessage());
            }
        }
        log.error("OpenCV重连失败，已达最大重试次数");
    }

    private void releaseCapture() {
        if (capture != null) {
            try {
                capture.release();
            } catch (Exception ignored) {}
            capture = null;
        }
    }

    @Override
    public byte[] grabFrame() {
        byte[] frame = latestFrame.get();
        if (frame != null && frame.length > 0) {
            return frame;
        }
        log.debug("grabFrame返回缓存帧");
        return frame;
    }

    @Override
    public byte[] getLatestJpeg() {
        return latestFrame.get();
    }

    @Override
    public BufferedImage getLatestFrameImage() {
        byte[] jpeg = latestFrame.get();
        if (jpeg == null || jpeg.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(jpeg));
        } catch (Exception e) {
            log.debug("JPEG转图片失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void releaseCamera() {
        initialized = false;
        isRunning.set(false);
        if (grabThread != null) {
            grabThread.interrupt();
            try { grabThread.join(3000); } catch (Exception ignored) {}
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
    public BufferedImage getFrame() {
        return null;
    }

    @Override
    public byte[] getLatestJpegFrame() {
        return getLatestJpeg();
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