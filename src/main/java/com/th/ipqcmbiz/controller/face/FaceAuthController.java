package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.FaceEnrollVO;
import com.th.ipqcmbiz.entity.vo.FaceRecognizeVO;
import com.th.ipqcmbiz.entity.vo.FaceVideoEnrollVO;
import com.th.ipqcmbiz.service.face.FaceAuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

// ========== 请求参数Map ==========
import java.util.Map;


@RestController
@RequestMapping("/face")
@Slf4j
public class FaceAuthController {

    // ========== 人脸认证服务（自动注入） ==========
    @Resource
    private FaceAuthService faceAuthService;

    // ==================== 状态查询 ====================

    /**
     * 获取用户人脸录入状态
     *
     * 查询指定用户是否已完成人脸录入
     *
     * @param userId 用户ID
     * @return FaceEnrollVO包含录入状态、已录入数量等
     *
     * 接口: GET /face/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public Result getStatus(@PathVariable String userId) {
        // 调用服务获取录入状态
        FaceEnrollVO status = faceAuthService.getEnrollStatus(userId);
        return Result.success(status);
    }


    // ==================== 拍照录入模式（手动单张） ====================

    /**
     * 开始人脸录入（拍照模式）
     *
     * 创建一个新的录入会话，返回enrollId
     * 前端后续拍照时需要携带此enrollId
     *
     * @param params 包含userId和faceCount
     * @return FaceEnrollVO包含enrollId、userId、初始进度等
     *
     * 接口: POST /face/enroll/start
     * 请求体: {"userId": "xxx", "faceCount": 8}
     */
    @PostMapping("/enroll/start")
    public Result startEnroll(@RequestBody Map<String, Object> params) {
        // 从请求体获取userId
        String userId = (String) params.get("userId");
        // 从请求体获取faceCount（可选，默认8）
        Integer faceCount = (Integer) params.get("faceCount");

        // 参数校验
        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }

        // 使用传入的脸数量或默认值8
        int count = faceCount != null ? faceCount : 8;

        // 调用服务开始录入会话
        String enrollId = faceAuthService.startEnroll(userId, count);

        // 构建返回对象
        FaceEnrollVO vo = new FaceEnrollVO();
        vo.setEnrollId(enrollId);
        vo.setUserId(userId);
        vo.setRequired(count);
        vo.setCaptured(0);
        vo.setProgress(0);

