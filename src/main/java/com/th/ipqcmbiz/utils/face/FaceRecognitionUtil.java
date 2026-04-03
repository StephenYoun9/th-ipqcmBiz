package com.th.ipqcmbiz.utils.face;

import com.th.ipqcmbiz.entity.po.FaceInfoDO;
import com.th.ipqcmbiz.service.face.FaceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName 人脸识别工具类
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/2 10:07
 * @Version 1.0
 */
@Component
public class FaceRecognitionUtil {

    // OpenCV人脸检测器
    private CascadeClassifier faceDetector;
    // 人脸模板列表
    private List<Mat> faceList = new ArrayList<>();
    // 人脸索引-用户信息DO映射
    private Map<Integer, FaceInfoDO> faceInfoMap = new HashMap<>();

    @Resource
    private FaceService faceService;

    // 初始化：加载OpenCV库 + 加载人脸检测器 + 加载数据库模板
    @PostConstruct
    public void init() {
        // 1. 加载OpenCV本地库
        OpenCV.loadLocally();

        // 2. 加载人脸检测器（Haar级联分类器）
        try {
            ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
            faceDetector = new CascadeClassifier(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("加载人脸检测器失败", e);
        }

        // 3. 从数据库加载人脸模板
        loadAllFaces();
    }

    /**
    * @Description 从数据库加载人脸模型
    * @Param
    * @Return
    * @Author 杨兴明
    * @Date 2026/4/2 13:42
    */
    private void loadAllFaces() {
        // 通过Mapper查询所有人脸模板信息
        List<FaceInfoDO> faceInfoList = faceService.selectAllFace();

        for (int i = 0; i < faceInfoList.size(); i++) {
            FaceInfoDO faceInfo = faceInfoList.get(i);
            byte[] faceFeature = faceInfo.getFaceFeature();

            // 转换为100x100灰度Mat
            Mat faceMat = new Mat(100, 100, CvType.CV_8UC1);
            faceMat.put(0, 0, faceFeature);

            // 存储模板和用户信息
            faceList.add(faceMat);
            faceInfoMap.put(i, faceInfo);
        }
        System.out.println("已加载人脸模板数量：" + faceList.size());
    }

    /**
     * 人脸匹配核心方法
     *
     * @param faceImageBytes 前端传入的人脸图片字节数组
     * @return 匹配到的用户信息DO（null=未匹配）
     */
    public FaceInfoDO matchFace(byte[] faceImageBytes) {
        // 1. 将字节数组转换为OpenCV Mat
        Mat srcMat = Imgcodecs.imdecode(new MatOfByte(faceImageBytes), Imgcodecs.IMREAD_GRAYSCALE);
        if (srcMat.empty()) {
            return null;
        }

        // 2. 检测人脸
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(srcMat, faces, 1.1, 5, 0, new Size(80, 80));
        Rect[] faceRects = faces.toArray();

        // 未检测到人脸
        if (faceRects.length == 0) {
            return null;
        }

        // 3. 裁剪并缩放人脸为100x100
        Mat faceMat = new Mat(srcMat, faceRects[0]);
        Imgproc.resize(faceMat, faceMat, new Size(100, 100));

        // 4. 模板匹配
        double maxSim = 0;
        int bestIndex = -1;
        for (int i = 0; i < faceList.size(); i++) {
            Mat res = new Mat();
            Imgproc.matchTemplate(faceMat, faceList.get(i), res, Imgproc.TM_CCOEFF_NORMED);
            double sim = Core.minMaxLoc(res).maxVal;

            if (sim > 0.6 && sim > maxSim) {
                maxSim = sim;
                bestIndex = i;
            }
        }

        // 5. 返回匹配结果（改为返回UserInfoDO）
        return bestIndex >= 0 ? faceInfoMap.get(bestIndex) : null;
    }

    /**
     * Base64字符串转字节数组
     */
    public byte[] base64ToBytes(String base64Str) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(base64Str);
    }
}