# OpenCV Face Recognition Models

本目录存放人脸识别所需的 ONNX 模型文件。

## 需要下载的文件

由于网络原因，请手动下载以下模型文件：

### 1. YuNet (人脸检测)
- 文件: `face_detection_yunet_2023mar.onnx`
- 大小: ~227 KB
- 下载地址: https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx
- 或: https://huggingface.co/opencv/opencv_zoo/resolve/main/face_detection_yunet/face_detection_yunet_2023mar.onnx

### 2. SFace (人脸识别)
- 文件: `face_recognition_sface_2021dec.onnx`
- 大小: ~2.3 MB
- 下载地址: https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx
- 或: https://huggingface.co/opencv/opencv_zoo/resolve/main/face_recognition_sface/face_recognition_sface_2021dec.onnx

## 验证文件

下载完成后，验证文件大小：
- YuNet: ~227 KB
- SFace: ~2.3 MB

## 模型说明

| 模型 | 用途 | 输入尺寸 | 输出 |
|------|------|----------|------|
| YuNet | 人脸检测+5点定位 | 160x160 | 检测框+landmarks |
| SFace | 人脸识别(128维特征) | 112x112 | 128维向量 |

## 参考资料

- OpenCV Zoo: https://github.com/opencv/opencv_zoo
- 模型论文: https://arxiv.org/abs/2208.12675 (SFace)