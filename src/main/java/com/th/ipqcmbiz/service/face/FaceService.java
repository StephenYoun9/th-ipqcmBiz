package com.th.ipqcmbiz.service.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.FaceInfoDO;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @ClassName FaceService
 * @Description 类功能描述
 * @Author 杨兴明
 * @Date 2026/3/25 14:15
 * @Version 1.0
 */

public interface FaceService {

    /**
     * @Description 查询所有人脸模型
     * @Param
     * @Return 人脸
     * @Author 杨兴明
     * @Date 2026/4/2 13:45
     */
    List<FaceInfoDO> selectAllFace();

    BufferedImage getFrame(String userId,String userName);

    String getDirection();

    int getProgress();

    void releaseCamera();

    Result reinitCamera();

    byte[] getLatestJpegFrame();

}
