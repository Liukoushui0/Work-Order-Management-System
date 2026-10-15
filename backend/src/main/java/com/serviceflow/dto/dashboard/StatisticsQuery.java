package com.serviceflow.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "统计查询条件")
public class StatisticsQuery {

    @Schema(description = "统计起始日期（含），默认近 30 天", example = "2026-09-16")
    private LocalDate startDate;

    @Schema(description = "统计结束日期（含），默认今天", example = "2026-10-15")
    private LocalDate endDate;

    @Schema(description = "趋势粒度：DAY/WEEK/MONTH，默认 DAY")
    private String granularity = "DAY";
}
