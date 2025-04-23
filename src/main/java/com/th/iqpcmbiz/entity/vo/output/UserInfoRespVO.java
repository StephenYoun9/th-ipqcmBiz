package com.th.iqpcmbiz.entity.vo.output;

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

    @Schema(description = "用户编号", example = "000001")
    private String userId;

    @Schema(description = "用户姓名", example = "张三")
    private String userName;
}
