package com.th.ipqcmbiz.service.face;

// ========== 统一返回结果格式 ==========
import com.th.ipqcmbiz.entity.common.Result;
// ========== 拍照录入返回VO ==========
import com.th.ipqcmbiz.entity.vo.FaceEnrollVO;
// ========== 人脸识别返回VO ==========
import com.th.ipqcmbiz.entity.vo.FaceRecognizeVO;
// ========== 视频录入返回VO ==========
import com.th.ipqcmbiz.entity.vo.FaceVideoEnrollVO;

// ========== Java AWT图片格式（用于特征提取） ==========
import java.awt.image.BufferedImage;

/**
 * 人脸认证服务接口
 *
 * 该接口定义了人脸识别系统的核心功能：
 *
 * 【功能模块】
 * 1. 人脸录入（拍照模式）- 用户手动一张一张拍照录入
 * 2. 人脸录入（视频模式）- 用户录制视频，自动从视频中提取人脸
 * 3. 人脸识别登录 - 从视频流中识别人脸并登录
 *
 * 【使用模型】
 * - YuNet: 人脸检测模型，检测图片中的人脸位置和边界框
 * - SFace: 人脸识别模型，从检测到的人脸中提取128维特征向量
 *
 * 【质量评估】
 * - 使用亮度、对比度等因素综合评估人脸质量
 * - 质量低于阈值(默认0.5)的人脸会被拒绝录入
 */
public interface FaceAuthService {

    // ==================== 拍照录入模式（手动单张） ====================

    /**
     * 开始人脸录入（拍照模式）
     *
     * 流程说明：
     * 1. 前端点击"开始采集"按钮，传入userId
     * 2. 后端创建EnrollSession会话，生成唯一的enrollId
     * 3. 前端后续每次"拍照采集"都会带上这个enrollId
     *
     * @param userId 用户ID，用于标识待录入的用户
     * @param faceCount 需要录入的人脸数量，默认8张
     * @return 录入会话ID（enrollId），用于后续操作的凭证
     */
    String startEnroll(String userId, int faceCount);

    /**
     * 捕获当前视频帧进行人脸录入（拍照模式）
     *
     * 工作流程：
     * 1. 从FaceVideoService获取当前摄像头帧
     * 2. 使用YuNet检测人脸位置
     * 3. 裁剪并调整为112x112大小
     * 4. 评估人脸质量
     * 5. 如果质量达标，使用SFace提取128维特征向量
     * 6. 将特征向量存入EnrollSession会话
     *
     * 注意：此方法直接从视频流抓帧，适用于后端有视频流的场景
     *
     * @param enrollId 录入会话ID，由startEnroll生成
     * @param userId 用户ID，用于校验
     * @return FaceEnrollVO包含当前录入进度、捕获状态等信息
     */
    FaceEnrollVO captureForEnroll(String enrollId, String userId);

    /**
     * 完成人脸录入（拍照模式）
     *
     * 结束流程：
     * 1. 从会话map中移除EnrollSession
     * 2. 遍历会话中存储的所有人脸特征
     * 3. 创建FaceFeatureDO对象存入FACE_FEATURE表
     * 4. 更新USER_INFO表的faceEnrolled标志为"1"
     *
     * 存储内容：
     * - 每张人脸的128维特征向量（用于后续识别比对）
     * - 人脸图片（裁剪后的112x112图片）
     * - 质量分数、阈值、创建时间等元数据
     *
     * @param enrollId 录入会话ID
     * @return Result包含操作结果，成功时message说明共录入了几张人脸
     */
    Result completeEnroll(String enrollId);

    /**
     * 取消人脸录入（拍照模式）
     *
     * 用于用户中途放弃录入：
     * 1. 从会话map中移除EnrollSession
     * 2. 清除该会话存储的所有临时数据
     *
     * 注意：此方法不会删除已存入数据库的数据
     *
     * @param enrollId 录入会话ID
     */
    void cancelEnroll(String enrollId);


    // ==================== 人脸识别登录 ====================

    /**
     * 人脸识别登录
     *
     * 识别流程：
     * 1. 从视频流获取当前帧
     * 2. 使用YuNet检测人脸
     * 3. 使用SFace提取128维特征向量
     * 4. 从数据库加载该用户的所有人脸特征（或所有用户）
     * 5. 计算余弦相似度，找出最相似的人脸
     * 6. 如果相似度超过阈值(默认0.4)，则认为匹配成功
     *
     * 阈值说明：
     * - 阈值越低，越容易匹配成功（误识率高）
     * - 阈值越高，越严格（误识率低，但可能拒识）
     * - 默认0.4是经过测试的平衡值
     *
     * @param userId 用户ID（可选）
     *              - 如果指定，则只与该用户的人脸比对
     *              - 如果为空，则与所有已录入用户比对（用于不知道用户身份的登录场景）
     * @return FaceRecognizeVO包含匹配结果、相似度、用户信息等
     */
    FaceRecognizeVO recognize(String userId);

