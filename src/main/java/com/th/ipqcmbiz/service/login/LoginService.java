package com.th.ipqcmbiz.service.login;

import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;

import java.util.Map;

/**
 * @ClassName LoginService
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/4/2 10:40
 * @Version 1.0
 */
public interface LoginService {

    UserInfoRespVO loginByPwd(LoginVO loginVO);

    UserInfoRespVO loginByFinger(LoginVO loginVO);

    UserInfoRespVO loginByFace(Map<String, String> params);
}
