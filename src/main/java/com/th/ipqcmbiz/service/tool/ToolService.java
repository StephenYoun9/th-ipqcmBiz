package com.th.ipqcmbiz.service.tool;

import com.th.ipqcmbiz.entity.po.ToolInfoDO;

import java.util.List;

public interface ToolService {

    List<ToolInfoDO> getToolList(String keyword);

    ToolInfoDO getToolByCode(String toolCode);

    boolean addTool(ToolInfoDO tool);

    boolean updateTool(ToolInfoDO tool);

    boolean deleteTool(String toolCode);

    boolean updateToolStatus(String toolCode, String status);

    boolean updateToolImage(String toolCode, String imageUrl);
}