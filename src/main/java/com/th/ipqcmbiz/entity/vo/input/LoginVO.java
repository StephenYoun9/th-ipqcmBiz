package com.th.ipqcmbiz.entity.vo.input;

import lombok.Data;

/**
 * @ClassName LoginVO
 * @Description 登录入参实体类
 * @Author 杨兴明
 * @Date 2026/3/27 14:20
 * @Version 1.0
 */
@Data
public class LoginVO {

    /** 工号 */
    private String userId;

    /** 密码 */
    private String password;

}
