package com.th.ipqcmbiz.recognition;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class ContourRecognition {
    static {
        System.loadLibrary("opencv_java480");
    }

    public static void main(String[] args) {
        // 1. 读取图片（替换为你的图片路径）
        String imgPath = "src/main/test.jpg"; // 建议用纯色背景的物体图片
        Mat srcImg = Imgcodecs.imread(imgPath);
        if (srcImg.empty()) {
            System.out.println("读取图片失败！");
            return;
        }

        // 2. 预处理：转灰度图 → 高斯模糊 → 边缘检测
        Mat grayImg = new Mat();
        Imgproc.cvtColor(srcImg, grayImg, Imgproc.COLOR_BGR2GRAY);
        // 高斯模糊（降噪）
        Imgproc.GaussianBlur(grayImg, grayImg, new Size(5, 5), 0);
        // Canny边缘检测
        Mat edgeImg = new Mat();
        Imgproc.Canny(grayImg, edgeImg, 50, 150);

        // 3. 查找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
                edgeImg.clone(),    // 边缘图（需克隆，避免原数据被修改）
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,  // 只检测最外层轮廓
                Imgproc.CHAIN_APPROX_SIMPLE // 压缩轮廓点
        );

        // 4. 绘制轮廓（绿色）
        for (MatOfPoint contour : contours) {
            // 过滤小轮廓（避免识别噪点）
            double area = Imgproc.contourArea(contour);
            if (area > 100) { // 面积阈值，可根据实际调整
                Imgproc.drawContours(
                        srcImg,
                        contours,
                        contours.indexOf(contour),
                        new Scalar(0, 255, 0), // 绿色
                        2
                );
                // 计算轮廓外接矩形并标注
                Rect rect = Imgproc.boundingRect(contour);
                Imgproc.putText(
                        srcImg,
                        "轮廓(" + (int) area + ")",
                        new Point(rect.x, rect.y - 5),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.5,
                        new Scalar(0, 255, 0),
                        1
                );
            }
        }

        // 5. 保存/显示结果
        Imgcodecs.imwrite("result.jpg", srcImg);
        org.opencv.highgui.HighGui.imshow("轮廓识别结果", srcImg);
        org.opencv.highgui.HighGui.waitKey(0);
        org.opencv.highgui.HighGui.destroyAllWindows();
    }
}