package com.th.ipqcmbiz.interceptor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.auth.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final String REDIS_KEY_TOKEN_PREFIX = "auth:token:";
    private static final String REDIS_KEY_USER_PREFIX = "auth:user:";

    @Value("${auth.token.header-name:X-Auth-Token}")
    private String authHeaderName;

    @Resource
    private AuthService authService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/",
            "/index.html",
            "/login/",
            "/logout",
            "/auth/heartbeat",
            "/auth/check",
            "/auth/server-starttime",
            "/swagger-ui",
            "/v3/api-docs",
            "/error",
            "/js/**",
            "/css/**",
            "/image/**",
            "/images/**",
            "/static/**"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();

        for (String excludePath : EXCLUDE_PATHS) {
            if (requestUri.contains(excludePath)) {
                return true;
            }
        }

        String token = request.getHeader(authHeaderName);

        if (!StringUtils.hasText(token)) {
            sendUnauthorizedResponse(response, "未提供认证Token");
            return false;
        }

        boolean isValid = authService.validateToken(token);
        if (!isValid) {
            sendUnauthorizedResponse(response, "Token无效或已过期");
            return false;
        }

        if (authService.isTokenCreatedBeforeServerRestart(token)) {
            sendUnauthorizedResponse(response, "服务已重启，请重新登录");
            return false;
        }

        String userId = getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
            request.setAttribute("token", token);
        }

        return true;
    }

    private String getUserIdFromToken(String token) {
        try {
            String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                if (cached instanceof String) {
                    JSONObject json = JSON.parseObject((String) cached);
                    return json.getString("userId");
                } else if (cached instanceof JSONObject) {
                    return ((JSONObject) cached).getString("userId");
                }
            }
        } catch (Exception e) {
            log.warn("获取Token用户ID失败", e);
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Result<?> errorResult = Result.error(401, message);
        response.getWriter().write(JSON.toJSONString(errorResult));
    }
}