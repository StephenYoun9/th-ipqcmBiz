package com.th.ipqcmbiz.entity.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private Object extra;

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, null);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, null);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    public static <T> Result<T> error(int code, String msg, T data) {
        return new Result<>(code, msg, data, null);
    }

    public Result<T> setExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new java.util.HashMap<>();
        }
        ((java.util.Map<String, Object>) this.extra).put(key, value);
        return this;
    }
}
