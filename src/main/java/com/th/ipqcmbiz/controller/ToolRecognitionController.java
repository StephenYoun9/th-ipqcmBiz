package com.th.ipqcmbiz.controller;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.ToolFrameInfo;
import com.th.ipqcmbiz.service.toolrecognition.ToolVideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工具识别控制器
 * 提供视频流和识别结果的REST接口
 */
@RestController
@RequestMapping("/tool-recognition")
@Slf4j
public class ToolRecognitionController {

    @Resource
    private ToolVideoService toolVideoService;

    /**
     * 获取单帧JPEG图片
     */
    @GetMapping(value = "/frame", produces = "image/jpeg")
    public void getFrame(HttpServletResponse response) {
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            byte[] jpeg = toolVideoService.getLatestJpeg();
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
     * 获取单帧图片及工具检测信息
     */
    @GetMapping(value = "/frame-info")
    public Result getFrameWithInfo() {
        ToolFrameInfo info = toolVideoService.getLatestFrameWithInfo();
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
        return Result.success().setExtra("running", toolVideoService.isRunning());
    }

    /**
     * 启动或重启摄像头
     */
    @GetMapping("/start")
    public Result start() {
        return toolVideoService.reinitCamera();
    }

    /**
     * 释放摄像头资源
     */
    @GetMapping("/release")
    public Result release() {
        toolVideoService.releaseCamera();
        return Result.success("摄像头已释放");
    }
}
