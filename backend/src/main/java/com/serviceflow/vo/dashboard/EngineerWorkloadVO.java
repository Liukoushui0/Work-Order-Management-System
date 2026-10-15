package com.serviceflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "工程师工作量")
public class EngineerWorkloadVO {

    private Long engineerId;
    private String engineerName;

    @Schema(description = "负责工单总数")
    private Long total;

    @Schema(description = "已完成工单数")
    private Long completed;

    @Schema(description = "未完成工单数")
    private Long open;

    @Schema(description = "平均处理时长（小时）")
    private BigDecimal avgHandleHours;
}
