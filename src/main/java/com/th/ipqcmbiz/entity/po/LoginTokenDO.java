package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class LoginTokenDO {
    private Long id;
    private String userId;
    private String token;
    private Date lastActivity;
    private Date createTime;
    private Date expireTime;
}