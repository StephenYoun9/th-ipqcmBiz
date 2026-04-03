package com.th.ipqcmbiz.mapper.face;

import com.th.ipqcmbiz.entity.po.FaceInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @ClassName FaceMapper
 * @Description 人脸相关mapper
 * @Author 杨兴明
 * @Date 2026/3/27 09:53
 * @Version 1.0
 */
@Mapper
public interface FaceMapper {

    /**
    * @Description 查询所有人脸模型
    * @Param
    * @Return 人脸
    * @Author 杨兴明
    * @Date 2026/4/2 13:45
    */
    List<FaceInfoDO> selectAllFace();

    /**
     * @Description 录入人脸信息
     * @Param faceInfoDO 人脸信息
     * @Return int
     * @Author 杨兴明
     * @Date 2026/4/2 10:32
     */
    int insertFaceWithName(FaceInfoDO faceInfoDO);
}
