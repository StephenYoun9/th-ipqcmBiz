package com.th.ipqcmbiz.entity.vo.output;

import com.alibaba.fastjson2.annotation.JSONField;
import com.th.ipqcmbiz.config.serializer.ToolCodeListSerializer;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class BorrowRecordRespVO {
    private Long id;

    @JSONField(serializeUsing = ToolCodeListSerializer.class)
    private String toolCode;

    private String cabinetNo;
    private String userId;
    private String userName;
    private Date borrowTime;
    private Date returnTime;
    private String status;
    private String statusName;
    private String operatorType;
}