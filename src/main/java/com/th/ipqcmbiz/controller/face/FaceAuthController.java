package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.FaceEnrollVO;
import com.th.ipqcmbiz.service.face.FaceAuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<FaceEnrollVO> startEnroll(@RequestBody Map<String, Object> params) {
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
    public Result<FaceEnrollVO> capture(@RequestBody Map<String, Object> params) {
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
    public Result<Void> cancel(@RequestBody Map<String, Object> params) {
        String enrollId = (String) params.get("enrollId");

        // enrollId为null时静默忽略（可能用户已取消）
        if (enrollId != null) {
            faceAuthService.cancelEnroll(enrollId);
        }

        return Result.success("已取消");
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
    public Result<FaceEnrollVO> captureWithImage(@RequestBody Map<String, Object> params) {
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
     * 删除指定用户的人脸数据（用于重新采集）
     *
     * @param params 包含userId
     * @return Result操作结果
     *
     * 接口: POST /face/delete
     * 请求体: {"userId": "xxx"}
     */
    @PostMapping("/delete")
    public Result deleteFace(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }
        return faceAuthService.deleteFaceData(userId);
    }
}