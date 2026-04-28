package com.th.ipqcmbiz.handler;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName GlobalExceptionHandler
 * @Description 全局异常处理器
 * @Author 杨兴明
 * @Date 2026/3/30 13:36
 * @Version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 捕获我们自定义的业务异常
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // 捕获所有其他异常（兜底）
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("服务器异常: ", e);
        return Result.error(500, "服务器异常：" + e.getMessage());
    }

    /**
     * 处理参数校验异常（MethodArgumentNotValidException）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 返回400状态码
    public Result<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 1. 解析所有校验失败的字段和提示信息
        Map<String, String> errorMap = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            // 获取字段名
            String fieldName = ((FieldError) error).getField();
            // 获取校验提示信息（对应VO中@NotBlank/@Size的message）
            String errorMessage = error.getDefaultMessage();
            errorMap.put(fieldName, errorMessage);
        });
        // 2. 打印日志（可选）
        log.error("参数校验失败：{}", errorMap);
        // 3. 返回标准化结果（Result是你项目中统一的返回体）
        return Result.error(400,"参数校验失败", errorMap);
    }
}
