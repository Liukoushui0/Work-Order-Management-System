package com.serviceflow.controller;

import com.serviceflow.common.api.Result;
import com.serviceflow.dto.dashboard.StatisticsQuery;
import com.serviceflow.service.StatisticsService;
import com.serviceflow.vo.dashboard.DashboardOverviewVO;
import com.serviceflow.vo.dashboard.DistributionItemVO;
import com.serviceflow.vo.dashboard.EngineerWorkloadVO;
import com.serviceflow.vo.dashboard.SatisfactionVO;
import com.serviceflow.vo.dashboard.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 售后运营统计接口。
 *
 * 数据域：管理员与客服看到全局统计，工程师只看到与自己相关的统计，客户无权访问。
 * 统计结果来自数据库真实聚合，并带有缓存（写操作后失效）。
 */
@Tag(name = "运营统计")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final StatisticsService statisticsService;

    public DashboardController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "总览指标", description = "工单总数、各状态数、逾期数、平均处理时长与平均满意度")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(statisticsService.overview());
    }

    @Operation(summary = "工单趋势", description = "按日/周/月统计新建与完成工单数，默认近 30 天")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@Valid StatisticsQuery query) {
        return Result.success(statisticsService.trend(query));
    }

    @Operation(summary = "故障类型分布")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/fault-distribution")
    public Result<List<DistributionItemVO>> faultDistribution() {
        return Result.success(statisticsService.faultDistribution());
    }

    @Operation(summary = "优先级分布")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/priority-distribution")
    public Result<List<DistributionItemVO>> priorityDistribution() {
        return Result.success(statisticsService.priorityDistribution());
    }

    @Operation(summary = "工程师工作量", description = "负责工单数、已完成数与平均处理时长")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/engineer-workload")
    public Result<List<EngineerWorkloadVO>> engineerWorkload() {
        return Result.success(statisticsService.engineerWorkload());
    }

    @Operation(summary = "客户满意度", description = "评价总数、平均评分与评分分布")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/satisfaction")
    public Result<SatisfactionVO> satisfaction() {
        return Result.success(statisticsService.satisfaction());
    }
}
