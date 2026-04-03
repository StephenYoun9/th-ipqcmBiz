package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName FaceInfoDO
 * @Description DO
 * @Author 杨兴明
 * @Date 2026/3/27 10:08
 * @Version 1.0
 */
@Data
@Builder
public class FaceInfoDO {
    /**
     * 编号
     */
    private Long id;

    /**
     * 用户名
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 人脸特征向量
     */
    private byte[] faceFeature;

    /**
     * 方向z
     */
    private String direction;

    /**
     * 创建时间
     */
    private Date createTime;
}
