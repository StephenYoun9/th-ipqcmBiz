package com.th.ipqcmbiz.entity.vo.output;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class BorrowRecordRespVO {
    private Long id;
    private String toolCode;
    private String toolName;
    private String cabinetNo;
    private String userId;
    private String userName;
    private Date borrowTime;
    private Date returnTime;
    private String status;
    private String statusName;
    private String operatorType;
}