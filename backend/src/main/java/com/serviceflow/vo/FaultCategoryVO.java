package com.serviceflow.vo;

import com.serviceflow.entity.FaultCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "故障分类信息")
public class FaultCategoryVO {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private String description;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    public static FaultCategoryVO from(FaultCategory category) {
        FaultCategoryVO vo = new FaultCategoryVO();
        vo.setId(category.getId());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setDescription(category.getDescription());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }
}
