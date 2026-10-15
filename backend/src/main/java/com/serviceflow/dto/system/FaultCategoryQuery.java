package com.serviceflow.dto.system;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "故障分类查询条件")
public class FaultCategoryQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "sort", "categoryCode", "createTime");

    @Schema(description = "关键词：编码 / 名称")
    private String keyword;

    @Schema(description = "状态：1 启用，0 禁用")
    private Integer status;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
