package com.th.ipqcmbiz.entity.po;

import lombok.Data;
import java.util.Date;

/**
 * 工具柜库存实体
 * 记录工具柜中每个位置的当前工具状态
 */
@Data
public class ToolCabinetInventoryDO {

    /** 主键ID */
    private Long id;

    /** 柜号 */
    private String cabinetNo;

    /** 工具编号 */
    private String toolCode;

    /** 柜门位置 */
    private String position;

    /** 状态: in_cabinet在柜中 / empty空位 */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
