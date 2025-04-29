package com.th.ipqcmbiz.entity.vo.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserInfoReqVO {

    @Schema(description = "用户编号", example = "000001")
    @NotBlank(message = "用户编号不能为空")
    @Size(min = 6, max = 10, message = "用户编号长度需在6到10字符之间")
    private String userId;

    @Schema(description = "用户姓名", example = "张三")
    private String userName;

}
