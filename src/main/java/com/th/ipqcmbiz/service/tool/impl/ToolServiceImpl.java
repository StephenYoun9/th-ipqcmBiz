package com.th.ipqcmbiz.service.tool.impl;

import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.mapper.ToolMapper;
import com.th.ipqcmbiz.service.tool.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolServiceImpl implements ToolService {

    @Autowired
    private ToolMapper toolMapper;

    @Override
    public List<ToolInfoDO> getToolList(String keyword) {
        return toolMapper.selectList(keyword);
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
}