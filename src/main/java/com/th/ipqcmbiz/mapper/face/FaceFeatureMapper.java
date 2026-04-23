package com.th.ipqcmbiz.mapper.face;

import com.th.ipqcmbiz.entity.po.FaceFeatureDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FaceFeatureMapper {

    int insert(FaceFeatureDO faceFeature);

    int deleteByUserId(@Param("userId") String userId);

    List<FaceFeatureDO> selectByUserId(@Param("userId") String userId);

    List<FaceFeatureDO> selectAll();

    int countByUserId(@Param("userId") String userId);
}