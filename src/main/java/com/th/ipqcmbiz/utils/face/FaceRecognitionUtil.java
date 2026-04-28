package com.th.ipqcmbiz.utils.face;

import com.th.ipqcmbiz.entity.po.FaceFeatureDO;
import com.th.ipqcmbiz.mapper.face.FaceFeatureMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

@Component
@Slf4j
public class FaceRecognitionUtil {

    private CascadeClassifier faceDetector;
    private Net recognizerNet;

    @Resource
    private FaceFeatureMapper faceFeatureMapper;

    @PostConstruct
    public void init() {
        try {
            OpenCV.loadLocally();
            ClassPathResource haar = new ClassPathResource("haarcascade_frontalface_default.xml");
            faceDetector = new CascadeClassifier(haar.getFile().getAbsolutePath());
            log.info("Haar级联检测器加载成功");

            File tempModel = new File(System.getProperty("java.io.tmpdir"), "sface_recog_" + System.currentTimeMillis() + ".onnx");
            ClassPathResource sface = new ClassPathResource("face_models/face_recognition_sface_2021dec.onnx");
            try (java.io.InputStream is = sface.getInputStream()) {
                java.nio.file.Files.copy(is, tempModel.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                recognizerNet = Dnn.readNetFromONNX(tempModel.getAbsolutePath());
                log.info("SFace模型加载成功: {}", tempModel.getAbsolutePath());
            }
            tempModel.deleteOnExit();
        } catch (Exception e) {
            log.error("FaceRecognitionUtil初始化失败: {}", e.getMessage(), e);
        }
    }

    public FaceFeatureDO matchFace(byte[] faceImageBytes) {
        if (faceImageBytes == null || faceImageBytes.length == 0) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(faceImageBytes));
            if (image == null) {
                return null;
            }

            BufferedImage face = detectAndCropFace(image);
            if (face == null) {
                log.debug("未检测到人脸");
                return null;
            }

            float[] queryFeature = extractFeature(face);
            if (queryFeature == null) {
                return null;
            }

            List<FaceFeatureDO> allFaces = faceFeatureMapper.selectAll();
            if (allFaces == null || allFaces.isEmpty()) {
                log.debug("数据库中无人脸模板");
                return null;
            }

            float maxSim = 0;
            FaceFeatureDO best = null;

            for (FaceFeatureDO stored : allFaces) {
                float[] dbFeature = bytesToFloats(stored.getFeatureVector());
                float sim = cosineSimilarity(queryFeature, dbFeature);
                if (sim > maxSim) {
                    maxSim = sim;
                    best = stored;
                }
            }

            float threshold = best != null && best.getThreshold() != null ? best.getThreshold() : 0.4f;
            if (maxSim >= threshold) {
                log.info("人脸匹配成功: userId={}, similarity={}", best.getUserId(), maxSim);
                return best;
            } else {
                log.info("人脸匹配失败: 最高相似度={}, 阈值={}", maxSim, threshold);
                return null;
            }

        } catch (Exception e) {
            log.error("人脸匹配异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private BufferedImage detectAndCropFace(BufferedImage image) {
        if (image == null) return null;

        try {
            Mat mat = bufferedImageToMat(image);
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faces = new MatOfRect();
            if (faceDetector != null) {
                faceDetector.detectMultiScale(gray, faces, 1.15, 2, 0, new Size(80, 80), new Size(400, 400));
            }

            List<Rect> faceList = faces.toList();
            if (faceList.isEmpty()) {
                mat.release();
                gray.release();
                faces.release();
                return null;
            }

            Rect r = faceList.get(0);
            int margin = (int) (Math.max(r.width, r.height) * 0.2);
            int ix = Math.max(0, r.x - margin);
            int iy = Math.max(0, r.y - margin);
            int iw = Math.min(mat.cols() - ix, r.width + margin * 2);
            int ih = Math.min(mat.rows() - iy, r.height + margin * 2);

            Rect faceRect = new Rect(ix, iy, iw, ih);
            Mat faceMat = new Mat(mat, faceRect);
            Mat resized = new Mat();
            Imgproc.resize(faceMat, resized, new Size(112, 112));

            BufferedImage result = matToBufferedImage(resized);

            mat.release();
            gray.release();
            faces.release();
            faceMat.release();
            resized.release();

            return result;

        } catch (Exception e) {
            log.error("人脸检测失败: {}", e.getMessage());
            return null;
        }
    }

    private float[] extractFeature(BufferedImage image) {
        if (image == null || recognizerNet == null) return null;

        try {
            Mat mat = bufferedImageToMat(image);
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

            Mat blob = Dnn.blobFromImage(rgb, 1.0 / 128.0, new Size(112, 112), new Scalar(0, 0, 0), false, false);
            recognizerNet.setInput(blob);

            Mat featureMat = recognizerNet.forward();
            Core.normalize(featureMat, featureMat);

            float[] feature = new float[(int) featureMat.total()];
            featureMat.get(0, 0, feature);

            mat.release();
            rgb.release();
            blob.release();
            featureMat.release();

            return feature;

        } catch (Exception e) {
            log.error("特征提取失败: {}", e.getMessage());
            return null;
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10));
    }

    private float[] bytesToFloats(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
        return floats;
    }

    private Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) return null;
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int type = image.getType();
            if (type == 0) type = BufferedImage.TYPE_3BYTE_BGR;
            Mat mat = new Mat(height, width, CvType.CV_8UC3);
            byte[] pixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, pixels);
            return mat;
        } catch (Exception e) {
            log.error("BufferedImage转Mat失败: {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null) return null;
        try {
            int width = mat.cols();
            int height = mat.rows();
            byte[] pixels = new byte[width * height * 3];
            mat.get(0, 0, pixels);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            image.getRaster().setDataElements(0, 0, width, height, pixels);
            return image;
        } catch (Exception e) {
            log.error("Mat转BufferedImage失败: {}", e.getMessage());
            return null;
        }
    }

    public byte[] base64ToBytes(String base64Str) {
        if (base64Str == null) return null;
        String s = base64Str;
        if (s.contains(",")) s = s.split(",")[1];
        return org.apache.commons.codec.binary.Base64.decodeBase64(s);
    }
}
