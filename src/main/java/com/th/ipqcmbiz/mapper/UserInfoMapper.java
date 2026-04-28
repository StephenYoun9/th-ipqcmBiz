package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.UserInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserInfoMapper {

    int addUser(UserInfoDO userInfo);

    List<UserInfoDO> queryUserListByIdOrName(@Param("keyword") String keyword);

    UserInfoDO queryUserById(@Param("userId") String userId);

    UserInfoDO queryUserByUserInfo(UserInfoDO userInfo);

    List<UserInfoDO> queryUserListByUserIds(List<String> userIds);

    UserInfoDO selectByUserId(@Param("userId") String userId);

    int updateFaceEnrolled(UserInfoDO user);

    int updateFingerEnrolled(UserInfoDO user);

    int updateUser(UserInfoDO user);

    int deleteByUserId(@Param("userId") String userId);
}