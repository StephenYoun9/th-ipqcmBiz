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
public class BorrowRecordDO {
    private Long id;
    private String toolCode;
    private String userId;
    private Date borrowTime;
    private Date returnTime;
    private String status;
    private String operatorType;
    private String cabinetNo;
    private String returnOperatorId;
}