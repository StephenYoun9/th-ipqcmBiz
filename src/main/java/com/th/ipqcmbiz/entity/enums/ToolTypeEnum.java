package com.th.ipqcmbiz.entity.enums;

/**
 * 工具管理分类枚举
 * 用于行政管理的工具分类
 */
public enum ToolTypeEnum {

    HAND("hand", "手动工具"),
    ELECTRIC("electric", "电动工具"),
    MEASURING("measuring", "测量工具");

    private final String code;
    private final String description;

    ToolTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ToolTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ToolTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
