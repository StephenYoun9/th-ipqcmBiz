package com.th.ipqcmbiz.entity.vo;

import com.th.ipqcmbiz.entity.enums.InventoryStatusEnum;
import com.th.ipqcmbiz.entity.enums.ToolRecognitionTypeEnum;
import lombok.Data;

/**
 * 工具柜库存VO
 * 用于API返回
 */
@Data
public class ToolCabinetInventoryVO {

    private Long id;

    private String cabinetNo;

    /**
     * 工具编号 - 英文label
     */
    private String toolCode;

    /**
     * 工具编号中文名
     */
    private String toolCodeName;

    private String position;

    /**
     * 状态 - 0在柜中/1空位
     */
    private String status;

    /**
     * 状态中文描述
     */
    private String statusName;

    public String getToolCodeName() {
        if (this.toolCode == null) {
            return null;
        }
        ToolRecognitionTypeEnum typeEnum = ToolRecognitionTypeEnum.fromLabel(this.toolCode);
        if (typeEnum != null) {
            return typeEnum.getDescription();
        }
        return this.toolCode;
    }

    public String getStatusName() {
        return InventoryStatusEnum.getDescription(this.status);
    }
}