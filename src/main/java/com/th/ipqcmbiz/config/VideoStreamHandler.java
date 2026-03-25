package com.th.ipqcmbiz.config;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

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
        new Thread(() -> startStreaming(session, "C:\\Users\\23685\\Desktop\\yxm-Doc\\2.mp4")).start();
    }

    private void startStreaming(WebSocketSession session, String rtspUrl) {
        try {
            FFmpegLogCallback.set();  // 启用日志
            grabber = new FFmpegFrameGrabber(rtspUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setOption("stimeout", "5000000");
            grabber.setVideoCodec(avcodec.AV_CODEC_ID_H264);  // 强制 H.264 解码
            grabber.setVideoOption("bufsize", "20M");
            grabber.setVideoStream(0);  // 强制绑定 Stream #0:0（视频流）
            grabber.setVideoCodecName("h264");
            grabber.setVideoOption("profile:v", "high");  // 显式声明 Profile
            grabber.setVideoOption("coder:v", "0");       // 关闭熵编码器冲突
            grabber.setPixelFormat(AV_PIX_FMT_YUV420P);   // 匹配视频的像素格式
            grabber.setVideoOption("hwaccel", "none");  // 禁用硬件加速
            grabber.setVideoOption("threads", "1");
            grabber.start();
            //设置帧率参数，避免发送过快导致前端卡顿：
            grabber.setFrameRate(25);
            Frame frame;
            int retry = 0;
            while ((frame = grabber.grab()) != null && retry < 5) {
                if (frame.image != null && frame.keyFrame) {
                    System.out.println("成功获取视频帧");
                    break;
                }
                retry++;
            }
            if (frame == null || frame.image == null) {
                System.err.println("无有效帧：检查视频流或解码器兼容性");
            }
            if (frame != null) {
                // 转换为H.264 NAL单元
                ByteBuffer buffer = (ByteBuffer) frame.image[0].position(0);
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                // 发送二进制数据
                session.sendMessage(new BinaryMessage(data));
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