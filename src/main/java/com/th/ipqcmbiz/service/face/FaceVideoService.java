package com.th.ipqcmbiz.service.face;

import com.th.ipqcmbiz.entity.common.Result;

import java.awt.image.BufferedImage;

/**
 * @ClassName FaceVideoService
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/14 13:27
 * @Version 1.0
 */
public interface FaceVideoService {

    /**
     * 按需获取一帧（同步阻塞）
     * @return JPEG字节数组
     */
    byte[] grabFrame();

    /**
     * 获取最新JPEG视频帧
     * @return JPEG字节数组
     */
    byte[] getLatestJpeg();

    /**
     * 获取最新视频帧
     */
    BufferedImage getFrame();

    /**
     * 获取预压缩的JPEG视频帧
     */
    byte[] getLatestJpegFrame();

    /**
     * 获取最新图片格式的视频帧
     */
    BufferedImage getLatestFrameImage();

    /**
     * 释放摄像头资源
     */
    void releaseCamera();

    /**
     * 重启摄像头
     */
    Result reinitCamera();

    /**
     * 摄像头是否正在运行
     */
    boolean isRunning();

    /**
     * 获取摄像头信息
     */
    CameraInfo getCameraInfo();
}