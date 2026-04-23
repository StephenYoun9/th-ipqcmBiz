package com.th.ipqcmbiz.entity.vo;

// ========== Lombok自动生成getter/setter/toString等方法 ==========
import lombok.Data;

/**
 * 视频人脸录入返回VO类
 *
 * 用于视频人脸录入流程中，每帧处理结果的返回对象
 *
 * 设计背景：
 * - 视频录入模式：前端录制10秒视频，每200ms发送一帧到后端
 * - 后端每收到一帧处理后，返回当前帧的检测结果
 * - 前端根据返回结果更新进度显示
 *
 * 与FaceEnrollVO的区别：
 * - FaceEnrollVO用于拍照录入模式（单帧手动拍照）
 * - FaceVideoEnrollVO用于视频录入模式（连续帧自动处理）
 */
@Data
public class FaceVideoEnrollVO {

    // ========== 录入会话ID ==========
    // 由后端startVideoEnroll生成
    // 每次视频录入会话唯一
    // 用于后续帧的上传和完成确认
    private String enrollId;

    // ========== 用户ID ==========
    // 待录入人脸的用户编号
    // 与员工信息表中的userId对应
    private String userId;

    // ========== 当前帧序号 ==========
    // 从0开始递增
    // 用于标识帧顺序（虽然实际处理顺序由后端保证）
    private int frameIndex;

    // ========== 已录制的总帧数 ==========
    // 本次会话中前端已发送的帧总数
    // 包括检测到人脸和未检测到人脸的帧
    private int totalFrames;

    // ========== 检测到人脸的帧数 ==========
    // 累计检测到有效人脸（置信度>0.5且质量>=阈值）的帧数
    // 用于判断是否足够完成录入
    private int detectedFaces;

    // ========== 当前帧的质量分数 ==========
    // 范围0-1
    // 由assessQuality方法评估
    // 包含亮度、对比度等因素
    private float quality;

    // ========== 当前帧是否检测到人脸 ==========
    // true: 检测到人脸且质量达标
    // false: 未检测到人脸或质量不达标
    private boolean detected;

    // ========== 处理结果描述信息 ==========
    // 如"已检测到人脸，质量: 85%"
    // 或"未检测到人脸"、"人脸质量不足"等
    private String message;
}