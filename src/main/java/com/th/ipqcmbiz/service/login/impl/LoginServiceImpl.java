package com.th.ipqcmbiz.service.login.impl;

import com.th.ipqcmbiz.entity.po.FaceInfoDO;
import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.service.login.LoginService;
import com.th.ipqcmbiz.service.user.UserService;
import com.th.ipqcmbiz.utils.face.FaceRecognitionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * @ClassName LoginServiceImpl
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/2 10:41
 * @Version 1.0
 */
@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    // 密码工具
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private UserService userService;

    @Resource
    private FaceRecognitionUtil faceRecognitionUtil;


    @Override
    public UserInfoRespVO loginByPwd(LoginVO loginVO) {
        // 1. 查询用户信息
        UserInfoRespVO user = userService.queryUserById(loginVO.getUserId());
        // 2. 检查状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已禁用");
        }
        // 3. 验证密码
        if (!passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 返回用户信息
        return user;
    }

    @Override
    public UserInfoRespVO loginByFinger(LoginVO loginVO) {
        return null;
    }

    @Override
    public UserInfoRespVO loginByFace(Map<String, String> params) {
        try {
            String faceImageBase64 = params.get("faceImageBase64");
            // 1. Base64转字节数组
            byte[] faceImageBytes = faceRecognitionUtil.base64ToBytes(faceImageBase64);
            // 2. 调用人脸识别工具类匹配
            FaceInfoDO faceInfoDO = faceRecognitionUtil.matchFace(faceImageBytes);
            if (Objects.isNull(faceInfoDO)) {
                throw new BusinessException("未识别到系统匹配人脸，请重试！");
            }
            UserInfoRespVO userInfoRespVO = userService.queryUserById(faceInfoDO.getUserId());
            // 3. 检查状态
            if (userInfoRespVO.getStatus() == 0) {
                throw new BusinessException("账号已禁用");
            }
            return userInfoRespVO;
        } catch (BusinessException e) {
            log.error("人脸识别登录异常", e); // 打印完整堆栈
            throw e; // 继续抛出不影响原有逻辑
        }
    }
}
