package com.th.ipqcmbiz.entity.vo;

import lombok.Data;

@Data
public class FaceEnrollVO {
    private String enrollId;
    private String userId;
    private int captured;
    private int required;
    private float quality;
    private float similarity;
    private int progress;
    private boolean completed;
    private String message;
}