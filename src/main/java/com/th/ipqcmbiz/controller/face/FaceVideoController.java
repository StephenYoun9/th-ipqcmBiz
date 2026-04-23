package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.face.CameraInfo;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/face-video")
@Slf4j
public class FaceVideoController {

    @Resource
    private FaceVideoService faceVideoService;

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
    @Value("${ffmpeg.path:C:/yxm/ffmpeg-n8.1-latest-win64-gpl-8.1/ffmpeg-n8.1-latest-win64-gpl-8.1/bin/ffmpeg.exe}")
    private String ffmpegPath;

    @GetMapping(value = "/frame", produces = "image/jpeg")
    public void getSingleFrame(HttpServletResponse response) {
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            byte[] jpeg = faceVideoService.getLatestJpeg();
            if (jpeg == null || jpeg.length == 0) {
                return;
            }
            response.setContentLength(jpeg.length);
            response.getOutputStream().write(jpeg);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.debug("获取单帧图片失败: {}", e.getMessage());
        }
    }

    @GetMapping(value = "/fast-frame", produces = "image/jpeg")
    public void getFastFrame(HttpServletResponse response) {
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Accel-Buffering", "no");

        try (OutputStream os = response.getOutputStream()) {
            byte[] jpeg = faceVideoService.grabFrame();
            if (jpeg == null || jpeg.length == 0) {
                return;
            }
            os.write(jpeg);
            os.flush();
        } catch (Exception e) {
            log.debug("获取快速帧失败: {}", e.getMessage());
        }
    }

    @GetMapping("/reinit")
    public Result reinit() {
        return faceVideoService.reinitCamera();
    }

    @GetMapping("/release")
    public Result release() {
        faceVideoService.releaseCamera();
        return Result.success("已释放");
    }

    @GetMapping("/status")
    public Result getStatus() {
        CameraInfo info = faceVideoService.getCameraInfo();
        boolean running = faceVideoService.isRunning();
        return Result.success(info).setExtra("running", running);
    }

    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace; boundary=frame")
    public void getStream(HttpServletResponse response) {
        response.setContentType("multipart/x-mixed-replace; boundary=frame");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Content-Type-Options", "nosniff");

        try (OutputStream os = response.getOutputStream()) {
            long lastFrameTime = 0;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] jpeg = faceVideoService.getLatestJpeg();
                if (jpeg == null || jpeg.length == 0) {
                    Thread.sleep(10);
                    continue;
                }

                long now = System.currentTimeMillis();
                if (now - lastFrameTime < 33) {
                    Thread.sleep(5);
                    continue;
                }
                lastFrameTime = now;

                os.write(("--frame\r\n" +
                        "Content-Type: image/jpeg\r\n" +
                        "Content-Length: " + jpeg.length + "\r\n\r\n").getBytes());
                os.write(jpeg);
                os.write("\r\n".getBytes());
                os.flush();
            }
        } catch (Exception e) {
            log.debug("流连接断开: {}", e.getMessage());
        }
    }

    @GetMapping(value = "/mjpeg", produces = "video/x-motion-jpeg")
    public void getMjpeg(HttpServletResponse response) {
        response.setContentType("video/x-motion-jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Content-Type-Options", "nosniff");

        String rtspUrl = String.format("rtsp://%s:%s@%s:%d%s",
                cameraUsername, cameraPassword, cameraIp, cameraPort, streamPath);

        String[] command = {
            ffmpegPath,
            "-rtsp_transport", "tcp",
            "-stimeout", "10000000",
            "-fflags", "nobuffer",
            "-flags", "low_delay",
            "-max_delay", "500000",
            "-probesize", "32",
            "-analyzeduration", "100000",
            "-sync", "video",
            "-an",
            "-i", rtspUrl,
            "-c:v", "mjpeg",
            "-q:v", "5",
            "-f", "mjpeg",
            "-"
        };

        log.info("FFmpeg MJPEG流命令: {}", String.join(" ", command));

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            InputStream inputStream = process.getInputStream();
            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[32768];

            while (!Thread.currentThread().isInterrupted() && process.isAlive()) {
                int len = inputStream.read(buffer);
                if (len > 0) {
                    os.write(buffer, 0, len);
                    os.flush();
                }
            }
        } catch (Exception e) {
            log.debug("MJPEG流异常: {}", e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }
}