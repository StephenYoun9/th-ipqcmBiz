package com.th.ipqcmbiz.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfoDO {
    private Long id;
    private String toolCode;
    private String toolName;
    private String toolType;
    private String cabinetNo;
    private String status;
    private String specification;
    private String imageUrl;
    private Date createTime;
    private Date updateTime;
}