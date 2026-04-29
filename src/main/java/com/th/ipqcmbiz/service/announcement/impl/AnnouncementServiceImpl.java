package com.th.ipqcmbiz.service.announcement.impl;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.po.AnnouncementDO;
import com.th.ipqcmbiz.entity.vo.input.AnnouncementReqVO;
import com.th.ipqcmbiz.entity.vo.output.AnnouncementRespVO;
import com.th.ipqcmbiz.mapper.AnnouncementMapper;
import com.th.ipqcmbiz.service.announcement.AnnouncementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统公告服务实现类
 */
@Service
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Value("${tool.image.upload-path:/tmp/tool-images}")
    private String uploadPath;

    private static final String TYPE_INFO = "info";
    private static final String TYPE_WARNING = "warning";
    private static final String TYPE_DANGER = "danger";

    private static final String TYPE_NAME_INFO = "通知";
    private static final String TYPE_NAME_WARNING = "警告";
    private static final String TYPE_NAME_DANGER = "危险";

    @Override
    public boolean publishAnnouncement(AnnouncementReqVO reqVO, String publisherId, String publisherName) {
        AnnouncementDO announcement = AnnouncementDO.builder()
                .title(reqVO.getTitle())
                .content(reqVO.getContent())
                .type(reqVO.getType() != null ? reqVO.getType() : TYPE_INFO)
                .publisherId(publisherId)
                .publisherName(publisherName)
                .status("active")
                .attachments(attachmentsToJson(reqVO.getAttachments()))
                .expireTime(reqVO.getExpireTime())
                .build();

        return announcementMapper.insert(announcement) > 0;
    }

    @Override
    public List<AnnouncementRespVO> getActiveAnnouncements() {
        List<AnnouncementDO> list = announcementMapper.selectActiveList();
        return list.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    @Override
    public PageInfo<AnnouncementRespVO> getActiveAnnouncementsPaged(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AnnouncementDO> list = announcementMapper.selectActiveList();
        List<AnnouncementRespVO> respList = list.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
        return new PageInfo<>(respList);
    }

    @Override
    public PageInfo<AnnouncementRespVO> getAllAnnouncementsPaged(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AnnouncementDO> allList = announcementMapper.selectAllList();
        List<AnnouncementRespVO> sortedList = allList.stream()
                .map(this::convertToResp)
                .sorted(Comparator.comparing((AnnouncementRespVO a) -> {
                    boolean isExpired = a.getExpireTime() != null && a.getExpireTime().before(new Date());
                    return isExpired;
                }).thenComparing(AnnouncementRespVO::getCreateTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        PageInfo<AnnouncementRespVO> pageInfo = new PageInfo<>(sortedList);
        return pageInfo;
    }

    @Override
    public boolean deleteAnnouncement(Long id) {
        AnnouncementDO announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            deleteAttachmentFiles(announcement.getAttachments());
        }
        return announcementMapper.deleteById(id) > 0;
    }

    private void deleteAttachmentFiles(String attachmentsJson) {
        List<String> attachments = jsonToAttachments(attachmentsJson);
        for (String attachment : attachments) {
            try {
                String fileName = attachment.substring(attachment.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadPath, fileName);
                Files.deleteIfExists(filePath);
                log.debug("删除附件文件: {}", fileName);
            } catch (IOException e) {
                log.warn("删除附件文件失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean updateAnnouncement(Long id, AnnouncementReqVO reqVO) {
        AnnouncementDO announcement = AnnouncementDO.builder()
                .id(id)
                .title(reqVO.getTitle())
                .content(reqVO.getContent())
                .type(reqVO.getType() != null ? reqVO.getType() : TYPE_INFO)
                .attachments(attachmentsToJson(reqVO.getAttachments()))
                .expireTime(reqVO.getExpireTime())
                .build();

        return announcementMapper.update(announcement) > 0;
    }

    private AnnouncementRespVO convertToResp(AnnouncementDO announcement) {
        return AnnouncementRespVO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .typeName(getTypeName(announcement.getType()))
                .publisherId(announcement.getPublisherId())
                .publisherName(announcement.getPublisherName())
                .status(announcement.getStatus())
                .attachments(jsonToAttachments(announcement.getAttachments()))
                .expireTime(announcement.getExpireTime())
                .createTime(announcement.getCreateTime())
                .build();
    }

    private String getTypeName(String type) {
        if (type == null) return TYPE_NAME_INFO;
        switch (type) {
            case TYPE_WARNING:
                return TYPE_NAME_WARNING;
            case TYPE_DANGER:
                return TYPE_NAME_DANGER;
            default:
                return TYPE_NAME_INFO;
        }
    }

    private String attachmentsToJson(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "[]";
        }
        return JSON.toJSONString(attachments);
    }

    private List<String> jsonToAttachments(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            log.warn("解析附件JSON失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}