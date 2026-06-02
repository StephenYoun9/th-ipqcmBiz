package com.th.ipqcmbiz.service.toolrecognition;

import com.th.ipqcmbiz.entity.vo.ToolDetection;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YOLO目标检测工具类
 * 通过HTTP调用Python YOLO服务进行推理
 */
@Component
@Slf4j
public class YoloRecognitionUtil {

    @Value("${tool.yolo-server.host}")
    private String serverHost;

    @Value("${tool.yolo-server.port}")
    private int serverPort;

    @Value("${tool.yolo-server.timeout}")
    private int timeoutMs;

    /** 工具类别标签（与模型训练时一致） */
    private static final String[] LABELS = {"wrench", "screwdriver", "pliers"};

    /** 置信度阈值 */
    private static final float CONFIDENCE_THRESHOLD = 0.5f;

    /** 模型是否已加载 */
    private boolean modelLoaded = false;

    /** 临时文件路径 */
    private String tempDir;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        tempDir = System.getProperty("java.io.tmpdir");
        modelLoaded = checkServerHealth();
    }

    /**
     * 检查服务是否可用
     */
    private boolean checkServerHealth() {
        try {
            URL url = new URL(String.format("http://%s:%d/health", serverHost, serverPort));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String response = reader.readLine();
                reader.close();
                conn.disconnect();

                if (response != null && response.contains("ok")) {
                    log.info("YOLO服务健康检查通过: {}:{}", serverHost, serverPort);
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("YOLO服务健康检查失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 检测目标
     * @param frame 输入图像帧
     * @param confidenceThreshold 置信度阈值
     * @return 检测结果列表
     */
    public List<ToolDetection> detect(Mat frame, float confidenceThreshold) {
        List<ToolDetection> results = new ArrayList<>();

        if (frame == null || frame.empty()) {
            return results;
        }

        String imagePath = null;
        try {
            // 保存图像到临时文件
            imagePath = tempDir + "/yolo_detect_" + System.currentTimeMillis() + ".jpg";
            Imgcodecs.imwrite(imagePath, frame);

            // 发送HTTP请求到Python服务
            String jsonResponse = sendDetectionRequest(imagePath, confidenceThreshold);

            // 解析JSON响应
            results = parseJsonResponse(jsonResponse);

            log.debug("检测到 {} 个目标", results.size());

        } catch (Exception e) {
            log.error("YOLO推理失败: {}", e.getMessage());
        } finally {
            // 清理临时文件
            if (imagePath != null) {
                new File(imagePath).delete();
            }
        }

        return results;
    }

    /**
     * 发送检测请求到Python服务
     */
    private String sendDetectionRequest(String imagePath, float confidence) throws Exception {
        URL url = new URL(String.format(
                "http://%s:%d/detect?confidence=%.2f",
                serverHost, serverPort, confidence));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        // 读取图片文件并发送
        try (FileInputStream fis = new FileInputStream(imagePath);
             OutputStream os = conn.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP错误: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        conn.disconnect();
        return response.toString();
    }

    /**
     * 解析JSON响应
     * 响应格式: {"success":true,"count":2,"detections":[{"label":"wrench","confidence":0.95,"x1":10,"y1":20,"x2":100,"y2":200}]}
     */
    private List<ToolDetection> parseJsonResponse(String json) {
        List<ToolDetection> results = new ArrayList<>();

        if (json == null || json.trim().isEmpty()) {
            return results;
        }

        try {
            // 检查success字段
            if (!json.contains("\"success\":true")) {
                log.warn("检测请求失败: {}", json);
                return results;
            }

            // 提取detections数组
            Pattern detectionsPattern = Pattern.compile("\"detections\"\\s*:\\s*\\[([^\\]]*)\\]");
            Matcher matcher = detectionsPattern.matcher(json);

            if (matcher.find()) {
                String detectionsStr = matcher.group(1);

                // 分割每个对象
                Pattern objPattern = Pattern.compile("\\{[^}]+\\}");
                Matcher objMatcher = objPattern.matcher(detectionsStr);

                while (objMatcher.find()) {
                    String obj = objMatcher.group();
                    ToolDetection detection = parseDetectionObject(obj);
                    if (detection != null) {
                        results.add(detection);
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析JSON响应失败: {}, 原始: {}", e.getMessage(), json);
        }

        return results;
    }

    /**
     * 解析单个检测对象
     */
    private ToolDetection parseDetectionObject(String json) {
        try {
            String label = extractString(json, "label");
            float confidence = (float) extractDouble(json, "confidence");
            int x1 = (int) extractDouble(json, "x1");
            int y1 = (int) extractDouble(json, "y1");
            int x2 = (int) extractDouble(json, "x2");
            int y2 = (int) extractDouble(json, "y2");

            // 验证标签
            boolean validLabel = false;
            for (String valid : LABELS) {
                if (valid.equalsIgnoreCase(label)) {
                    label = valid;
                    validLabel = true;
                    break;
                }
            }

            if (validLabel) {
                return new ToolDetection(label, confidence, x1, y1, x2, y2);
            }
        } catch (Exception e) {
            log.debug("解析检测对象失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 提取字符串值
     */
    private String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取double值
     */
    private double extractDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0;
    }

    /**
     * 检测目标（使用默认置信度阈值）
     */
    public List<ToolDetection> detect(Mat frame) {
        return detect(frame, CONFIDENCE_THRESHOLD);
    }

    /**
     * 模型是否已加载（服务是否可用）
     */
    public boolean isModelLoaded() {
        return modelLoaded;
    }
}