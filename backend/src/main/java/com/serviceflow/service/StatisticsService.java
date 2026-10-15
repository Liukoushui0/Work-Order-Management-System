package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.enums.TicketPriority;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.dashboard.StatisticsQuery;
import com.serviceflow.entity.FaultCategory;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.FaultCategoryMapper;
import com.serviceflow.mapper.StatisticsMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.view.CountByOwner;
import com.serviceflow.mapper.view.DateCount;
import com.serviceflow.mapper.view.EngineerWorkloadRow;
import com.serviceflow.mapper.view.LabelCount;
import com.serviceflow.mapper.view.SatisfactionRow;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.vo.dashboard.DashboardOverviewVO;
import com.serviceflow.vo.dashboard.DistributionItemVO;
import com.serviceflow.vo.dashboard.EngineerWorkloadVO;
import com.serviceflow.vo.dashboard.SatisfactionVO;
import com.serviceflow.vo.dashboard.TrendPointVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 售后运营统计。
 *
 * 数据域：管理员与客服看到全局；工程师只看到与自己相关的统计；客户无权限。
 * 缓存：先查 Redis，未命中再查库并回写；写操作由 StatisticsCacheService 在事务提交后失效。
 */
@Service
public class StatisticsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final StatisticsMapper statisticsMapper;
    private final SysUserMapper sysUserMapper;
    private final FaultCategoryMapper faultCategoryMapper;
    private final StatisticsCacheService cacheService;
    private final DataScopeService dataScopeService;

    public StatisticsService(StatisticsMapper statisticsMapper,
                             SysUserMapper sysUserMapper,
                             FaultCategoryMapper faultCategoryMapper,
                             StatisticsCacheService cacheService,
                             DataScopeService dataScopeService) {
        this.statisticsMapper = statisticsMapper;
        this.sysUserMapper = sysUserMapper;
        this.faultCategoryMapper = faultCategoryMapper;
        this.cacheService = cacheService;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 仪表盘总览指标。
     */
    @Transactional(readOnly = true)
    public DashboardOverviewVO overview() {
        Long assigneeId = scopedAssigneeId();
        String cacheKey = cacheService.overviewKey(scopeKey(assigneeId));
        Optional<DashboardOverviewVO> cached = cacheService.get(cacheKey, DashboardOverviewVO.class);
        if (cached.isPresent()) {
            DashboardOverviewVO cachedVo = cached.get();
            cachedVo.setCached(true);
            return cachedVo;
        }

        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setScope(assigneeId == null ? "ALL" : "SELF");
        vo.setCached(false);
        vo.setTotal(statisticsMapper.countTotal(assigneeId));
        List<LabelCount> statusCounts = statisticsMapper.countByStatus(assigneeId);
        vo.setStatusDistribution(toDistribution(statusCounts, StatisticsService::statusLabel));
        vo.setOpenCount(statusCounts.stream()
                .filter(item -> {
                    TicketStatus status = TicketStatus.of(item.getLabel());
                    return status != null && status.isOpen();
                })
                .mapToLong(item -> nullToZero(item.getTotal()))
                .sum());
        vo.setCompletedCount(statusCounts.stream()
                .filter(item -> TicketStatus.COMPLETED.name().equals(item.getLabel()))
                .mapToLong(item -> nullToZero(item.getTotal()))
                .sum());
        vo.setOverdueCount(statisticsMapper.countOverdue(assigneeId, LocalDateTime.now()));
        Double avgHours = statisticsMapper.avgHandleHours(assigneeId);
        vo.setAvgHandleHours(avgHours == null ? null : BigDecimal.valueOf(avgHours).setScale(2, RoundingMode.HALF_UP));
        SatisfactionRow satisfaction = statisticsMapper.satisfaction(assigneeId);
        vo.setEvaluationCount(satisfaction == null ? 0L : nullToZero(satisfaction.getTotal()));
        vo.setAvgSatisfaction(satisfaction == null || satisfaction.getAvgScore() == null
                ? null : satisfaction.getAvgScore().setScale(2, RoundingMode.HALF_UP));
        cacheService.put(cacheKey, vo, cacheService.overviewTtl());
        return vo;
    }

    /**
     * 工单趋势（新建与完成），支持按日/周/月聚合。
     */
    @Transactional(readOnly = true)
    public List<TrendPointVO> trend(StatisticsQuery query) {
        Long assigneeId = scopedAssigneeId();
        String granularity = normalizeGranularity(query);
        String cacheKey = cacheService.trendKey(scopeKey(assigneeId), granularity);
        Optional<TrendSeries> cached = cacheService.get(cacheKey, TrendSeries.class);
        if (cached.isPresent()) {
            return cached.get().points();
        }
        LocalDate end = query.getEndDate() == null ? LocalDate.now() : query.getEndDate();
        LocalDate start = query.getStartDate() == null
                ? end.minusDays(DEFAULT_RANGE_DAYS - 1L) : query.getStartDate();
        if (start.isAfter(end)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "起始日期不能晚于结束日期");
        }
        List<DateCount> created = statisticsMapper.countCreatedByDate(start.atStartOfDay(),
                end.atTime(LocalTime.MAX), assigneeId);
        List<DateCount> closed = statisticsMapper.countClosedByDate(start.atStartOfDay(),
                end.atTime(LocalTime.MAX), assigneeId);
        List<TrendPointVO> points = buildTrendSeries(created, closed, start, end, granularity);
        cacheService.put(cacheKey, new TrendSeries(points), cacheService.statisticsTtl());
        return points;
    }

    /**
     * 故障类型分布。
     */
    @Transactional(readOnly = true)
    public List<DistributionItemVO> faultDistribution() {
        Long assigneeId = scopedAssigneeId();
        String cacheKey = cacheService.faultDistributionKey(scopeKey(assigneeId));
        Optional<DistributionList> cached = cacheService.get(cacheKey, DistributionList.class);
        if (cached.isPresent()) {
            return cached.get().items();
        }
        List<CountByOwner> rows = statisticsMapper.countByFaultCategory(assigneeId);
        Map<Long, String> names = categoryNames(rows);
        long total = rows.stream().mapToLong(row -> nullToZero(row.getTotal())).sum();
        List<DistributionItemVO> items = rows.stream()
                .map(row -> new DistributionItemVO(String.valueOf(row.getOwnerId()),
                        names.getOrDefault(row.getOwnerId(), "未分类"),
                        nullToZero(row.getTotal()), percent(nullToZero(row.getTotal()), total)))
                .toList();
        cacheService.put(cacheKey, new DistributionList(items), cacheService.statisticsTtl());
        return items;
    }

    /**
     * 优先级分布。
     */
    @Transactional(readOnly = true)
    public List<DistributionItemVO> priorityDistribution() {
        Long assigneeId = scopedAssigneeId();
        String cacheKey = cacheService.priorityDistributionKey(scopeKey(assigneeId));
        Optional<DistributionList> cached = cacheService.get(cacheKey, DistributionList.class);
        if (cached.isPresent()) {
            return cached.get().items();
        }
        List<DistributionItemVO> items = toDistribution(statisticsMapper.countByPriority(assigneeId),
                label -> {
                    TicketPriority priority = TicketPriority.of(label);
                    return priority == null ? label : priority.getLabel();
                });
        cacheService.put(cacheKey, new DistributionList(items), cacheService.statisticsTtl());
        return items;
    }

    /**
     * 工程师工作量。
     */
    @Transactional(readOnly = true)
    public List<EngineerWorkloadVO> engineerWorkload() {
        Long assigneeId = scopedAssigneeId();
        String cacheKey = cacheService.engineerWorkloadKey(scopeKey(assigneeId));
        Optional<WorkloadList> cached = cacheService.get(cacheKey, WorkloadList.class);
        if (cached.isPresent()) {
            return cached.get().items();
        }
        List<EngineerWorkloadRow> rows = statisticsMapper.engineerWorkload(assigneeId);
        Map<Long, String> names = engineerNames(rows);
        List<EngineerWorkloadVO> items = rows.stream().map(row -> {
            EngineerWorkloadVO vo = new EngineerWorkloadVO();
            vo.setEngineerId(row.getOwnerId());
            vo.setEngineerName(names.getOrDefault(row.getOwnerId(), "已删除用户"));
            vo.setTotal(nullToZero(row.getTotal()));
            vo.setCompleted(nullToZero(row.getCompleted()));
            vo.setOpen(vo.getTotal() - vo.getCompleted());
            vo.setAvgHandleHours(row.getAvgHours() == null
                    ? null : row.getAvgHours().setScale(2, RoundingMode.HALF_UP));
            return vo;
        }).toList();
        cacheService.put(cacheKey, new WorkloadList(items), cacheService.statisticsTtl());
        return items;
    }

    /**
     * 客户满意度。
     */
    @Transactional(readOnly = true)
    public SatisfactionVO satisfaction() {
        Long assigneeId = scopedAssigneeId();
        String cacheKey = cacheService.satisfactionKey(scopeKey(assigneeId));
        Optional<SatisfactionVO> cached = cacheService.get(cacheKey, SatisfactionVO.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        SatisfactionRow row = statisticsMapper.satisfaction(assigneeId);
        SatisfactionVO vo = new SatisfactionVO();
        long total = row == null ? 0L : nullToZero(row.getTotal());
        vo.setTotal(total);
        vo.setAvgScore(row == null || row.getAvgScore() == null
                ? null : row.getAvgScore().setScale(2, RoundingMode.HALF_UP));
        List<DistributionItemVO> distribution = new ArrayList<>();
        if (row != null) {
            long five = nullToZero(row.getFiveStar());
            long four = nullToZero(row.getFourStar());
            long three = nullToZero(row.getThreeStar());
            long low = nullToZero(row.getLowStar());
            distribution.add(new DistributionItemVO("5", "5 星", five, percent(five, total)));
            distribution.add(new DistributionItemVO("4", "4 星", four, percent(four, total)));
            distribution.add(new DistributionItemVO("3", "3 星", three, percent(three, total)));
            distribution.add(new DistributionItemVO("low", "1~2 星", low, percent(low, total)));
        }
        vo.setDistribution(distribution);
        cacheService.put(cacheKey, vo, cacheService.satisfactionTtl());
        return vo;
    }

    /**
     * 组装趋势序列：先按天补齐（无数据填 0），再按粒度聚合。
     */
    static List<TrendPointVO> buildTrendSeries(List<DateCount> created, List<DateCount> closed,
                                               LocalDate start, LocalDate end, String granularity) {
        Map<LocalDate, Long> createdMap = toDateMap(created);
        Map<LocalDate, Long> closedMap = toDateMap(closed);
        Map<String, long[]> aggregated = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String period = periodOf(date, granularity);
            long[] values = aggregated.computeIfAbsent(period, key -> new long[2]);
            values[0] += createdMap.getOrDefault(date, 0L);
            values[1] += closedMap.getOrDefault(date, 0L);
        }
        List<TrendPointVO> points = new ArrayList<>();
        aggregated.forEach((period, values) -> points.add(new TrendPointVO(period, values[0], values[1])));
        return points;
    }

    private static String periodOf(LocalDate date, String granularity) {
        if ("MONTH".equals(granularity)) {
            return date.format(MONTH_FORMAT);
        }
        if ("WEEK".equals(granularity)) {
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            return date.get(WeekFields.ISO.weekBasedYear()) + "-W" + String.format("%02d", week);
        }
        return date.format(DAY_FORMAT);
    }

    private static Map<LocalDate, Long> toDateMap(List<DateCount> rows) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (DateCount row : rows) {
            if (row.getStatDate() != null) {
                map.put(row.getStatDate(), nullToZero(row.getTotal()));
            }
        }
        return map;
    }

    /**
     * 统计范围：管理员/客服为全局（null），工程师为本人；客户无权访问统计。
     */
    private Long scopedAssigneeId() {
        if (dataScopeService.isCustomer()) {
            throw BusinessException.of(ErrorCode.ACCESS_DENIED, "客户角色无权访问运营统计");
        }
        return dataScopeService.canViewAll() ? null : dataScopeService.scopedAssigneeId();
    }

    private String scopeKey(Long assigneeId) {
        return assigneeId == null ? StatisticsCacheService.SCOPE_ALL : "u" + assigneeId;
    }

    private String normalizeGranularity(StatisticsQuery query) {
        String value = query.getGranularity() == null ? "DAY" : query.getGranularity().toUpperCase();
        if (!List.of("DAY", "WEEK", "MONTH").contains(value)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "粒度只能是 DAY、WEEK 或 MONTH");
        }
        return value;
    }

    private static String statusLabel(String label) {
        TicketStatus status = TicketStatus.of(label);
        return status == null ? label : status.getLabel();
    }

    private List<DistributionItemVO> toDistribution(List<LabelCount> rows, Function<String, String> labelMapper) {
        long total = rows.stream().mapToLong(row -> nullToZero(row.getTotal())).sum();
        return rows.stream()
                .map(row -> new DistributionItemVO(row.getLabel(), labelMapper.apply(row.getLabel()),
                        nullToZero(row.getTotal()), percent(nullToZero(row.getTotal()), total)))
                .toList();
    }

    private Map<Long, String> categoryNames(List<CountByOwner> rows) {
        List<Long> ids = rows.stream().map(CountByOwner::getOwnerId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (FaultCategory category : faultCategoryMapper.selectBatchIds(ids)) {
            names.put(category.getId(), category.getCategoryName());
        }
        return names;
    }

    private Map<Long, String> engineerNames(List<EngineerWorkloadRow> rows) {
        List<Long> ids = rows.stream().map(EngineerWorkloadRow::getOwnerId).filter(Objects::nonNull)
                .distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (SysUser user : sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, ids))) {
            names.put(user.getId(), user.getRealName());
        }
        return names;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static Double percent(Long value, long total) {
        if (total <= 0 || value == null) {
            return 0D;
        }
        return BigDecimal.valueOf(value * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** 缓存序列化包装类型，保持 JSON 结构稳定。 */
    record TrendSeries(List<TrendPointVO> points) {
    }

    record DistributionList(List<DistributionItemVO> items) {
    }

    record WorkloadList(List<EngineerWorkloadVO> items) {
    }
}