    /**
     * 获取录入状态
     *
     * 查询指定用户的录入情况：
     * 1. 查询USER_INFO表获取faceEnrolled标志
     * 2. 查询FACE_FEATURE表统计该用户已录入的人脸数量
     *
     * @param userId 用户ID
     * @return FaceEnrollVO包含录入状态信息
     */
    FaceEnrollVO getEnrollStatus(String userId);


    // ==================== 核心算法（检测/识别/质量评估） ====================

    /**
     * 从人脸图片提取128维特征向量
     *
     * 使用SFace模型进行特征提取：
     * 1. 输入：112x112 RGB人脸图片
     * 2. 处理：归一化处理，像素值缩放到[-1,1]
     * 3. 模型推理：SFace ONNX模型前向传播
     * 4. 输出：128维浮点特征向量
     * 5. 后处理：L2归一化，确保特征向量的模为1
     *
     * 特征向量用途：
     * - 录入时：保存特征向量到数据库
     * - 识别时：查询特征向量与数据库中存储的特征比对
     *
     * @param image 裁剪后的人脸图片（112x112）
     * @return 128维特征向量数组，如果提取失败返回null
     */
    float[] extractFeature(BufferedImage image);

    /**
     * 检测人脸并裁剪
     *
     * 使用YuNet模型进行人脸检测：
     * 1. 输入：原始摄像头图片
     * 2. 预处理：BGR转RGB，归一化，缩放到160x160
     * 3. 模型推理：YuNet ONNX模型前向传播
     * 4. 输出：人脸检测结果[N, 15]，N为检测到的人脸数
     *    - 每行：[x, y, w, h, conf, ...]
     *    - x,y: 人脸框左上角坐标（相对值）
     *    - w,h: 人脸框宽高（相对值）
     *    - conf: 置信度
     * 5. 后处理：选择置信度最高的检测结果
     * 6. 裁剪：根据检测框坐标从原图裁剪人脸区域
     * 7. 调整大小：缩放到112x112
     *
     * 注意事项：
     * - 只返回置信度>0.5的检测结果
     * - 裁剪时会添加20%的边界margin
     * - 如果未检测到人脸返回null
     *
     * @param image 输入图片（原始摄像头帧）
     * @return 裁剪后的人脸图片（112x112），检测失败返回null
     */
    BufferedImage detectAndCropFace(BufferedImage image);

    /**
     * 评估人脸质量
     *
     * 质量评估算法（综合得分0-1）：
     * 1. 亮度得分（权重40%）：
     *    - 计算灰度图的平均灰度值
     *    - 理想平均灰度128（既不过暗也不过亮）
     *    - 得分 = 1 - |mean - 128| / 128
     *
     * 2. 对比度得分（权重60%）：
     *    - 计算灰度图的标准差
     *    - 标准差越大对比度越高
     *    - 得分 = stddev / 64（最高1.0）
     *
     * 3. 综合得分 = 亮度得分 * 0.4 + 对比度得分 * 0.6
     *
     * 质量阈值说明：
     * - 低于0.5的人脸通常光线太差或对比度不足
     * - 建议录入时只接受质量>=0.5的人脸
     *
     * @param face 裁剪后的人脸图片（112x112）
     * @return 质量分数，范围0-1，数值越高表示质量越好
     */
    float assessQuality(BufferedImage face);


    // ==================== 图片模式（前端上传图片） ====================

    /**
     * 使用上传的图片进行人脸录入（拍照模式-图片方式）
     *
     * 与captureForEnroll的区别：
     * - captureForEnroll：从后端视频流抓帧
     * - 本方法：接收前端Base64编码的图片数据
     *
     * 适用场景：
     * - 前端自己从video元素截图
     * - 适用于前端独立控制视频流获取的情况
     *
     * 处理流程：
     * 1. 解码Base64图片数据
     * 2. detectAndCropFace检测人脸
     * 3. assessQuality评估质量
     * 4. extractFeature提取特征
     * 5. 存入EnrollSession
     *
     * @param enrollId 录入会话ID
     * @param userId 用户ID
     * @param imageData Base64编码的图片数据（可能带data:image/jpeg;base64,前缀）
     * @return FaceEnrollVO包含录入结果和进度
     */
    FaceEnrollVO captureForEnrollWithImage(String enrollId, String userId, String imageData);

