package com.th.ipqcmbiz.entity.po;

import lombok.Data;
import java.util.Date;

@Data
public class FaceFeatureDO {
    private Long id;
    private String userId;
    private Integer faceIndex;
    private byte[] featureVector;
    private byte[] faceImage;
    private Float qualityScore;
    private Float threshold;
    private Date createTime;
}