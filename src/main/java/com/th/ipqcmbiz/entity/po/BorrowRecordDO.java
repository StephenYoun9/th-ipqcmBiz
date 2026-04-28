package com.th.ipqcmbiz.entity.po;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class BorrowRecordDO {
    private Long id;
    private String toolCode;
    private String userId;
    private Date borrowTime;
    private Date returnTime;
    private String status;
    private String operatorType;
}