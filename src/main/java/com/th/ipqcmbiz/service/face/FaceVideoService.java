package com.th.ipqcmbiz.service.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.FaceFrameInfo;

/**
 * 摄像头视频服务接口
 * 使用本地摄像头（VideoCapture）获取视频流
 */
public interface FaceVideoService {

    /**
     * 获取最新JPEG视频帧
     * 首次调用时自动启动摄像头
     * @return JPEG字节数组
     */
    byte[] getLatestJpeg();

    /**
     * 获取最新帧及人脸检测信息
     * @return 帧信息（含Base64图片和人脸坐标）
     */
    FaceFrameInfo getLatestFrameWithInfo();

    /**
     * 释放摄像头资源
     * 释放后可重新调用getLatestJpeg启动
     */
    void releaseCamera();

    /**
     * 启动或重启摄像头
     * @return 操作结果
     */
    Result reinitCamera();

    /**
     * 摄像头是否正在运行
     * @return 是否运行中
     */
    boolean isRunning();

    /**
     * 获取摄像头信息（分辨率、帧率等）
     * @return 摄像头信息
     */
    CameraInfo getCameraInfo();
}