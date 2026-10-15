package com.serviceflow.dto.customer;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "客户列表查询条件")
public class CustomerQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "customerCode", "customerName", "level", "createTime");

    @Schema(description = "关键词：客户编号 / 名称 / 联系人 / 电话")
    private String keyword;

    @Schema(description = "客户等级：S/A/B/C")
    private String level;

    @Schema(description = "状态：1 正常，0 停用")
    private Integer status;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
