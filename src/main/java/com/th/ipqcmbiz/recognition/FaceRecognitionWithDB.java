package com.th.ipqcmbiz.recognition;

// 导入OpenCV核心类，用于矩阵、标量、图像处理等基础操作

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于OpenCV+Oracle数据库的实时人脸识别类
 * 功能：从Oracle数据库加载人脸特征模板，通过摄像头实时检测人脸并与模板匹配，识别人员身份
 */
public class FaceRecognitionWithDB {

    // 静态代码块：在类加载时执行，加载OpenCV的Java本地库（需确保opencv_java480.dll/.so文件存在）
    static {
        System.loadLibrary("opencv_java480");
    }

    // ====================== 数据库配置常量 ======================
    // Oracle数据库连接URL（格式：jdbc:oracle:thin:@//主机:端口/服务名）
    static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/orcl";
    // 数据库用户名
    static final String DB_USER = "C##IPQCM";
    // 数据库密码
    static final String DB_PASS = "IPQCM_123";

    // ====================== 全局变量 ======================
    // 人脸检测器（基于Haar级联分类器）
    static CascadeClassifier faceDetector;
    // 人脸特征索引与姓名的映射表（key：人脸列表索引，value：用户名）
    static Map<Integer, String> nameMap = new HashMap<>();
    // 存储从数据库加载的所有人脸特征矩阵（模板）
    static List<Mat> faceList = new ArrayList<>();

