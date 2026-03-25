package com.th.ipqcmbiz.recognition;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * 人脸采集（修复卡帧 + 2秒间隔采集）
 * 功能：摄像头全程流畅运行，每2秒自动采集1张人脸存入Oracle
 * 解决：sleep导致的摄像头冻结、卡顿问题
 */
public class FaceRegister {

    static {
        // 加载OpenCV本地库（必须最先加载）
        System.loadLibrary("opencv_java480");
    }

    // ==================== Oracle 数据库配置 ====================
    static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/orcl";
    static final String DB_USER = "C##IPQCM";
    static final String DB_PASS = "IPQCM_123";

    // ==================== 采集参数设置 ====================
    static final int MAX_COUNT = 10;        // 总共采集10张人脸
    static final long INTERVAL = 2000;      // 采集间隔：2000毫秒 = 2秒

    public static void main(String[] args) {
        // 开始采集，传入要注册的姓名
        startCollect("杨兴明");
    }

    /**
     * 摄像头人脸采集（不卡帧版本）
     * @param userName 要录入的姓名
     */
    public static void startCollect(String userName) {
        // 1. 加载人脸检测模型（Haar分类器）
        String cascadePath = FaceRegister.class.getClassLoader().getResource("haarcascade_frontalface_default.xml").getPath();
        cascadePath = cascadePath.replaceFirst("/", "").replace("%20", " ");
        CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);

        // 2. 打开默认摄像头（0=默认摄像头）
        VideoCapture cap = new VideoCapture(0);
        Mat frame = new Mat(); // 存储每一帧画面
        int count = 0;         // 已采集数量

        // ==================== 关键修复：记录上一次采集时间 ====================
        long lastCollectTime = 0;

        System.out.println("人脸采集已启动，请正对摄像头...");

        // ==================== 摄像头主循环（全程流畅，绝不sleep） ====================
        while (count < MAX_COUNT) {
            // 读取摄像头帧（一直跑，不卡顿）
            cap.read(frame);
            if (frame.empty()) break;

            // 3. 转灰度图（人脸检测必须用灰度图）
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            // 4. 检测人脸
            MatOfRect faces = new MatOfRect();
            // 优化人脸检测参数（减少重复检测）,增加参数，减少多尺度检测的冗余）：
            faceDetector.detectMultiScale(
                    gray,            // 输入灰度图
                    faces,           // 输出人脸框
                    1.1,             // 尺度因子（scaleFactor，越大检测次数越少，默认1.1）
                    5,               // 邻域数（minNeighbors，越大筛选越严格，默认3）
                    0,               // 标志位（默认0）
                    new Size(80, 80),// 最小人脸尺寸（过滤过小的检测框）
                    new Size(300, 300)// 最大人脸尺寸（过滤过大的检测框）
            );

            // 5. 遍历检测到的人脸
            for (Rect rect : faces.toArray()) {
                // 画出绿色人脸框
                Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0), 2);

                // ==================== 核心：时间差判断，不卡帧 ====================
                long now = System.currentTimeMillis();
                if (now - lastCollectTime >= INTERVAL) {
                    // 裁剪人脸区域
                    Mat faceMat = new Mat(gray, rect);
                    Imgproc.resize(faceMat, faceMat, new Size(100, 100));

                    // 转字节数组
                    byte[] bytes = new byte[(int) (faceMat.total() * faceMat.channels())];
                    faceMat.get(0, 0, bytes);

                    // 保存到Oracle
                    saveToOracle(userName, bytes);

                    count++;
                    lastCollectTime = now; // 更新最后采集时间
                    System.out.println("已采集：" + count + "/" + MAX_COUNT);
                }
            }

            // 6. 实时显示画面（全程流畅）
            org.opencv.highgui.HighGui.imshow("人脸采集（流畅版）", frame);
            // 按ESC退出
            if (org.opencv.highgui.HighGui.waitKey(1) == 27) break;
        }

        // 采集完成，释放资源
        System.out.println("采集完成！共采集：" + count + " 张");
        cap.release();
        org.opencv.highgui.HighGui.destroyAllWindows();
    }

    /**
     * 将人脸数据保存到Oracle数据库
     * @param userName 姓名
     * @param faceBytes 人脸图片字节
     */
    public static void saveToOracle(String userName, byte[] faceBytes) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "INSERT INTO face_database(id, username, facefeature) VALUES(face_database_seq.nextval, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userName);
                pstmt.setBytes(2, faceBytes);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}