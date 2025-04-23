package com.th.iqpcmbiz.service.user.impl;

import com.th.iqpcmbiz.entity.po.UserInfoDO;
import com.th.iqpcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.iqpcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.iqpcmbiz.mapper.UserInfoMapper;
import com.th.iqpcmbiz.mapstruct.UserInfoProcessor;
import com.th.iqpcmbiz.service.user.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserInfoProcessor userInfoProcessor;

    @Override
    public UserInfoRespVO queryUserById(String userId) {
        UserInfoDO userInfoDO = userInfoMapper.queryUserById(userId);
        return userInfoProcessor.po2Vo(userInfoDO);
    }

    @Override
    public UserInfoRespVO queryUserBysUserInfo(UserInfoReqVO userInfo) {
        UserInfoDO infoDO = userInfoProcessor.vo2Po(userInfo);
        UserInfoDO userInfoDO = userInfoMapper.queryUserBysUserInfo(infoDO);
        return userInfoProcessor.po2Vo(userInfoDO);
    }
}
