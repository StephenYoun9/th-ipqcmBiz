package com.th.ipqcmbiz.controller.login;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.service.login.LoginService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @ClassName LoginController
 * @Description 登录controller
 * @Author 杨兴明
 * @Date 2026/3/27 14:18
 * @Version 1.0
 */
@RestController
@RequestMapping("/login")
public class LoginController extends BaseController {

    @Resource
    private LoginService loginService;

    /** 账号密码登录 */
    @PostMapping("/pwd")
    public Result<UserInfoRespVO> loginByPwd(@RequestBody LoginVO loginVO) {
        return success(loginService.loginByPwd(loginVO));
    }

    /** 指纹认证登录 */
    @PostMapping("/finger")
    public Result<UserInfoRespVO> loginByFinger(@RequestBody LoginVO loginVO) {
        return success(loginService.loginByFinger(loginVO));
    }

    /** 人脸认证登录 */
    @PostMapping("/face")
    public Result<UserInfoRespVO> loginByFace(@RequestBody Map<String, String> params) {
        return success(loginService.loginByFace(params));
    }
}
