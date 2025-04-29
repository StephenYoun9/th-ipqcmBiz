package com.th.ipqcmbiz.enums.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "系统错误"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未授权访问"),
    ERROR(404, "接口无响应");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
