package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDO {
    /**
     * 用户编号
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 状态 1-正常 0-禁用
     */
    private Integer status;

    /**
     * 用户角色；0：管理员；1：普通员工
     */
    private Integer userRole;

    /**
     * 人脸录入状态
     */
    private String faceRegistered;
}
