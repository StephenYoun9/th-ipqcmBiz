package com.th.ipqcmbiz.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;

/**
 * @ClassName USBCamera
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/24 09:32
 * @Version 1.0
 */
public class USBCamera {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        // ==================== 修复核心：强制使用 DSHOW，不使用 MSMF ====================
        VideoCapture capture = new VideoCapture(1 + CAP_DSHOW);

        if (!capture.isOpened()) {
            System.out.println("尝试切换摄像头 1");
            capture = new VideoCapture(1 + CAP_DSHOW);
        }

        Mat mat = new Mat();
        while (true) {
            capture.read(mat);
            if (!mat.empty()) {
                imshow("罗技摄像头已修复", mat);
            }
            if (waitKey(1) == 27) break;
        }
    }
}