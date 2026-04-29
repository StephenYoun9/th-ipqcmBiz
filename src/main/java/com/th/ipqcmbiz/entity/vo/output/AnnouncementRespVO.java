package com.th.ipqcmbiz.entity.vo.output;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 公告响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "公告响应VO")
public class AnnouncementRespVO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "公告类型: info-通知, warning-警告, danger-危险")
    private String type;

    @Schema(description = "公告类型名称")
    private String typeName;

    @Schema(description = "发布人ID")
    private String publisherId;

    @Schema(description = "发布人姓名")
    private String publisherName;

    @Schema(description = "状态: active-生效, inactive-失效")
    private String status;

    @Schema(description = "附件URL列表")
    private List<String> attachments;

    @Schema(description = "失效时间")
    private Date expireTime;

    @Schema(description = "创建时间")
    private Date createTime;
}