    /**
     * 主方法：程序入口
     * 执行流程：加载人脸检测器 → 从数据库加载人脸模板 → 启动摄像头实时识别
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 1. 加载人脸检测器（Haar级联分类器）
        // 获取人脸检测配置文件的路径（haarcascade_frontalface_default.xml是OpenCV自带的人脸检测模型）
        String cascadePath = FaceRecognition.class.getClassLoader().getResource("haarcascade_frontalface_default.xml").getPath();
        // 修复Windows系统下路径编码问题（去除路径开头的"/"，替换空格的编码%20为实际空格）
        cascadePath = cascadePath.replaceFirst("/", "").replace("%20", " ");
        // 初始化人脸检测器，传入配置文件路径
        faceDetector = new CascadeClassifier(cascadePath);

        // 2. 从数据库加载所有人脸特征模板
        loadAllFaces();

        // 3. 打开摄像头，启动实时人脸对比识别
        startCamera();
    }

    /**
     * 从Oracle数据库加载所有人脸特征模板
     * 功能：连接数据库，查询face_database表中的用户名和人脸特征，转换为Mat矩阵并存储
     */
    public static void loadAllFaces() {
        // try-with-resources语法：自动关闭Connection、Statement、ResultSet（实现AutoCloseable接口）
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             // 执行SQL查询：获取用户名和人脸特征字节数组
             ResultSet rs = stmt.executeQuery("SELECT username, facefeature FROM face_database")) {

            // 遍历查询结果集
            while (rs.next()) {
                // 获取用户名
                String name = rs.getString("username");
                // 获取人脸特征（数据库中存储的字节数组）
                byte[] bytes = rs.getBytes("facefeature");
                // 创建100x100的单通道灰度图矩阵（CV_8UC1：8位无符号字符，1通道）
                Mat faceMat = new Mat(100, 100, CvType.CV_8UC1);
                // 将字节数组写入Mat矩阵，作为人脸特征模板
                faceMat.put(0, 0, bytes);

                // 将人脸矩阵添加到模板列表
                faceList.add(faceMat);
                // 建立索引与姓名的映射（索引为列表最后一个元素的下标）
                nameMap.put(faceList.size() - 1, name);
            }
            // 打印加载结果，便于调试
            System.out.println("已加载数据库人脸：" + faceList.size() + " 张");
        } catch (Exception e) {
            // 捕获数据库连接/查询异常，打印堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 启动摄像头，实时检测并识别人脸
     * 流程：读取摄像头帧 → 灰度转换 → 人脸检测 → 人脸裁剪缩放 → 模板匹配 → 绘制识别结果 → 显示窗口
     */
    public static void startCamera() {
        // 初始化视频捕获对象，参数0表示使用默认摄像头（笔记本内置摄像头）
        VideoCapture cap = new VideoCapture(0);
        // 存储摄像头每一帧的彩色图像
        Mat frame = new Mat();
        // 存储灰度化后的图像（人脸检测和匹配需灰度图，减少计算量）
        Mat gray = new Mat();
        // 初始化Java字体（支持中文）
        Font font = new Font("微软雅黑", Font.PLAIN, 20); // 宋体/黑体也可，根据系统安装字体调整

        // 循环读取摄像头帧，直到手动退出
        while (true) {
            // 读取摄像头当前帧到frame矩阵
            cap.read(frame);
            // 如果帧为空（摄像头断开/异常），退出循环
            if (frame.empty()) break;

            // 将彩色帧转换为灰度图（COLOR_BGR2GRAY：BGR转灰度，OpenCV默认图像格式为BGR）
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            // 存储检测到的人脸区域（多个Rect，每个Rect表示一个人脸的坐标和尺寸）
            MatOfRect faces = new MatOfRect();
            // 检测灰度图中的人脸（detectMultiScale：多尺度检测，适配不同大小的人脸）添加参数过滤冗余框
            // 参数说明：
            // 1. 输入图像 2. 输出人脸框 3. 缩放因子（>1，越小检测越细但慢）
            // 4. 最小邻域数（越高过滤越强，建议3~5） 5. 最小人脸尺寸（过滤过小的误检框）
            faceDetector.detectMultiScale(
                    gray,
                    faces,
                    1.1,        // scaleFactor：缩放步长
                    5,          // minNeighbors：最小邻域数（核心！过滤冗余框）
                    0,          // flags：默认
                    new Size(80, 80) // minSize：最小人脸尺寸（过滤小噪点框）
            );

            // ========== 新增：合并重叠的人脸框 ==========
            List<Rect> faceRects = mergeOverlappingRects(faces.toList(), 0.5); // IOU阈值调整为0.3

            // 关键：将OpenCV的Mat转换为Java BufferedImage（支持中文绘制）
            BufferedImage bufImage = matToBufferedImage(frame);
            Graphics2D g2d = bufImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(font);

            // 遍历每一个检测到的人脸区域
            for (Rect rect : faceRects) {          // 新代码
                Mat current = new Mat(gray, rect);
                Imgproc.resize(current, current, new Size(100, 100));

                String name = match(current);
                Color color = name.equals("未知") ? Color.RED : Color.GREEN;
                g2d.setColor(color);

                // 绘制单个合并后的框（仅绘制一次）
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                int textX = rect.x;
                int textY = rect.y - 5;
                g2d.drawString(name, textX, textY);
            }

            // 释放绘图资源
            g2d.dispose();
            // 将绘制好中文的BufferedImage转回Mat
            frame = bufferedImageToMat(bufImage);

            // 显示识别窗口（窗口名：人脸识别（Oracle），显示内容：绘制了结果的彩色帧）
            org.opencv.highgui.HighGui.imshow("人脸识别（Oracle）", frame);
            // 等待按键输入，按ESC键（ASCII码27）退出循环
            if (org.opencv.highgui.HighGui.waitKey(1) == 27) break;
        }

        // 释放摄像头资源
        cap.release();
        // 销毁所有OpenCV窗口
        org.opencv.highgui.HighGui.destroyAllWindows();
    }

    /**
     * 模板匹配方法：将当前检测到的人脸与数据库中的模板对比，返回匹配度最高的姓名
     * @param current 当前检测到并缩放后的人脸矩阵（100x100灰度图）
     * @return 匹配结果（匹配成功返回用户名，否则返回"未知"）
     */
    public static String match(Mat current) {
        // 存储最高匹配相似度（归一化后0~1之间，值越高越相似）
        double maxSim = 0;
        // 存储最佳匹配的姓名，默认"未知"
        String bestName = "未知";

        // 遍历所有数据库中的人脸模板
        for (int i = 0; i < faceList.size(); i++) {
            // 存储模板匹配的结果矩阵
            Mat res = new Mat();
            // 执行模板匹配（参数：待匹配图像、模板图像、结果矩阵、匹配算法）
            // TM_CCOEFF_NORMED：归一化相关系数匹配法，结果范围[-1,1]，归一化后0~1，值越高匹配度越高
            Imgproc.matchTemplate(current, faceList.get(i), res, Imgproc.TM_CCOEFF_NORMED);
            // 获取匹配结果的最大值（最高相似度）
            double sim = Core.minMaxLoc(res).maxVal;

            // 筛选条件：相似度>0.6（阈值，可根据实际情况调整）且高于当前最高相似度
            if (sim > 0.6 && sim > maxSim) {
                // 更新最高相似度
                maxSim = sim;
                // 更新最佳匹配姓名
                bestName = nameMap.get(i);
            }
        }
        // 返回最终匹配结果
        return bestName;
    }

    /**
     * OpenCV Mat 转 Java BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer); // 提取Mat的字节数据
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }

    /**
     * Java BufferedImage 转 OpenCV Mat
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    // 新增：合并重叠Rect的工具方法（IOU交并比算法）
    private static List<Rect> mergeOverlappingRects(List<Rect> rects, double iouThreshold) {
        List<Rect> mergedRects = new ArrayList<>();
        if (rects.isEmpty()) return mergedRects;

        // 修复1：按面积降序排序（优先保留大框，过滤小的误检框）
        rects.sort((r1, r2) -> Integer.compare(r2.width * r2.height, r1.width * r1.height));

        // 修复2：使用“标记法”处理链式重叠，而非单次遍历
        boolean[] merged = new boolean[rects.size()];
        for (int i = 0; i < rects.size(); i++) {
            if (merged[i]) continue; // 已合并的框跳过
            Rect current = rects.get(i);
            for (int j = i + 1; j < rects.size(); j++) {
                if (merged[j]) continue;
                Rect next = rects.get(j);
                double iou = calculateIOU(current, next);
                if (iou > iouThreshold) {
                    // 合并框
                    int x = Math.min(current.x, next.x);
                    int y = Math.min(current.y, next.y);
                    int width = Math.max(current.x + current.width, next.x + next.width) - x;
                    int height = Math.max(current.y + current.height, next.y + next.height) - y;
                    current = new Rect(x, y, width, height);
                    merged[j] = true; // 标记为已合并
                }
            }
            mergedRects.add(current);
            merged[i] = true;
        }
        return mergedRects;
    }

    // 新增：计算两个Rect的IOU（交并比）
    private static double calculateIOU(Rect a, Rect b) {
        // 计算交集的坐标
        int interX = Math.max(a.x, b.x);
        int interY = Math.max(a.y, b.y);
        int interWidth = Math.min(a.x + a.width, b.x + b.width) - interX;
        int interHeight = Math.min(a.y + a.height, b.y + b.height) - interY;

        if (interWidth <= 0 || interHeight <= 0) return 0.0; // 无交集

        // 交集面积
        double interArea = interWidth * interHeight;
        // 并集面积 = A面积 + B面积 - 交集面积
        double unionArea = (a.width * a.height) + (b.width * b.height) - interArea;

        return interArea / unionArea; // IOU = 交集/并集
    }

}