package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.FaceFrameInfo;
import com.th.ipqcmbiz.service.face.CameraInfo;
import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/face-video")
@Slf4j
public class FaceVideoController {

    @Resource
    private FaceVideoService faceVideoService;

    /**
     * 获取单帧JPEG图片
     * 用于轮询方式获取视频帧
     */
    @GetMapping(value = "/frame", produces = "image/jpeg")
    public void getFrame(HttpServletResponse response) {
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            byte[] jpeg = faceVideoService.getLatestJpeg();
            if (jpeg == null || jpeg.length == 0) {
                return;
            }
            response.setContentLength(jpeg.length);
            response.getOutputStream().write(jpeg);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.debug("获取视频帧失败: {}", e.getMessage());
        }
    }

    /**
     * 获取单帧JPEG图片及人脸检测信息
     * 返回Base64图片和人脸坐标
     */
    @GetMapping(value = "/frame-info")
    public Result getFrameWithInfo() {
        FaceFrameInfo info = faceVideoService.getLatestFrameWithInfo();
        if (info == null) {
            return Result.error(500, "无视频帧");
        }
        return Result.success(info);
    }

    /**
     * 获取摄像头状态
     */
    @GetMapping("/status")
    public Result getStatus() {
        CameraInfo info = faceVideoService.getCameraInfo();
        boolean running = faceVideoService.isRunning();
        return Result.success(info).setExtra("running", running);
    }

    /**
     * 启动/重启摄像头
     * 懒加载方式，首次调用时启动
     */
    @GetMapping("/start")
    public Result start() {
        return faceVideoService.reinitCamera();
    }

    /**
     * 释放摄像头资源
     */
    @GetMapping("/release")
    public Result release() {
        faceVideoService.releaseCamera();
        return Result.success("摄像头已释放");
    }
}