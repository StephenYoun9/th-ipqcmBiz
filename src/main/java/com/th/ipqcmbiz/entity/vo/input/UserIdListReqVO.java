package com.th.ipqcmbiz.entity.vo.input;

import com.th.ipqcmbiz.entity.dto.BasePageDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * @ClassName UserIdListReqVO
 * @Description 用户编号列表入参
 * @Author 杨兴明
 * @Date 2025/4/23 09:50
 * @Version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserIdListReqVO extends BasePageDTO {

    private List<@NotBlank @Size(min = 6, message = "用户编号至少6位") String> userIds;
}
