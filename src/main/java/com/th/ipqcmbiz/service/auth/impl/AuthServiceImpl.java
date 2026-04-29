package com.th.ipqcmbiz.service.auth.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.th.ipqcmbiz.entity.po.LoginTokenDO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.mapper.LoginTokenMapper;
import com.th.ipqcmbiz.service.auth.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String REDIS_KEY_TOKEN_PREFIX = "auth:token:";
    private static final String REDIS_KEY_USER_PREFIX = "auth:user:";
    private static final String REDIS_KEY_SERVER_START = "auth:server:starttime";

    @Value("${auth.token.expire-minutes:30}")
    private int tokenExpireMinutes;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private LoginTokenMapper loginTokenMapper;

    @Override
    public String createToken(String userId, UserInfoRespVO userInfo) {
        invalidateUserTokens(userId);

        String token = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + tokenExpireMinutes * 60 * 1000L);

        LoginTokenDO tokenDO = LoginTokenDO.builder()
                .userId(userId)
                .token(token)
                .lastActivity(now)
                .createTime(now)
                .expireTime(expireTime)
                .build();

        loginTokenMapper.insert(tokenDO);

        JSONObject tokenData = new JSONObject();
        tokenData.put("userId", userId);
        tokenData.put("userInfo", userInfo);
        tokenData.put("createTime", now.getTime());
        redisTemplate.opsForValue().set(REDIS_KEY_TOKEN_PREFIX + token, tokenData, tokenExpireMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(REDIS_KEY_USER_PREFIX + userId, token, tokenExpireMinutes, TimeUnit.MINUTES);

        log.debug("创建Token成功, userId={}, token={}", userId, token);
        return token;
    }

    @Override
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            Date lastActivity = new Date();
            redisTemplate.expire(redisKey, tokenExpireMinutes, TimeUnit.MINUTES);
            String userId = getUserIdFromTokenData(cached);
            if (userId != null) {
                redisTemplate.expire(REDIS_KEY_USER_PREFIX + userId, tokenExpireMinutes, TimeUnit.MINUTES);
            }
            loginTokenMapper.updateLastActivity(token, lastActivity);
            return true;
        }

        LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
        if (tokenDO == null) {
            return false;
        }

        if (isTokenExpired(token)) {
            invalidateToken(token);
            return false;
        }

        if (isTokenCreatedBeforeServerRestart(token)) {
            invalidateToken(token);
            return false;
        }

        Date now = new Date();
        redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(tokenDO), tokenExpireMinutes, TimeUnit.MINUTES);
        String userId = tokenDO.getUserId();
        redisTemplate.opsForValue().set(REDIS_KEY_USER_PREFIX + userId, token, tokenExpireMinutes, TimeUnit.MINUTES);
        loginTokenMapper.updateLastActivity(token, now);

        return true;
    }

    @Override
    public boolean refreshToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(redisKey);

        if (cached == null) {
            LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
            if (tokenDO == null || isTokenExpired(token) || isTokenCreatedBeforeServerRestart(token)) {
                return false;
            }
        }

        Date lastActivity = new Date();
        redisTemplate.expire(redisKey, tokenExpireMinutes, TimeUnit.MINUTES);
        String userId = getUserIdFromTokenData(cached);
        if (userId != null) {
            redisTemplate.expire(REDIS_KEY_USER_PREFIX + userId, tokenExpireMinutes, TimeUnit.MINUTES);
        }
        loginTokenMapper.updateLastActivity(token, lastActivity);

        log.debug("Token续期成功, token={}", token);
        return true;
    }

    @Override
    public void invalidateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
        if (tokenDO != null) {
            redisTemplate.delete(REDIS_KEY_TOKEN_PREFIX + token);
            redisTemplate.delete(REDIS_KEY_USER_PREFIX + tokenDO.getUserId());
            loginTokenMapper.deleteByToken(token);
            log.debug("失效Token成功, userId={}, token={}", tokenDO.getUserId(), token);
        }
    }

    @Override
    public void invalidateUserTokens(String userId) {
        LoginTokenDO existingToken = loginTokenMapper.selectByUserId(userId);
        if (existingToken != null) {
            redisTemplate.delete(REDIS_KEY_TOKEN_PREFIX + existingToken.getToken());
            redisTemplate.delete(REDIS_KEY_USER_PREFIX + userId);
            loginTokenMapper.deleteByUserId(userId);
            log.debug("失效用户所有Token, userId={}", userId);
        }
    }

    @Override
    public Long getServerStartTime() {
        Object startTime = redisTemplate.opsForValue().get(REDIS_KEY_SERVER_START);
        if (startTime == null) {
            return null;
        }
        if (startTime instanceof Long) {
            return (Long) startTime;
        }
        return Long.parseLong(startTime.toString());
    }

    @Override
    public boolean isTokenCreatedBeforeServerRestart(String token) {
        Long serverStartTime = getServerStartTime();
        if (serverStartTime == null) {
            return false;
        }

        String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(redisKey);

        Long tokenCreateTime = null;
        if (cached != null) {
            tokenCreateTime = getCreateTimeFromTokenData(cached);
        } else {
            LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
            if (tokenDO != null && tokenDO.getCreateTime() != null) {
                tokenCreateTime = tokenDO.getCreateTime().getTime();
            }
        }

        if (tokenCreateTime == null) {
            return true;
        }

        return tokenCreateTime < serverStartTime;
    }

    @Override
    public boolean isTokenExpired(String token) {
        String redisKey = REDIS_KEY_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            return false;
        }

        LoginTokenDO tokenDO = loginTokenMapper.selectByToken(token);
        if (tokenDO == null) {
            return true;
        }

        Date now = new Date();
        long diff = now.getTime() - tokenDO.getLastActivity().getTime();
        return diff > tokenExpireMinutes * 60 * 1000L;
    }

    private String getUserIdFromTokenData(Object data) {
        if (data == null) {
            return null;
        }
        try {
            if (data instanceof String) {
                JSONObject json = JSON.parseObject((String) data);
                return json.getString("userId");
            } else if (data instanceof JSONObject) {
                return ((JSONObject) data).getString("userId");
            }
        } catch (Exception e) {
            log.warn("解析Token数据失败", e);
        }
        return null;
    }

    private Long getCreateTimeFromTokenData(Object data) {
        if (data == null) {
            return null;
        }
        try {
            if (data instanceof String) {
                JSONObject json = JSON.parseObject((String) data);
                return json.getLong("createTime");
            } else if (data instanceof JSONObject) {
                return ((JSONObject) data).getLong("createTime");
            }
        } catch (Exception e) {
            log.warn("解析Token创建时间失败", e);
        }
        return null;
    }
}