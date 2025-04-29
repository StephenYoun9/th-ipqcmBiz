package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDO {
    /**
     * 用户编号
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;
}
