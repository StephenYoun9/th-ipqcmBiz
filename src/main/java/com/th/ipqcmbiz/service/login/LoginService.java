package com.th.ipqcmbiz.service.login;

import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.LoginRespVO;

import java.util.Map;

public interface LoginService {

    LoginRespVO loginByPwd(LoginVO loginVO);

    LoginRespVO loginByFinger(LoginVO loginVO);

    LoginRespVO loginByFace(Map<String, String> params);
}
