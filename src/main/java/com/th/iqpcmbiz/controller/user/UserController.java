package com.th.iqpcmbiz.controller.user;

import com.th.iqpcmbiz.controller.common.BaseController;
import com.th.iqpcmbiz.entity.common.Result;
import com.th.iqpcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.iqpcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.iqpcmbiz.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/th/user", name = "用户管理")
public class UserController extends BaseController {

    @Resource
    private UserService userService;

    @Operation(summary = "用户查询", description = "根据用户编号查询用户信息")
    @PostMapping(value = "/query-user-by-id")
    public Result<UserInfoRespVO> queryUserById(@RequestParam("userId") String userId) {
        return success(userService.queryUserById(userId));
    }

    @Operation(summary = "用户查询", description = "根据用户信息查询用户信息")
    @PostMapping(value = "/query-user-by-user-info")
    public Result<UserInfoRespVO> queryUserById(@Valid @RequestBody UserInfoReqVO userInfo) {
        return success(userService.queryUserBysUserInfo(userInfo));
    }
}
