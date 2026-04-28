package com.th.ipqcmbiz.controller.user;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.input.UserIdListReqVO;
import com.th.ipqcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping(value = "/user", name = "用户管理")
public class UserController extends BaseController {

    @Resource
    private UserService userService;

    @Operation(summary = "新增用户", description = "新增用户")
    @PostMapping(value = "/add")
    public Result<Void> addUser(@Valid @RequestBody UserInfoReqVO userInfo) {
        return success(userService.addUser(userInfo));
    }

    @Operation(summary = "用户列表查询", description = "根据用户id或名称查询用户列表")
    @PostMapping(value = "/query-user-list-by-id-or-name")
    public Result<List<UserInfoRespVO>> queryUserListByIdOrName(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(userService.queryUserListByIdOrName(keyword));
    }

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

    @Operation(summary = "分页用户查询", description = "根据用户编号列表查询用户信息")
    @PostMapping(value = "/query-user-list-by-user-id-list")
    public Result<PageInfo<UserInfoRespVO>> queryUserListByIds(@Valid @RequestBody UserIdListReqVO userIdListReqVO) {
        return success(userService.queryUserListByIds(userIdListReqVO));
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @PostMapping(value = "/update")
    public Result<Void> updateUser(@Valid @RequestBody UserInfoReqVO userInfo) {
        return success(userService.updateUser(userInfo));
    }

    @Operation(summary = "删除用户", description = "根据用户编号删除用户")
    @PostMapping(value = "/delete")
    public Result<Void> deleteUser(@RequestParam("userId") String userId) {
        return success(userService.deleteUser(userId));
    }

}