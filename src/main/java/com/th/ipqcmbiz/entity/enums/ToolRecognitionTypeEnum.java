package com.th.ipqcmbiz.entity.enums;

/**
 * YOLO工具识别分类枚举
 * 对应YOLO模型训练时的类别标签
 */
public enum ToolRecognitionTypeEnum {

    WRENCH("wrench", "扳手", ToolTypeEnum.HAND),
    SCREWDRIVER("screwdriver", "螺丝刀", ToolTypeEnum.ELECTRIC),
    PLIERS("pliers", "钳子", ToolTypeEnum.HAND);

    private final String label;
    private final String description;
    private final ToolTypeEnum defaultToolType;

    ToolRecognitionTypeEnum(String label, String description, ToolTypeEnum defaultToolType) {
        this.label = label;
        this.description = description;
        this.defaultToolType = defaultToolType;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public ToolTypeEnum getDefaultToolType() {
        return defaultToolType;
    }

    public static ToolRecognitionTypeEnum fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (ToolRecognitionTypeEnum type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        return null;
    }

    public static String translateToChinese(String toolCode) {
        if (toolCode == null || toolCode.isEmpty()) {
            return toolCode;
        }
        ToolRecognitionTypeEnum type = fromLabel(toolCode);
        if (type != null) {
            return type.description;
        }
        return toolCode;
    }

    public static String translateToolCodeList(String toolCodes) {
        if (toolCodes == null || toolCodes.isEmpty()) {
            return toolCodes;
        }
        String[] codes = toolCodes.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(translateToChinese(codes[i]));
        }
        return sb.toString();
    }
}
