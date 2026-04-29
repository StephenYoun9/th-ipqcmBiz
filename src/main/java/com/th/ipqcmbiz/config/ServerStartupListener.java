package com.th.ipqcmbiz.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Slf4j
public class ServerStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final String REDIS_KEY_SERVER_START = "auth:server:starttime";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Long currentTime = System.currentTimeMillis();
        redisTemplate.opsForValue().set(REDIS_KEY_SERVER_START, currentTime);
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTime));
        log.info("服务启动时间已记录: {}", formattedTime);
    }
}