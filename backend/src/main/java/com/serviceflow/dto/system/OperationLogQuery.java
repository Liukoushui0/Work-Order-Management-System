package com.serviceflow.dto.system;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Schema(description = "操作日志查询条件")
public class OperationLogQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createTime", "durationMs");

    @Schema(description = "模块")
    private String module;

    @Schema(description = "操作人账号")
    private String username;

    @Schema(description = "是否成功：1 成功，0 失败")
    private Integer success;

    @Schema(description = "开始日期", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-10-31")
    private LocalDate endDate;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
