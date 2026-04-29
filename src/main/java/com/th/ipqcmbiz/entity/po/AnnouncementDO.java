package com.th.ipqcmbiz.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 系统公告实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDO {

    /**
     * 公告ID
     */
    private Long id;

    /**
     * 公告标题，最大256字符
     */
    private String title;

    /**
     * 公告内容，最大2048字符
     */
    private String content;

    /**
     * 公告类型: info-通知, warning-警告, danger-危险
     */
    private String type;

    /**
     * 发布人ID
     */
    private String publisherId;

    /**
     * 发布人姓名
     */
    private String publisherName;

    /**
     * 状态: active-生效, inactive-失效
     */
    private String status;

    /**
     * 附件URL列表，JSON格式存储
     */
    private String attachments;

    /**
     * 失效时间（为空表示永久有效）
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}