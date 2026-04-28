package com.th.ipqcmbiz.entity.vo.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName UserInfoReqVO
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2025/4/18 11:14
 * @Version 1.0
 */
@Data
public class UserInfoRespVO {

    @Schema(description = "用户编号", example = "admin")
    private String userId;

    @Schema(description = "用户姓名", example = "管理员")
    private String userName;

    @Schema(description = "状态 1-正常 0-禁用", example = "1")
    private Integer status;

    @Schema(description = "用户角色；0：管理员；1：普通员工", example = "0")
    private Integer userRole;

    @JsonIgnore
    @Schema(description = "登录密码", example = "000000", hidden = true)
    private String password;

    @Schema(description = "人脸录入情况;Y：已录入；N：未录入", example = "Y")
    private Boolean faceRegistered;

    @Schema(description = "指纹录入情况;Y：已录入；N：未录入", example = "N")
    private Boolean fingerRegistered;
}
