package com.th.ipqcmbiz.service.tool;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.entity.vo.output.BorrowRecordRespVO;

import java.util.List;
import java.util.Map;

public interface ToolService {

    List<ToolInfoDO> getToolList(String keyword);

    PageInfo<ToolInfoDO> getToolListPaged(String keyword, int pageNum, int pageSize);

    PageInfo<ToolInfoDO> getAvailableToolsPaged(String keyword, String toolType, int pageNum, int pageSize);

    ToolInfoDO getToolByCode(String toolCode);

    boolean addTool(ToolInfoDO tool);

    boolean updateTool(ToolInfoDO tool);

    boolean deleteTool(String toolCode);

    boolean updateToolStatus(String toolCode, String status);

    boolean updateToolImage(String toolCode, String imageUrl);

    boolean borrowTool(String toolCode);

    boolean returnTool(String toolCode);

    boolean returnTool(String toolCode, String returnOperatorId);

    PageInfo<BorrowRecordDO> getMyBorrowedTools(int pageNum, int pageSize);

    PageInfo<BorrowRecordRespVO> getMyBorrowRecords(int pageNum, int pageSize);

    Map<String, Object> getBorrowStats();
}