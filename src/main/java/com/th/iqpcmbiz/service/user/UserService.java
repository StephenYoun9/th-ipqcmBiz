package com.th.iqpcmbiz.service.user;

import com.th.iqpcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.iqpcmbiz.entity.vo.output.UserInfoRespVO;

public interface UserService {

    /**
     * @Description 根据用户编号查询用户信息
     * @Param userId 用户编号
     * @Return 用户信息
     * @Author 杨兴明
     * @Date 2025/4/18 10:02
     */
    UserInfoRespVO queryUserById(String userId);

    /**
     * @Description 根据用户信息查询用户信息
     * @Param userInfo 用户信息
     * @Return 用户信息
     * @Author 杨兴明
     * @Date 2025/4/18 10:12
     */
    UserInfoRespVO queryUserBysUserInfo(UserInfoReqVO userInfo);
}
