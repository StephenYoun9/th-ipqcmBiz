package com.th.ipqcmbiz.service.announcement;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.vo.input.AnnouncementReqVO;
import com.th.ipqcmbiz.entity.vo.output.AnnouncementRespVO;

import java.util.List;

/**
 * 系统公告服务接口
 */
public interface AnnouncementService {

    /**
     * 发布公告
     * @param reqVO 公告信息
     * @param publisherId 发布人ID
     * @param publisherName 发布人姓名
     * @return 是否成功
     */
    boolean publishAnnouncement(AnnouncementReqVO reqVO, String publisherId, String publisherName);

    /**
     * 获取生效中的公告列表
     * @return 公告列表
     */
    List<AnnouncementRespVO> getActiveAnnouncements();

    /**
     * 获取生效中的公告列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageInfo<AnnouncementRespVO> getActiveAnnouncementsPaged(int pageNum, int pageSize);

    /**
     * 获取所有公告列表（分页，生效优先）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageInfo<AnnouncementRespVO> getAllAnnouncementsPaged(int pageNum, int pageSize);

    /**
     * 删除公告
     * @param id 公告ID
     * @return 是否成功
     */
    boolean deleteAnnouncement(Long id);

    /**
     * 更新公告
     * @param id 公告ID
     * @param reqVO 公告信息
     * @return 是否成功
     */
    boolean updateAnnouncement(Long id, AnnouncementReqVO reqVO);
}