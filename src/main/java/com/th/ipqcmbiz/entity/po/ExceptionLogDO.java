package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class ExceptionLogDO {
    private Long id;
    private String exceptionType;
    private String toolCode;
    private String userId;
    private String description;
    private String status;
    private Date createTime;
    private Date handleTime;
    private String handler;
}