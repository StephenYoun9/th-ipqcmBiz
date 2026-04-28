package com.th.ipqcmbiz.entity.vo.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRespVO {
    private String token;
    private UserInfoRespVO userInfo;
}