    /**
     * 使用上传的图片进行人脸识别
     *
     * 与recognize的区别：
     * - recognize：从后端视频流抓帧进行识别
     * - 本方法：接收前端Base64编码的图片数据进行识别
     *
     * 适用场景：
     * - 前端自己截图后上传识别
     * - 适用于测试或离线图片识别
     *
     * 处理流程：
     * 1. 解码Base64图片数据
     * 2. detectAndCropFace检测人脸
     * 3. extractFeature提取特征
     * 4. 从数据库加载人脸特征
     * 5. 计算余弦相似度找最优匹配
     *
     * @param userId 用户ID（可选），指定时只比对该用户的特征
     * @param imageData Base64编码的图片数据
     * @return FaceRecognizeVO包含识别结果和相似度
     */
    FaceRecognizeVO recognizeWithImage(String userId, String imageData);


    // ==================== 视频录入模式（自动连续帧） ====================

    /**
     * 开始视频人脸录入
     *
     * 视频录入 vs 拍照录入：
     * - 拍照录入：用户手动一张一张拍照，需要用户配合
     * - 视频录入：用户录制10秒视频，系统自动从视频中提取多张人脸
     *
     * 视频录入优势：
     * 1. 用户体验更好：只需录制10秒，不用反复调整姿势
     * 2. 人脸多样性：视频中可能有轻微角度变化
     * 3. 自动筛选：系统自动选取质量最高的帧
     *
     * 前端流程：
     * 1. 前端调用本接口获取enrollId
     * 2. 开始10秒倒计时录制
     * 3. 每200ms截取一帧，调用addVideoFrame上传
     * 4. 录制结束后调用completeVideoEnroll完成
     *
     * @param userId 用户ID
     * @return 视频录入会话ID（enrollId）
     */
    String startVideoEnroll(String userId);

    /**
     * 添加视频帧进行人脸录入
     *
     * 核心处理流程：
     * 1. 接收前端发送的Base64图片数据
     * 2. 解码为BufferedImage
     * 3. detectAndCropFace检测人脸
     *    - 如果未检测到人脸，返回detected=false
     * 4. assessQuality评估质量
     *    - 如果质量<阈值(0.5)，返回detected=false
     * 5. extractFeature提取128维特征向量
     * 6. 存入VideoEnrollSession会话
     *
     * 帧过滤策略：
     * - 帧间隔：前端每200ms发送一帧
     * - 有效帧：检测到人脸且质量>=0.5
     * - 无效帧：未检测到人脸或质量不达标，不存入session
     *
     * 关于frameIndex：
     * - 前端递增发送，从0开始
     * - 主要用于调试和日志追踪
     * - 后端不依赖此序号保证顺序（依赖TCP传输顺序）
     *
     * @param enrollId 视频录入会话ID
     * @param userId 用户ID（用于校验）
     * @param frameIndex 帧序号（前端的递增序号）
     * @param imageData Base64编码的帧图片
     * @return FaceVideoEnrollVO包含处理结果和当前检测到的人脸数
     */
    FaceVideoEnrollVO addVideoFrame(String enrollId, String userId, int frameIndex, String imageData);

    /**
     * 完成视频人脸录入
     *
     * 完成流程：
     * 1. 从会话map中移除VideoEnrollSession
     * 2. 检查是否检测到足够的人脸（至少8张）
     * 3. 按质量降序排序所有检测到的人脸
     * 4. 选取质量最高的8张
     * 5. 为每张人脸创建FaceFeatureDO存入数据库
     * 6. 更新USER_INFO表的faceEnrolled="1"
     *
     * 质量筛选算法：
     * - 将所有帧按quality降序排列
     * - 取前8张（如检测到10张，取质量最好的8张）
     * - 确保最终存入的都是高质量人脸
     *
     * 存储内容：
     * - 128维特征向量（用于识别比对）
     * - 质量分数（记录该人脸的质量）
     * - faceIndex: 1-8（质量最好的为1，以此类推）
     *
     * 注意：与拍照模式不同，视频模式不存储人脸图片（faceImage=null）
     * 原因：视频录入的人脸是快速连续的帧，存储价值不大
     *
     * @param enrollId 视频录入会话ID
     * @return Result成功时message说明共检测到几张、存入几张
     */
    Result completeVideoEnroll(String enrollId);

    // ==================== 视频录制模式（后端FFmpeg录制） ====================

    /**
     * 录制视频人脸录入
     *
     * 后端使用FFmpeg从RTSP流录制5秒视频，然后处理视频中的人脸
     *
     * 流程：
     * 1. 使用FFmpeg录制RTSP流5秒视频
     * 2. 从视频中提取帧，检测人脸
     * 3. 质量排序，选取最好的8张
     * 4. 存入FACE_FEATURE表
     * 5. 更新用户faceEnrolled状态
     *
     * @param userId 用户ID
     * @return 录制结果，包含检测到的人脸数量、视频路径等
     */
    Result recordVideoEnroll(String userId);
}