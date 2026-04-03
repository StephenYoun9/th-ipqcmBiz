package com.th.ipqcmbiz.mapstruct;

import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.input.UserInfoReqVO;
import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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


    @Named("booleanToString")
    default String booleanToString(Boolean bool) {
        if (bool == null) {
            return ""; // 自定义null值处理
        }
        return bool ? "Y" : "N"; // 可替换为 "true"/"false" 或其他规则
    }

    /**
     * String转Boolean（示例："Y"→true，"N"→false，其他/null→null）
     */
    @Named("stringToBoolean")
    default Boolean stringToBoolean(String str) {
        if (str == null) {
            return null; // 自定义null值处理
        }
        return "Y".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str);
    }

    @Mapping(source = "faceRegistered", target = "faceRegistered", qualifiedByName = "stringToBoolean")
    UserInfoRespVO po2Vo(UserInfoDO userInfoDO);

    @Mapping(source = "faceRegistered", target = "faceRegistered", qualifiedByName = "booleanToString")
    UserInfoDO vo2Po(UserInfoReqVO reqVO);

    List<UserInfoRespVO> poList2VoList(List<UserInfoDO> userInfoDO);
}
