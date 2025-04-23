package com.th.iqpcmbiz.mapper;

import com.th.iqpcmbiz.entity.po.UserInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserInfoMapper {
    /**
     * @Description 根据用户编号查询用户信息
     * @Param userId 用户编号
     * @Return 用户信息
     * @Author 杨兴明
     * @Date 2025/4/23 09:18
     */
    UserInfoDO queryUserById(@Param("userId") String userId);

    /**
     * @Description 根据用户信息查询用户信息
     * @Param userInfo 用户信息
     * @Return 用户信息
     * @Author 杨兴明
     * @Date 2025/4/23 09:19
     */
    UserInfoDO queryUserByUserInfo(UserInfoDO userInfo);

    /**
     * @Description 根据用户编号集合查询用户信息列表
     * @Param userIds 用户编号集合
     * @Return 用户信息列表
     * @Author 杨兴明
     * @Date 2025/4/23 11:25
     */
    List<UserInfoDO> queryUserListByUserIds(List<String> userIds);
}
