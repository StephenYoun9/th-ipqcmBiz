package com.th.ipqcmbiz.entity.vo.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 公告发布请求VO
 */
@Data
@Schema(description = "公告发布请求VO")
public class AnnouncementReqVO {

    @Schema(description = "公告ID（编辑时传入）")
    private Long id;

    @Schema(description = "公告标题，最大256字符")
    @NotBlank(message = "标题不能为空")
    @Size(max = 256, message = "标题不能超过256字符")
    private String title;

    @Schema(description = "公告内容，最大2048字符")
    @NotBlank(message = "内容不能为空")
    @Size(max = 2048, message = "内容不能超过2048字符")
    private String content;

    @Schema(description = "公告类型: info-通知, warning-警告, danger-危险")
    private String type = "info";

    @Schema(description = "失效时间（为空表示永久有效）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @Schema(description = "附件URL列表")
    private List<String> attachments;
}