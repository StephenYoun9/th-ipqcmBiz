package com.th.ipqcmbiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @ClassName WebSocketConfig
 * @Description WebSocket配置类
 * @Author 杨兴明
 * @Date 2025/4/28 13:06
 * @Version 1.0
 */

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoStreamHandler(), "/video-stream")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler videoStreamHandler() {
        return new VideoStreamHandler();
    }
}
