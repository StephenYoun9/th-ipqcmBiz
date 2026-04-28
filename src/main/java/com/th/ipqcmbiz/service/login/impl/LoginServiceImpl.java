package com.th.ipqcmbiz.service.login.impl;

import com.th.ipqcmbiz.entity.po.FaceFeatureDO;
import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.LoginRespVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.service.auth.AuthService;
import com.th.ipqcmbiz.service.login.LoginService;
import com.th.ipqcmbiz.service.user.UserService;
import com.th.ipqcmbiz.utils.face.FaceRecognitionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private UserService userService;

    @Resource
    private FaceRecognitionUtil faceRecognitionUtil;

    @Resource
    private AuthService authService;

    @Override
    public LoginRespVO loginByPwd(LoginVO loginVO) {
        UserInfoRespVO user = userService.queryUserById(loginVO.getUserId());
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已禁用");
        }
        if (!passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        String token = authService.createToken(user.getUserId(), user);
        return LoginRespVO.builder()
                .token(token)
                .userInfo(user)
                .build();
    }

    @Override
    public LoginRespVO loginByFinger(LoginVO loginVO) {
        return null;
    }

    @Override
    public LoginRespVO loginByFace(Map<String, String> params) {
        try {
            String faceImageBase64 = params.get("faceImageBase64");
            byte[] faceImageBytes = faceRecognitionUtil.base64ToBytes(faceImageBase64);
            FaceFeatureDO matchResult = faceRecognitionUtil.matchFace(faceImageBytes);
            if (matchResult == null) {
                throw new BusinessException("未识别到系统匹配人脸，请重试！");
            }
            UserInfoRespVO user = userService.queryUserById(matchResult.getUserId());
            if (user.getStatus() == 0) {
                throw new BusinessException("账号已禁用");
            }
            String token = authService.createToken(user.getUserId(), user);
            return LoginRespVO.builder()
                    .token(token)
                    .userInfo(user)
                    .build();
        } catch (BusinessException e) {
            log.error("人脸识别登录异常", e);
            throw e;
        }
    }
}
