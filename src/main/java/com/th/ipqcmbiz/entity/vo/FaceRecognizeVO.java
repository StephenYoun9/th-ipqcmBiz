package com.th.ipqcmbiz.entity.vo;

import lombok.Data;

@Data
public class FaceRecognizeVO {
    private boolean matched;
    private String userId;
    private String userName;
    private float similarity;
    private float threshold;
    private String message;
}