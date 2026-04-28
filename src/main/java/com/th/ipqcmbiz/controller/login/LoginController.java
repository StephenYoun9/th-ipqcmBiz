package com.th.ipqcmbiz.controller.login;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.input.LoginVO;
import com.th.ipqcmbiz.entity.vo.output.LoginRespVO;
import com.th.ipqcmbiz.service.login.LoginService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController extends BaseController {

    @Resource
    private LoginService loginService;

    @PostMapping("/pwd")
    public Result<LoginRespVO> loginByPwd(@RequestBody LoginVO loginVO) {
        return success(loginService.loginByPwd(loginVO));
    }

    @PostMapping("/finger")
    public Result<LoginRespVO> loginByFinger(@RequestBody LoginVO loginVO) {
        return success(loginService.loginByFinger(loginVO));
    }

    @PostMapping("/face")
    public Result<LoginRespVO> loginByFace(@RequestBody Map<String, String> params) {
        return success(loginService.loginByFace(params));
    }
}
