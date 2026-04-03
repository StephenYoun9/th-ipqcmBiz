package com.th.ipqcmbiz.exception;

/**
 * @ClassName BusinessException
 * @Description 业务异常
 * @Author 杨兴明
 * @Date 2026/3/30 13:20
 * @Version 1.0
 */
public class BusinessException extends RuntimeException{

    private int code;

    // 构造方法
    public BusinessException(String msg) {
        super(msg);
        this.code = 500; // 默认错误码
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
