package com.th.ipqcmbiz.service.user;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.vo.input.UserIdListReqVO;
import com.th.ipqcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;

import java.util.List;

public interface UserService {

    /**
    * @Description 添加用户
    * @Param userInfoReqVO 用户信息
    * @Return void
    * @Author 杨兴明
    * @Date 2026/3/30 16:35
    */
    Void addUser(UserInfoReqVO userInfoReqVO);

    /**
    * @Description 通过用户编号查询用户信息
    * @Param userId 用户编号
    * @Return 登录结果
    * @Author 杨兴明
    * @Date 2026/3/27 14:45
    */
    UserInfoRespVO queryUserById(String userId);

    /**
    * @Description 根据用户id或名称查询用户列表
    * @Param keyword 关键字，用户id或名称
    * @Return 用户列表
    * @Author 杨兴明
    * @Date 2026/3/31 17:00
    */
    List<UserInfoRespVO> queryUserListByIdOrName(String keyword);

    /**
     * @Description 根据用户信息查询用户信息
     * @Param userInfo 用户信息
     * @Return 用户信息
     * @Author 杨兴明
     * @Date 2025/4/18 10:12
     */
    UserInfoRespVO queryUserBysUserInfo(UserInfoReqVO userInfo);

    /**
     * @Description 根据用户编号列表分页查询用户信息
     * @Param userIdListReqVO 用户编号列表
     * @Return 分页用户列表
     * @Author 杨兴明
     * @Date 2025/4/23 10:34
     */
    PageInfo<UserInfoRespVO> queryUserListByIds(UserIdListReqVO userIdListReqVO);
}