        return Result.success(vo);
    }

    /**
     * 捕获当前帧进行录入（拍照模式-后端抓帧）
     *
     * 从后端视频流抓取当前帧进行人脸检测和录入
     * 与captureWithImage的区别是：此方法由后端控制视频帧获取
     *
     * @param params 包含enrollId和userId
     * @return FaceEnrollVO包含捕获结果和进度
     *
     * 接口: POST /face/enroll/capture
     * 请求体: {"enrollId": "xxx", "userId": "xxx"}
     */
    @PostMapping("/enroll/capture")
    public Result capture(@RequestBody Map<String, Object> params) {
        // 获取enrollId和userId
        String enrollId = (String) params.get("enrollId");
        String userId = (String) params.get("userId");

        // 参数校验
        if (enrollId == null || userId == null) {
            return Result.error(400, "参数不完整");
        }

        // 调用服务捕获当前帧
        FaceEnrollVO vo = faceAuthService.captureForEnroll(enrollId, userId);
        return Result.success(vo);
    }

    /**
     * 完成人脸录入（拍照模式）
     *
     * 结束录入会话，保存所有人脸到数据库
     *
     * @param params 包含enrollId
     * @return Result操作结果
     *
     * 接口: POST /face/enroll/complete
     * 请求体: {"enrollId": "xxx"}
     */
    @PostMapping("/enroll/complete")
    public Result complete(@RequestBody Map<String, Object> params) {
        String enrollId = (String) params.get("enrollId");

        // 校验enrollId
        if (enrollId == null) {
            return Result.error(400, "enrollId不能为空");
        }

        // 调用服务完成录入
        return faceAuthService.completeEnroll(enrollId);
    }

    /**
     * 取消人脸录入（拍照模式）
     *
     * 取消当前录入会话，清理临时数据
     * 注意：不会删除已存入数据库的数据
     *
     * @param params 包含enrollId
     * @return Result操作结果
     *
     * 接口: POST /face/enroll/cancel
     * 请求体: {"enrollId": "xxx"}
     */
    @PostMapping("/enroll/cancel")
    public Result cancel(@RequestBody Map<String, Object> params) {
        String enrollId = (String) params.get("enrollId");

        // enrollId为null时静默忽略（可能用户已取消）
        if (enrollId != null) {
            faceAuthService.cancelEnroll(enrollId);
        }

        return Result.success("已取消");
    }


    // ==================== 人脸识别登录 ====================

    /**
     * 人脸识别登录
     *
     * 从视频流获取当前帧，识别人脸并匹配数据库
     *
     * @param params 包含userId（可选）
     * @return FaceRecognizeVO包含识别结果、相似度、用户信息等
     *
     * 接口: POST /face/recognize
     * 请求体: {"userId": "xxx"} 或 {"userId": ""}
     */
    @PostMapping("/recognize")
    public Result recognize(@RequestBody Map<String, Object> params) {
        // userId为可选参数
        String userId = (String) params.get("userId");

        // 调用服务进行识别
        FaceRecognizeVO vo = faceAuthService.recognize(userId);
        return Result.success(vo);
    }


    // ==================== 图片模式（前端上传） ====================

    /**
     * 使用上传的图片进行人脸录入（拍照模式-前端截图）
     *
     * 与capture的区别：
     * - capture: 后端从视频流抓帧
     * - 本方法: 接收前端Base64编码的图片数据
     *
     * 适用场景：
     * - 前端自己控制视频流获取和截图
     * - 通过canvas截取video元素当前帧
     *
     * @param params 包含enrollId、userId、image
     * @return FaceEnrollVO包含录入结果
     *
     * 接口: POST /face/enroll/capture/image
     * 请求体: {"enrollId": "xxx", "userId": "xxx", "image": "base64..."}
     */
    @PostMapping("/enroll/capture/image")
    public Result captureWithImage(@RequestBody Map<String, Object> params) {
        // 获取三个必要参数
        String enrollId = (String) params.get("enrollId");
        String userId = (String) params.get("userId");
        String imageData = (String) params.get("image");

        // 参数校验
        if (enrollId == null || userId == null || imageData == null) {
            return Result.error(400, "参数不完整");
        }

        // 调用服务处理上传的图片
        FaceEnrollVO vo = faceAuthService.captureForEnrollWithImage(enrollId, userId, imageData);
        return Result.success(vo);
    }

    /**
     * 使用上传的图片进行人脸识别
     *
     * 与recognize的区别：
     * - recognize: 后端从视频流抓帧
     * - 本方法: 接收前端Base64编码的图片数据
     *
     * @param params 包含userId（可选）、image
     * @return FaceRecognizeVO包含识别结果
     *
     * 接口: POST /face/recognize/image
     * 请求体: {"userId": "xxx", "image": "base64..."}
     */
    @PostMapping("/recognize/image")
    public Result recognizeWithImage(@RequestBody Map<String, Object> params) {
        // userId为可选参数
        String userId = (String) params.get("userId");
        // image为必选参数
        String imageData = (String) params.get("image");

        // 图片数据不能为空
        if (imageData == null) {
            return Result.error(400, "图片不能为空");
        }

        // 调用服务处理识别
        FaceRecognizeVO vo = faceAuthService.recognizeWithImage(userId, imageData);
        return Result.success(vo);
    }


    // ==================== 视频录入模式（自动连续帧） ====================

    /**
     * 开始视频人脸录入
     *
     * 创建视频录入会话，返回enrollId
     * 前端开始10秒录制后，每200ms调用addVideoFrame上传一帧
     *
     * @param params 包含userId
     * @return enrollId会话标识
     *
     * 接口: POST /face/enroll/video/start
     * 请求体: {"userId": "xxx"}
     *
     * 前端调用流程：
     * 1. 调用本接口获取enrollId
     * 2. 开始10秒倒计时
     * 3. 每200ms: canvas截图 → Base64编码 → addVideoFrame
     * 4. 10秒后自动停止
     * 5. 调用completeVideoEnroll完成录入
     */
    @PostMapping("/enroll/video/start")
    public Result startVideoEnroll(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");

        // userId必填
        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }

        // 调用服务创建视频录入会话
        String enrollId = faceAuthService.startVideoEnroll(userId);
        return Result.success(enrollId);
    }

    /**
     * 添加视频帧进行人脸录入
     *
     * 每收到一帧后：
     * 1. 检测人脸（YuNet）
     * 2. 评估质量
     * 3. 如果质量达标则提取特征并存储
     *
     * @param params 包含enrollId、userId、frameIndex、image
     * @return FaceVideoEnrollVO包含处理结果
     *
     * 接口: POST /face/enroll/video/frame
     * 请求体: {"enrollId": "xxx", "userId": "xxx", "frameIndex": 0, "image": "base64..."}
     *
     * 返回示例：
     * {
     *   "enrollId": "xxx",
     *   "userId": "xxx",
     *   "frameIndex": 5,
     *   "totalFrames": 5,
     *   "detectedFaces": 3,
     *   "quality": 0.82,
     *   "detected": true,
     *   "message": "已检测到人脸，质量: 82%"
     * }
     */
    @PostMapping("/enroll/video/frame")
    public Result addVideoFrame(@RequestBody Map<String, Object> params) {
        // 获取所有必要参数
        String enrollId = (String) params.get("enrollId");
        String userId = (String) params.get("userId");
        Integer frameIndex = (Integer) params.get("frameIndex");
        String imageData = (String) params.get("image");

        // 参数完整性校验
        if (enrollId == null || userId == null || frameIndex == null || imageData == null) {
            return Result.error(400, "参数不完整");
        }

        // 调用服务处理视频帧
        FaceVideoEnrollVO vo = faceAuthService.addVideoFrame(enrollId, userId, frameIndex, imageData);
        return Result.success(vo);
    }

    /**
     * 完成视频人脸录入
     *
     * 结束视频录入会话：
     * 1. 验证检测到的人脸数量（至少8张）
     * 2. 按质量排序，选取最好的8张
     * 3. 存入FACE_FEATURE表
     * 4. 更新用户状态
     *
     * @param params 包含enrollId
     * @return Result操作结果
     *
     * 接口: POST /face/enroll/video/complete
     * 请求体: {"enrollId": "xxx"}
     *
     * 成功返回示例：
     * {
     *   "code": 200,
     *   "message": "人脸录入完成，共检测到 25 张，选取质量最高的 8 张存入数据库"
     * }
     *
     * 失败返回示例：
     * {
     *   "code": 400,
     *   "message": "检测到的人脸不足8张，请重试。当前检测到: 5 张"
     * }
     */
    @PostMapping("/enroll/video/complete")
    public Result completeVideoEnroll(@RequestBody Map<String, Object> params) {
        String enrollId = (String) params.get("enrollId");

        // enrollId必填
        if (enrollId == null) {
            return Result.error(400, "enrollId不能为空");
        }

        // 调用服务完成视频录入
        return faceAuthService.completeVideoEnroll(enrollId);
    }

    // ==================== 视频录制模式（后端录制） ====================

    /**
     * 录制视频人脸录入
     *
     * 后端使用FFmpeg从RTSP流录制5秒视频，然后处理视频中的人脸
     *
     * @param params 包含userId
     * @return Result操作结果
     *
     * 接口: POST /face/enroll/video/record
     * 请求体: {"userId": "xxx"}
     */
    @PostMapping("/enroll/video/record")
    public Result recordVideo(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");

        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }

        log.info("收到视频录制请求: userId={}", userId);
        return faceAuthService.recordVideoEnroll(userId);
    }
}