package com.th.ipqcmbiz.entity.po;

import lombok.Data;
import java.util.Date;

/**
 * 工具柜快照实体
 * 记录借/还操作前后的工具柜状态，用于对比
 */
@Data
public class ToolCabinetSnapshotDO {

    /** 主键ID */
    private Long id;

    /** 柜号 */
    private String cabinetNo;

    /** 工具编号 */
    private String toolCode;

    /** 柜门位置 */
    private String position;

    /** 快照类型: before借前 / after借后 */
    private String snapshotType;

    /** 关联的借还记录ID */
    private Long borrowRecordId;

    /** 创建时间 */
    private Date createTime;
}
