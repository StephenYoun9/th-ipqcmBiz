package com.th.ipqcmbiz.service.user.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.input.UserIdListReqVO;
import com.th.ipqcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import com.th.ipqcmbiz.mapstruct.UserInfoProcessor;
import com.th.ipqcmbiz.service.user.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    // 密码工具
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserInfoProcessor userInfoProcessor;

    @Override
    public Void addUser(UserInfoReqVO userInfoReqVO) {
        //校验参数（基础非空校验）
        if (userInfoReqVO.getUserId() == null || userInfoReqVO.getUserId().trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (userInfoReqVO.getPassword() == null || userInfoReqVO.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        // 1. 查询用户
        UserInfoDO user = userInfoMapper.queryUserById(userInfoReqVO.getUserId());
        if (user != null) {
            throw new BusinessException("用户已存在");
        }
        // 2. 转换用户信息
        UserInfoDO userInfoDO = userInfoProcessor.vo2Po(userInfoReqVO);
        //对明文密码进行BCrypt加密
        userInfoDO.setPassword(passwordEncoder.encode(userInfoDO.getPassword()));
        // 默认设置用户状态为启用（1）
        userInfoDO.setStatus(1);
        // 2. 添加到数据库
        int insertCount  = userInfoMapper.addUser(userInfoDO);
        if (insertCount <= 0) {
            throw new BusinessException("新增用户失败");
        }
        return null;
    }

    @Override
    public UserInfoRespVO queryUserById(String userId) {
        try {
            // 1. 查询用户
            UserInfoDO user = userInfoMapper.queryUserById(userId);
            if (user == null) {
                throw new BusinessException("工号不存在");
            }
            // 2. 返回用户信息
            return userInfoProcessor.po2Vo(user);

        }catch (BusinessException e) {
            log.error("登录异常", e); // 打印完整堆栈
            throw e; // 继续抛出不影响原有逻辑
        }
    }

    @Override
    public List<UserInfoRespVO> queryUserListByIdOrName(String keyword) {
        List<UserInfoDO> userInfoList = userInfoMapper.queryUserListByIdOrName(keyword);

        List<UserInfoRespVO> userInfoRespVOList = userInfoProcessor.poList2VoList(userInfoList);
        if(CollectionUtils.isEmpty(userInfoRespVOList)){
            return List.of();
        }
        return userInfoRespVOList;
    }

    @Override
    public UserInfoRespVO queryUserBysUserInfo(UserInfoReqVO userInfo) {
        UserInfoDO infoDO = userInfoProcessor.vo2Po(userInfo);
        UserInfoDO userInfoDO = userInfoMapper.queryUserByUserInfo(infoDO);
        return userInfoProcessor.po2Vo(userInfoDO);
    }

    @Override
    public PageInfo<UserInfoRespVO> queryUserListByIds(UserIdListReqVO reqVO) {
        int pageNum = reqVO.getPageNum() == null ? 1 : reqVO.getPageNum();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        List<UserInfoDO> userInfoList = userInfoMapper.queryUserListByUserIds(reqVO.getUserIds());
        List<UserInfoRespVO> userInfoRespVOList = userInfoProcessor.poList2VoList(userInfoList);
        return new PageInfo<>(userInfoRespVOList);
    }
}
