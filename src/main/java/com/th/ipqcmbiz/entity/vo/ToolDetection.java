package com.th.ipqcmbiz.entity.vo;

import lombok.Data;

/**
 * 单个工具检测结果
 */
@Data
public class ToolDetection {

    /** 工具标签 (wrench/screwdriver/pliers) */
    private String label;

    /** 置信度 (0-1) */
    private float confidence;

    /** 边界框左上角X坐标 */
    private int x1;

    /** 边界框左上角Y坐标 */
    private int y1;

    /** 边界框右下角X坐标 */
    private int x2;

    /** 边界框右下角Y坐标 */
    private int y2;

    public ToolDetection() {}

    public ToolDetection(String label, float confidence, int x1, int y1, int x2, int y2) {
        this.label = label;
        this.confidence = confidence;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
}
