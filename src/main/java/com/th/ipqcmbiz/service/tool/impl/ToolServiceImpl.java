package com.th.ipqcmbiz.service.tool.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.context.UserContext;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.output.BorrowRecordRespVO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.mapper.ToolCabinetInventoryMapper;
import com.th.ipqcmbiz.mapper.ToolMapper;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import com.th.ipqcmbiz.mapper.borrow.BorrowRecordMapper;
import com.th.ipqcmbiz.service.tool.ToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ToolServiceImpl implements ToolService {

    @Autowired
    private ToolMapper toolMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private ToolCabinetInventoryMapper inventoryMapper;

    private String getRequiredUserId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }

    @Override
    public List<ToolInfoDO> getToolList(String keyword) {
        return toolMapper.selectList(keyword);
    }

    @Override
    public PageInfo<ToolInfoDO> getToolListPaged(String keyword, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ToolInfoDO> list = toolMapper.selectList(keyword);
        return new PageInfo<>(list);
    }

    @Override
    public PageInfo<ToolInfoDO> getAvailableToolsPaged(String keyword, String toolType, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<ToolInfoDO> list = toolMapper.selectAvailableList(keyword, toolType, offset, pageSize);
        Long total = toolMapper.countAvailableList(keyword, toolType);
        PageInfo<ToolInfoDO> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public ToolInfoDO getToolByCode(String toolCode) {
        return toolMapper.selectByCode(toolCode);
    }

    @Override
    public boolean addTool(ToolInfoDO tool) {
        if (tool.getStatus() == null || tool.getStatus().isEmpty()) {
            tool.setStatus("AVAILABLE");
        }
        return toolMapper.insert(tool) > 0;
    }

    @Override
    public boolean updateTool(ToolInfoDO tool) {
        return toolMapper.update(tool) > 0;
    }

    @Override
    public boolean deleteTool(String toolCode) {
        return toolMapper.deleteByCode(toolCode) > 0;
    }

    @Override
    public boolean updateToolStatus(String toolCode, String status) {
        return toolMapper.updateStatus(toolCode, status) > 0;
    }

    @Override
    public boolean updateToolImage(String toolCode, String imageUrl) {
        ToolInfoDO tool = new ToolInfoDO();
        tool.setToolCode(toolCode);
        tool.setImageUrl(imageUrl);
        return toolMapper.update(tool) > 0;
    }

    @Override
    @Transactional
    public boolean borrowTool(String toolCode) {
        String userId = getRequiredUserId();
        ToolInfoDO tool = toolMapper.selectByCode(toolCode);
        if (tool == null) {
            throw new BusinessException(404, "工具不存在");
        }
        if (!"AVAILABLE".equals(tool.getStatus())) {
            throw new BusinessException(400, "工具不可借");
        }

        BorrowRecordDO record = BorrowRecordDO.builder()
                .toolCode(toolCode)
                .userId(userId)
                .operatorType("borrow")
                .build();
        int inserted = borrowRecordMapper.insert(record);
        if (inserted <= 0) {
            throw new BusinessException(500, "借工具失败");
        }

        boolean updated = toolMapper.updateStatus(toolCode, "BORROWED") > 0;
        if (!updated) {
            throw new BusinessException(500, "借工具失败");
        }
        log.debug("用户 {} 借用了工具 {}", userId, toolCode);
        return true;
    }

    @Override
    @Transactional
    public boolean returnTool(String toolCode) {
        return returnTool(toolCode, getRequiredUserId());
    }

    @Override
    @Transactional
    public boolean returnTool(String toolCode, String returnOperatorId) {
        // 更新借出记录状态为已归还，记录归还操作人
        int updated = borrowRecordMapper.updateReturn(toolCode, returnOperatorId);
        if (updated <= 0) {
            throw new BusinessException(500, "还工具失败");
        }

        // 更新柜子库存状态为"在柜中"(0)
        int inventoryUpdated = inventoryMapper.updateStatusByToolCode(toolCode, "0");
        if (inventoryUpdated <= 0) {
            log.warn("归还工具 {} 时未找到对应的柜子库存记录", toolCode);
        }

        log.debug("操作人 {} 归还了工具 {}", returnOperatorId, toolCode);
        return true;
    }

    @Override
    public PageInfo<BorrowRecordDO> getMyBorrowedTools(int pageNum, int pageSize) {
        String userId = getRequiredUserId();
        int offset = (pageNum - 1) * pageSize;
        List<BorrowRecordDO> records = borrowRecordMapper.selectBorrowedByUserId(userId, offset, pageSize);
        Long total = borrowRecordMapper.countBorrowedByUserId(userId);
        PageInfo<BorrowRecordDO> pageInfo = new PageInfo<>(records);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public PageInfo<BorrowRecordRespVO> getMyBorrowRecords(int pageNum, int pageSize) {
        String userId = getRequiredUserId();
        int offset = (pageNum - 1) * pageSize;
        List<BorrowRecordRespVO> records = borrowRecordMapper.selectWithDetailByUserId(userId, offset, pageSize);
        Long total = borrowRecordMapper.countByUserId(userId);
        UserInfoDO currentUser = userInfoMapper.selectByUserId(userId);
        String userName = currentUser != null ? currentUser.getUserName() : userId;
        for (BorrowRecordRespVO record : records) {
            record.setUserName(userName);
        }
        PageInfo<BorrowRecordRespVO> pageInfo = new PageInfo<>(records);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public Map<String, Object> getBorrowStats() {
        String userId = getRequiredUserId();
        Long myBorrowedCount = borrowRecordMapper.countBorrowedByUserId(userId);
        Long totalBorrowedCount = borrowRecordMapper.countBorrowed();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("myBorrowedCount", myBorrowedCount);
        stats.put("totalBorrowedCount", totalBorrowedCount);
        return stats;
    }
}