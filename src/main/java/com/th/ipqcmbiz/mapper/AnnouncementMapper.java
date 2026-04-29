package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.AnnouncementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统公告数据访问接口
 */
@Mapper
public interface AnnouncementMapper {

    /**
     * 插入公告
     */
    int insert(AnnouncementDO announcement);

    /**
     * 更新公告
     */
    int update(AnnouncementDO announcement);

    /**
     * 删除公告
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID查询
     */
    AnnouncementDO selectById(@Param("id") Long id);

    /**
     * 查询生效中的公告列表（按创建时间倒序）
     */
    List<AnnouncementDO> selectActiveList();

    /**
     * 查询所有公告列表（按创建时间倒序）
     */
    List<AnnouncementDO> selectAllList();
}