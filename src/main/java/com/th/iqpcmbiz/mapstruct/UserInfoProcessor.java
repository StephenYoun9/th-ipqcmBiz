package com.th.iqpcmbiz.mapstruct;

import com.th.iqpcmbiz.entity.po.UserInfoDO;
import com.th.iqpcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.iqpcmbiz.entity.vo.output.UserInfoRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @ClassName UserInfoProcessor
 * @Description 用户信息转换组件
 * @Author 杨兴明
 * @Date 2025/4/18 11:27
 * @Version 1.0
 */
@Mapper(componentModel = "spring") // 与 Spring 集成时需指定组件模型
public interface UserInfoProcessor {

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "userName", target = "userName")
    UserInfoRespVO po2Vo(UserInfoDO userInfoDO);

    UserInfoDO vo2Po(UserInfoReqVO reqVO);

    List<UserInfoRespVO> poList2VoList(List<UserInfoDO> userInfoDO);
}
