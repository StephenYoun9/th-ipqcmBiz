package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import com.th.ipqcmbiz.entity.vo.ToolCabinetInventoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工具柜库存对象转换Mapper (MapStruct)
 */
@Mapper(componentModel = "spring")
public interface ToolCabinetVOConverter {

    ToolCabinetVOConverter INSTANCE = Mappers.getMapper(ToolCabinetVOConverter.class);

    ToolCabinetInventoryVO toVO(ToolCabinetInventoryDO DO);

    List<ToolCabinetInventoryVO> toVOList(List<ToolCabinetInventoryDO> DOList);
}