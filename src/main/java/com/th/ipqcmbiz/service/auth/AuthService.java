package com.th.ipqcmbiz.service.auth;

import com.th.ipqcmbiz.entity.vo.output.UserInfoRespVO;

public interface AuthService {

    String createToken(String userId, UserInfoRespVO userInfo);

    boolean validateToken(String token);

    boolean refreshToken(String token);

    void invalidateToken(String token);

    void invalidateUserTokens(String userId);

    Long getServerStartTime();

    boolean isTokenCreatedBeforeServerRestart(String token);

    boolean isTokenExpired(String token);
}