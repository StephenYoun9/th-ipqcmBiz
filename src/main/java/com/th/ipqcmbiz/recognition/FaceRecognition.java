package com.th.ipqcmbiz.recognition;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * OpenCV 4.8 人脸实时识别
 */
public class FaceRecognition {
    // 静态代码块加载 OpenCV 库
    static {
        System.loadLibrary("opencv_java480");
    }

    public static void main(String[] args) {
        // 1. 加载人脸分类器（替换为你的文件路径）
//        CascadeClassifier faceDetector = new CascadeClassifier(
//                "resources/haarcascade_frontalface_default.xml"
//        );
// Load from classpath (recommended for projects)
        String cascadePath = FaceRecognition.class.getClassLoader().getResource("haarcascade_frontalface_default.xml").getPath();
// Fix path encoding issue (for Windows)
        cascadePath = cascadePath.replaceFirst("/", "").replace("%20", " ");
        CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);
        // 2. 打开摄像头（0 表示默认摄像头）
        VideoCapture capture = new VideoCapture(0);
        // 设置摄像头分辨率
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

        if (!capture.isOpened()) {
            System.out.println("无法打开摄像头！");
            return;
        }

        // 3. 帧缓存对象
        Mat frame = new Mat();
        String windowName = "人脸识别窗口（按ESC退出）";
        org.opencv.highgui.HighGui.namedWindow(windowName);

        // 4. 循环读取帧并识别
        while (true) {
            // 读取摄像头帧
            capture.read(frame);
            if (frame.empty()) {
                System.out.println("读取帧失败！");
                break;
            }

            // 5. 预处理：转灰度图（提升识别效率）
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            // 直方图均衡化，增强对比度
            Imgproc.equalizeHist(grayFrame, grayFrame);

            // 6. 检测人脸
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(
                    grayFrame,          // 灰度图
                    faces,              // 检测结果（人脸矩形）
                    1.1,                // 缩放因子
                    5,                  // 邻域数阈值
                    0,                  // 附加参数
                    new Size(30, 30),   // 最小人脸尺寸
                    new Size(300, 300)  // 最大人脸尺寸
            );

            // 7. 绘制人脸框
            Rect[] faceArray = faces.toArray();
            for (Rect face : faceArray) {
                // 绘制红色矩形框（BGR格式，(0,0,255)为红色）
                Imgproc.rectangle(
                        frame,
                        face.tl(),  // 左上角点
                        face.br(),  // 右下角点
                        new Scalar(0, 0, 255),
                        2           // 线宽
                );
                // 标注"人脸"文字
                Imgproc.putText(
                        frame,
                        "人脸",
                        new Point(face.x, face.y - 5),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.8,
                        new Scalar(0, 0, 255),
                        2
                );
            }

            // 8. 显示帧
            org.opencv.highgui.HighGui.imshow(windowName, frame);

            // 按ESC键退出（ESC的ASCII码是27）
            if (org.opencv.highgui.HighGui.waitKey(1) == 27) {
                break;
            }
        }

        // 9. 释放资源
        capture.release();
        org.opencv.highgui.HighGui.destroyAllWindows();
    }
}