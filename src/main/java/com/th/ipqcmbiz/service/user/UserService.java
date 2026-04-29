package com.th.ipqcmbiz.service.user;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.vo.input.UserIdListReqVO;
import com.th.ipqcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;

import java.util.List;

public interface UserService {

    Void addUser(UserInfoReqVO userInfoReqVO);

    UserInfoRespVO queryUserById(String userId);

    List<UserInfoRespVO> queryUserListByIdOrName(String keyword);

    PageInfo<UserInfoRespVO> queryUserListPaged(String keyword, int pageNum, int pageSize);

    UserInfoRespVO queryUserBysUserInfo(UserInfoReqVO userInfo);

    PageInfo<UserInfoRespVO> queryUserListByIds(UserIdListReqVO userIdListReqVO);

    Void updateUser(UserInfoReqVO userInfoReqVO);

    Void deleteUser(String userId);
}