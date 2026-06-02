package com.th.ipqcmbiz.service.toolrecognition;

import lombok.Data;

@Data
public class RecognitionResult {
    private String label;
    private float confidence;
    private int x1;
    private int y1;
    private int x2;
    private int y2;
    private boolean detected;
}
