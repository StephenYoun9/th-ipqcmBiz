package com.th.ipqcmbiz.config;

import com.th.ipqcmbiz.service.face.FaceVideoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoStreamHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Resource
    private FaceVideoService faceVideoService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket客户端连接: {}, 当前连接数: {}", session.getId(), sessions.size());

        if (sessions.size() == 1) {
            faceVideoService.reinitCamera();
        }

        startStreaming();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket客户端断开: {}, 当前连接数: {}", session.getId(), sessions.size());

        if (sessions.isEmpty()) {
            log.info("无客户端连接，摄像头保持运行");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到消息: {}", payload);
    }

    private void startStreaming() {
        scheduler.submit(() -> {
            while (!sessions.isEmpty()) {
                try {
                    byte[] jpeg = faceVideoService.getLatestJpeg();
                    if (jpeg == null || jpeg.length == 0) {
                        Thread.sleep(10);
                        continue;
                    }

                    BinaryMessage msg = new BinaryMessage(jpeg);
                    for (WebSocketSession session : sessions) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(msg);
                            } catch (IOException e) {
                                log.debug("发送帧失败: {}", e.getMessage());
                            }
                        }
                    }
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.debug("推送视频流异常: {}", e.getMessage());
                }
            }
        });
    }

    public static int getClientCount() {
        return sessions.size();
    }
}