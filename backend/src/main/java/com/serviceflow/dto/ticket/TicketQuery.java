package com.serviceflow.dto.ticket;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Schema(description = "工单列表查询条件")
public class TicketQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "ticketNo", "priority", "status", "deadlineTime", "createTime");

    @Schema(description = "关键词：工单编号 / 标题 / 报修人")
    private String keyword;

    @Schema(description = "状态：PENDING_ASSIGN/PENDING_ACCEPT/PROCESSING/PENDING_CONFIRM/COMPLETED/CANCELLED")
    private String status;

    @Schema(description = "优先级：LOW/MEDIUM/HIGH/URGENT")
    private String priority;

    private Long customerId;

    private Long assigneeId;

    private Long faultCategoryId;

    @Schema(description = "创建时间起（含）", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "创建时间止（含）", example = "2026-10-31")
    private LocalDate endDate;

    @Schema(description = "是否只看逾期工单")
    private Boolean overdue;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
