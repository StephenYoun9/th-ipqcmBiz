package com.th.ipqcmbiz.config;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName VideoStreamHandler
 * @Description 视频流处理核心类
 * @Author 杨兴明
 * @Date 2025/4/28 13:16
 * @Version 1.0
 */
public class VideoStreamHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private FFmpegFrameGrabber grabber;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        // 启动拉流线程（示例：从RTSP摄像头拉流）
        new Thread(() -> startStreaming(session, "C:\\Users\\23685\\Videos\\1.mp4")).start();
    }

    private void startStreaming(WebSocketSession session, String rtspUrl) {
        try {
            grabber = new FFmpegFrameGrabber(rtspUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.start();
            //设置帧率参数，避免发送过快导致前端卡顿：
            grabber.setFrameRate(25);
            while (session.isOpen()) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    // 转换为H.264 NAL单元
                    ByteBuffer buffer = (ByteBuffer) frame.image[0].position(0);
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    // 发送二进制数据
                    session.sendMessage(new BinaryMessage(data));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeGrabber();
        }
    }

    private void closeGrabber() {
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}