package com.th.ipqcmbiz.interceptor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.th.ipqcmbiz.context.UserContext;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.LoginTokenDO;
import com.th.ipqcmbiz.mapper.LoginTokenMapper;
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

    @Resource
    private LoginTokenMapper loginTokenMapper;

    private static final List<String> EXACT_EXCLUDE_PATHS = Arrays.asList(
            "/",
            "/index.html",
            "/logout",
            "/error"
    );

    private static final List<String> PREFIX_EXCLUDE_PATHS = Arrays.asList(
            "/login",
            "/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/common/",
            "/face-video/"
    );

    private boolean isExcluded(String requestUri) {
        for (String excludePath : EXACT_EXCLUDE_PATHS) {
            if (requestUri.equals(excludePath)) {
                return true;
            }
        }
        for (String excludePath : PREFIX_EXCLUDE_PATHS) {
            if (requestUri.startsWith(excludePath)) {
                return true;
            }
        }
        if (requestUri.contains(".")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String token = request.getHeader(authHeaderName);
        log.debug("uri={}, token={}", requestUri, token);

        if (isExcluded(requestUri)) {
            return true;
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Token为空，拒绝访问");
            sendUnauthorizedResponse(response, "未提供认证Token");
            return false;
        }

        boolean isValid = authService.validateToken(token);
        if (!isValid) {
            log.warn("Token无效，拒绝访问");
            sendUnauthorizedResponse(response, "Token无效或已过期");
            return false;
        }

        String userId = getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
            request.setAttribute("token", token);
            UserContext.setUserId(userId);
        } else {
            log.warn("无法获取userId, token={}", token);
            sendUnauthorizedResponse(response, "用户未登录");
            return false;
        }

        return true;
    }

    private String getUserIdFromToken(String token) {
        try {
            String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return extractUserId(cached);
            }
        } catch (Exception e) {
            log.warn("Redis获取userId异常: {}", e.getMessage());
        }

        try {
            LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
            if (tokenDO != null) {
                return tokenDO.getUserId();
            }
        } catch (Exception e) {
            log.warn("从数据库获取userId异常: {}", e.getMessage());
        }
        return null;
    }

    private String extractUserId(Object data) {
        if (data instanceof String) {
            JSONObject json = JSON.parseObject((String) data);
            return json.getString("userId");
        } else if (data instanceof JSONObject) {
            return ((JSONObject) data).getString("userId");
        } else if (data instanceof LoginTokenDO) {
            return ((LoginTokenDO) data).getUserId();
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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}