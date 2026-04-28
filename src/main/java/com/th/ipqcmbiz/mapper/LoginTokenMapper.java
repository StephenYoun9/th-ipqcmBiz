package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.LoginTokenDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginTokenMapper {

    LoginTokenDO selectByToken(@Param("token") String token);

    LoginTokenDO selectByUserId(@Param("userId") String userId);

    int insert(LoginTokenDO token);

    int updateLastActivity(@Param("token") String token, @Param("lastActivity") java.util.Date lastActivity);

    int deleteByToken(@Param("token") String token);

    int deleteByUserId(@Param("userId") String userId);
}