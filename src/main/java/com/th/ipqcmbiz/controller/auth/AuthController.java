package com.th.ipqcmbiz.controller.auth;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private static final String AUTH_HEADER = "X-Auth-Token";
    private static final String REDIS_KEY_SERVER_START = "auth:server:starttime";

    @Value("${auth.token.header-name:X-Auth-Token}")
    private String authHeaderName;

    @Resource
    private AuthService authService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "发送心跳", description = "更新活动时间，防止会话超时")
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(HttpServletRequest request) {
        String token = request.getHeader(authHeaderName);
        if (!StringUtils.hasText(token)) {
            return error(401, "未提供认证Token");
        }

        boolean success = authService.refreshToken(token);
        if (!success) {
            return error(401, "Token无效或已过期");
        }
        return Result.success();
    }

    @Operation(summary = "检查Token状态", description = "检查当前Token是否有效")
    @GetMapping("/check")
    public Result<Map<String, Object>> checkToken(HttpServletRequest request) {
        String token = request.getHeader(authHeaderName);
        Map<String, Object> result = new HashMap<>();

        if (!StringUtils.hasText(token)) {
            result.put("valid", false);
            result.put("needReLogin", true);
            result.put("reason", "未提供Token");
            return success(result);
        }

        boolean isValid = authService.validateToken(token);
        if (!isValid) {
            result.put("valid", false);
            result.put("needReLogin", true);
            result.put("reason", "Token无效或已过期");
            return success(result);
        }

        boolean needReLogin = authService.isTokenCreatedBeforeServerRestart(token);
        result.put("valid", !needReLogin);
        result.put("needReLogin", needReLogin);
        result.put("reason", needReLogin ? "服务已重启，请重新登录" : "Token有效");
        return success(result);
    }

    @Operation(summary = "获取服务启动时间", description = "用于检测服务是否重启过")
    @GetMapping("/server-starttime")
    public Result<Long> getServerStartTime() {
        Long startTime = authService.getServerStartTime();
        if (startTime == null) {
            return error(500, "服务启动时间未初始化");
        }
        return success(startTime);
    }

    @Operation(summary = "退出登录", description = "使当前Token失效")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(authHeaderName);
        if (StringUtils.hasText(token)) {
            authService.invalidateToken(token);
        }
        return Result.success();
    }
}