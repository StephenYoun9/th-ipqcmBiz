package com.th.ipqcmbiz.service.toolrecognition.impl;

import com.th.ipqcmbiz.service.toolrecognition.RecognitionResult;
import com.th.ipqcmbiz.service.toolrecognition.ToolRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ToolRecognitionServiceImpl implements ToolRecognitionService {

    @Override
    public List<RecognitionResult> recognizeFromImage(byte[] imageData) {
        try {
            Path tempFile = Files.createTempFile("wrench_input_", ".jpg");
            Files.write(tempFile, imageData);

            String result = callPythonScript(tempFile.toString());
            Files.deleteIfExists(tempFile);

            return parsePythonResult(result);
        } catch (Exception e) {
            log.error("识别失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<RecognitionResult> recognizeFromFile(String filePath) {
        try {
            String result = callPythonScript(filePath);
            return parsePythonResult(result);
        } catch (Exception e) {
            log.error("识别失败", e);
            return new ArrayList<>();
        }
    }

    private String callPythonScript(String imagePath) throws Exception {
        String pythonExe = "C:/yxm/python/python.exe";

        List<String> command = new ArrayList<>();
        command.add(pythonExe);
        command.add("-c");
        command.add(buildPythonCommand(imagePath));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Python脚本退出码: {}", exitCode);
        }

        return output.toString();
    }

    private String buildPythonCommand(String imagePath) {
        return String.format(
            "import sys; sys.path.insert(0, 'C:/yxm/python/Lib/site-packages'); " +
            "from ultralytics import YOLO; " +
            "model = YOLO('C:/yxm/wrench_train/wrench_model-2/weights/best.pt'); " +
            "results = model('%s', verbose=False); " +
            "for r in results: " +
            "    boxes = r.boxes; " +
            "    for box in boxes: " +
            "        x1,y1,x2,y2 = box.xyxy[0].tolist(); " +
            "        conf = float(box.conf[0]); " +
            "        cls = int(box.cls[0]); " +
            "        print(f'DETECTED:{{x1},{y1},{x2},{y2},{conf},{cls}}')",
            imagePath.replace("\\", "\\\\")
        );
    }

    private List<RecognitionResult> parsePythonResult(String output) {
        List<RecognitionResult> results = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            return results;
        }

        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.startsWith("DETECTED:")) {
                String data = line.substring(9);
                String[] parts = data.split(",");
                if (parts.length >= 6) {
                    RecognitionResult result = new RecognitionResult();
                    result.setX1((int) Float.parseFloat(parts[0]));
                    result.setY1((int) Float.parseFloat(parts[1]));
                    result.setX2((int) Float.parseFloat(parts[2]));
                    result.setY2((int) Float.parseFloat(parts[3]));
                    result.setConfidence(Float.parseFloat(parts[4]));
                    result.setLabel("wrench");
                    result.setDetected(true);
                    results.add(result);
                }
            }
        }

        return results;
    }
}
