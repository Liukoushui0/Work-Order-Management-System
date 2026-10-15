package com.serviceflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "仪表盘总览指标")
public class DashboardOverviewVO {

    @Schema(description = "工单总数")
    private Long total;

    @Schema(description = "各状态工单数")
    private List<DistributionItemVO> statusDistribution;

    @Schema(description = "未结束的工单数")
    private Long openCount;

    @Schema(description = "逾期工单数")
    private Long overdueCount;

    @Schema(description = "已完成工单数")
    private Long completedCount;

    @Schema(description = "平均处理时长（小时）")
    private BigDecimal avgHandleHours;

    @Schema(description = "平均满意度评分（无评价时为 null）")
    private BigDecimal avgSatisfaction;

    @Schema(description = "已评价数量")
    private Long evaluationCount;

    @Schema(description = "数据范围：ALL 全部，SELF 仅本人")
    private String scope;

    @Schema(description = "本次结果是否来自缓存")
    private Boolean cached;
}
