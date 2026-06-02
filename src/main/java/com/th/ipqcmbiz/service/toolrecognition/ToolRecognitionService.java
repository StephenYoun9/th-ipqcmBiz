package com.th.ipqcmbiz.service.toolrecognition;

import java.util.List;

public interface ToolRecognitionService {

    List<RecognitionResult> recognizeFromImage(byte[] imageData);

    List<RecognitionResult> recognizeFromFile(String filePath);
}
