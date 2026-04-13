package com.th.ipqcmbiz.utils.face;

import com.th.ipqcmbiz.entity.po.FaceInfoDO;
import com.th.ipqcmbiz.mapper.face.FaceMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName 人脸识别工具类
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/2 10:07
 * @Version 1.0
 */
@Component
@Slf4j
public class FaceRecognitionUtil {

    private CascadeClassifier faceDetector;
    private List<Mat> faceList = new ArrayList<>();
    private Map<Integer, FaceInfoDO> faceInfoMap = new HashMap<>();
    private boolean isFaceLoaded = false;

    // 数据库服务
    @Resource
    private FaceMapper faceMapper;

    // Redis 缓存（核心）
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存 KEY（固定）
    private static final String FACE_CACHE_KEY = "face:template:all";

    // 缓存过期时间：1小时（可改）
    private static final long FACE_CACHE_EXPIRE = 60 * 60L;

    @PostConstruct
    public void init() {
        OpenCV.loadLocally();
        try {
            ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
            faceDetector = new CascadeClassifier(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("加载人脸检测器失败", e);
        }
    }

    /**
    * @Description 从 Redis 或数据库加载人脸（带缓存优化）
    * @Param
    * @Return
    * @Author 杨兴明
    * @Date 2026/4/10 14:28
    */
    private void loadAllFaces() {
        synchronized (this) {
            if (isFaceLoaded) return;

            // 清空旧数据
            faceList.clear();
            faceInfoMap.clear();

            List<FaceInfoDO> faceInfoList;

            try {
                // ==========================================
                // 第一步：先从 Redis 取
                // ==========================================
                faceInfoList = (List<FaceInfoDO>) redisTemplate.opsForValue().get(FACE_CACHE_KEY);

                if (faceInfoList != null && !faceInfoList.isEmpty()) {
                    log.info("=== 从 Redis 缓存加载人脸模板 ===");
                } else {
                    // ==========================================
                    // 第二步：缓存没有 → 查数据库
                    // ==========================================
                    log.info("=== 从数据库加载人脸模板，并写入 Redis ===");
                    faceInfoList = faceMapper.selectAllFace();

                    // 写入 Redis
                    redisTemplate.opsForValue().set(FACE_CACHE_KEY, faceInfoList, FACE_CACHE_EXPIRE, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                // Redis 挂了 → 降级直接查数据库
                log.info("Redis 异常，直接查询数据库：" + e);
                faceInfoList = faceMapper.selectAllFace();
            }

            // 加载到内存
            for (int i = 0; i < faceInfoList.size(); i++) {
                FaceInfoDO faceInfo = faceInfoList.get(i);
                byte[] faceFeature = faceInfo.getFaceFeature();

                Mat faceMat = new Mat(100, 100, CvType.CV_8UC1);
                faceMat.put(0, 0, faceFeature);

                faceList.add(faceMat);
                faceInfoMap.put(i, faceInfo);
            }

            isFaceLoaded = true;
            log.info("人脸模板加载完成，数量：" + faceList.size());
        }
    }

    /**
    * @Description 人脸匹配
    * @Param faceImageBytes 人脸特征
    * @Return 人脸DO
    * @Author 杨兴明
    * @Date 2026/4/10 14:28
    */
    public FaceInfoDO matchFace(byte[] faceImageBytes) {
        // 匹配时才加载（带缓存）
        loadAllFaces();

        Mat srcMat = Imgcodecs.imdecode(new MatOfByte(faceImageBytes), Imgcodecs.IMREAD_GRAYSCALE);
        if (srcMat.empty()) return null;

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(srcMat, faces, 1.1, 5, 0, new Size(80, 80));
        Rect[] faceRects = faces.toArray();
        if (faceRects.length == 0) return null;

        Mat faceMat = new Mat(srcMat, faceRects[0]);
        Imgproc.resize(faceMat, faceMat, new Size(100, 100));

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

        return bestIndex >= 0 ? faceInfoMap.get(bestIndex) : null;
    }

    /**
    * @Description 新增/删除/更新人脸后清除人脸缓存
    * @Param
    * @Return
    * @Author 杨兴明
    * @Date 2026/4/10 14:29
    */
    public void clearFaceCache() {
        synchronized (this) {
            // 删除 Redis 缓存
            redisTemplate.delete(FACE_CACHE_KEY);
            // 重置内存标记
            isFaceLoaded = false;
            faceList.clear();
            faceInfoMap.clear();
            log.info("=== 人脸缓存已清空，下次匹配重新加载 ===");
        }
    }

    public byte[] base64ToBytes(String base64Str) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(base64Str);
    }
}