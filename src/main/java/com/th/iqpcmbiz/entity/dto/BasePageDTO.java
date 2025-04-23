package com.th.iqpcmbiz.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName BasePageDTO
 * @Description 基础分页类
 * @Author 杨兴明
 * @Date 2025/4/23 11:11
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasePageDTO {
    private Integer pageNum = 1;    // 当前页码
    private Integer pageSize = 10;
}
