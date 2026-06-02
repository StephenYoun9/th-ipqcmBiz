package com.th.ipqcmbiz.entity.vo;

import lombok.Data;
import java.util.List;

/**
 * 工具识别帧信息
 * 包含视频帧图片和检测结果
 */
@Data
public class ToolFrameInfo {

    /** Base64编码的JPEG图片 */
    private String jpegBase64;

    /** 检测到的工具列表 */
    private List<ToolDetection> detections;

    /** 检测到的工具名称列表（去重） */
    private List<String> detectedTools;

    /** 是否有检测结果 */
    private boolean hasDetection;
}
