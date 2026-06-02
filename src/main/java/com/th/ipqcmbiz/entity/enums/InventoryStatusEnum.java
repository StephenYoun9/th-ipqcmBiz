package com.th.ipqcmbiz.entity.enums;

/**
 * 工具柜库存状态枚举
 */
public enum InventoryStatusEnum {

    IN_CABINET("0", "在柜中"),
    EMPTY("1", "空位");

    private final String code;
    private final String description;

    InventoryStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static InventoryStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InventoryStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescription(String code) {
        InventoryStatusEnum status = fromCode(code);
        return status != null ? status.description : code;
    }
}