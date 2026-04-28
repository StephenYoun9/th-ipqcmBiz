package com.th.ipqcmbiz.entity.vo.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "工具信息请求VO")
public class ToolInfoReqVO {

    @Schema(description = "工具编号")
    @NotBlank(message = "工具编号不能为空")
    private String toolCode;

    @Schema(description = "工具名称")
    @NotBlank(message = "工具名称不能为空")
    private String toolName;

    @Schema(description = "工具类型")
    private String toolType;

    @Schema(description = "存放位置/柜号")
    private String cabinetNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "规格参数")
    private String specification;

    @Schema(description = "图片URL")
    private String imageUrl;
}