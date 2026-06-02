package com.th.ipqcmbiz.service.toolrecognition;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.ToolFrameInfo;

/**
 * 工具视频服务接口
 * 提供实时视频流和工具识别功能
 */
public interface ToolVideoService {

    /**
     * 获取最新JPEG视频帧（已标注检测框）
     */
    byte[] getLatestJpeg();

    /**
     * 获取最新帧及工具检测信息
     */
    ToolFrameInfo getLatestFrameWithInfo();

    /**
     * 释放摄像头资源
     */
    void releaseCamera();

    /**
     * 启动或重启摄像头
     */
    Result reinitCamera();

    /**
     * 摄像头是否正在运行
     */
    boolean isRunning();
}
