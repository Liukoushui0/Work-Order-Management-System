package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.dashboard.StatisticsQuery;
import com.serviceflow.mapper.StatisticsMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.FaultCategoryMapper;
import com.serviceflow.mapper.view.DateCount;
import com.serviceflow.security.DataScopeService;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.dashboard.TrendPointVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private StatisticsMapper statisticsMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private FaultCategoryMapper faultCategoryMapper;
    @Mock private StatisticsCacheService cacheService;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(statisticsMapper, sysUserMapper, faultCategoryMapper,
                cacheService, new DataScopeService());
        loginAs(AccessGuardRole.ADMIN);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String roleCode) {
        UserPrincipal principal = new UserPrincipal(1L, "tester", "hash", "测试用户", null, true,
                Set.of(roleCode), Set.of("dashboard:view", "dashboard:view-all"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static DateCount row(String date, long total) {
        DateCount count = new DateCount();
        count.setStatDate(LocalDate.parse(date));
        count.setTotal(total);
        return count;
    }

    /** 便于测试中引用角色常量。 */
    private static final class AccessGuardRole {
        static final String ADMIN = "ROLE_ADMIN";
        static final String CUSTOMER = "ROLE_CUSTOMER";
    }

    @Test
    @DisplayName("趋势按天补齐缺失日期（无数据填 0）")
    void shouldFillMissingDaysWithZero() {
        List<TrendPointVO> points = StatisticsService.buildTrendSeries(
                List.of(row("2026-10-01", 2), row("2026-10-03", 1)),
                List.of(row("2026-10-02", 1)),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-04"), "DAY");

        assertThat(points).hasSize(4);
        assertThat(points.get(0).getPeriod()).isEqualTo("2026-10-01");
        assertThat(points.get(0).getCreated()).isEqualTo(2);
        assertThat(points.get(1).getCompleted()).isEqualTo(1);
        assertThat(points.get(2).getCreated()).isEqualTo(1);
        assertThat(points.get(3).getCreated()).isZero();
        assertThat(points.get(3).getCompleted()).isZero();
    }

    @Test
    @DisplayName("趋势可按周与按月聚合")
    void shouldAggregateByWeekAndMonth() {
        List<TrendPointVO> weekly = StatisticsService.buildTrendSeries(
                List.of(row("2026-10-05", 1), row("2026-10-06", 2), row("2026-10-13", 1)),
                List.of(), LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-14"), "WEEK");
        assertThat(weekly).hasSize(2);
        assertThat(weekly.get(0).getPeriod()).isEqualTo("2026-W41");
        assertThat(weekly.get(0).getCreated()).isEqualTo(3);

        List<TrendPointVO> monthly = StatisticsService.buildTrendSeries(
                List.of(row("2026-09-30", 1), row("2026-10-01", 2)),
                List.of(), LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-02"), "MONTH");
        assertThat(monthly).hasSize(2);
        assertThat(monthly.get(0).getPeriod()).isEqualTo("2026-09");
        assertThat(monthly.get(1).getCreated()).isEqualTo(2);
    }

    @Test
    @DisplayName("起始日期晚于结束日期时报参数错误")
    void shouldRejectInvalidDateRange() {
        StatisticsQuery query = new StatisticsQuery();
        query.setStartDate(LocalDate.parse("2026-10-10"));
        query.setEndDate(LocalDate.parse("2026-10-01"));
        given(cacheService.trendKey(any(), any())).willReturn("sf:test:stats:trend:all:DAY");
        given(cacheService.get(any(), any())).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> statisticsService.trend(query))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("非法粒度被拒绝")
    void shouldRejectInvalidGranularity() {
        StatisticsQuery query = new StatisticsQuery();
        query.setGranularity("HOUR");

        assertThatThrownBy(() -> statisticsService.trend(query))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("客户角色访问统计被拒绝，且不会查询数据库")
    void shouldRejectCustomerRole() {
        loginAs(AccessGuardRole.CUSTOMER);

        assertThatThrownBy(() -> statisticsService.overview())
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(statisticsMapper, never()).countTotal(any());
    }
}
