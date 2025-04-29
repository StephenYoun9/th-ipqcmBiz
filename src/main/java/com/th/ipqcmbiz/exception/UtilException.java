package com.th.ipqcmbiz.exception;

/**
 * @ClassName UtilException
 * @Description 工具类异常
 * @Author 杨兴明
 * @Date 2025/4/23 11:03
 * @Version 1.0
 */
public class UtilException extends RuntimeException {
    private static final long serialVersionUID = 8247610319171014183L;

    public UtilException(Throwable e) {
        super(e.getMessage(), e);
    }

    public UtilException(String message) {
        super(message);
    }

    public UtilException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
