package com.serviceflow.service;

import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.TicketProcessRecord;
import com.serviceflow.mapper.ServiceTicketMapper;
import com.serviceflow.mapper.StatisticsMapper;
import com.serviceflow.mapper.TicketProcessRecordMapper;
import com.serviceflow.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 定时任务单元测试：超期自动确认与逾期提醒的业务规则。
 */
@ExtendWith(MockitoExtension.class)
class TicketScheduledServiceTest {

    @Mock private ServiceTicketMapper ticketMapper;
    @Mock private TicketProcessRecordMapper recordMapper;
    @Mock private StatisticsMapper statisticsMapper;
    @Mock private ConfigService configService;
    @Mock private StatisticsCacheService cacheService;

    private TicketScheduledService scheduledService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(ServiceTicket.class, TicketProcessRecord.class);
    }

    @BeforeEach
    void setUp() {
        scheduledService = new TicketScheduledService(ticketMapper, recordMapper, statisticsMapper,
                configService, cacheService);
    }

    private ServiceTicket pendingConfirm(Long id) {
        ServiceTicket ticket = new ServiceTicket();
        ticket.setId(id);
        ticket.setTicketNo("SO2026101500000" + id);
        ticket.setStatus(TicketStatus.PENDING_CONFIRM.name());
        ticket.setVersion(0);
        ticket.setFinishTime(LocalDateTime.now().minusHours(100));
        return ticket;
    }

    @Test
    @DisplayName("配置关闭时不执行自动确认")
    void shouldSkipWhenDisabled() {
        given(configService.getBoolean("ticket.auto_confirm_enabled", true)).willReturn(false);

        assertThat(scheduledService.autoConfirmExpiredTickets()).isZero();

        verify(ticketMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("超期工单被自动确认，并写入 SYSTEM 操作人记录")
    void shouldAutoConfirmExpiredTickets() {
        given(configService.getBoolean("ticket.auto_confirm_enabled", true)).willReturn(true);
        given(configService.getInt("ticket.confirm_timeout_hours", 72)).willReturn(72);
        given(ticketMapper.selectList(any())).willReturn(List.of(pendingConfirm(9L)));
        given(ticketMapper.update(any(), any())).willReturn(1);

        int confirmed = scheduledService.autoConfirmExpiredTickets();

        assertThat(confirmed).isEqualTo(1);
        ArgumentCaptor<TicketProcessRecord> captor = ArgumentCaptor.forClass(TicketProcessRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("AUTO_CONFIRM");
        assertThat(captor.getValue().getOperatorName()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getOperatorId()).isNull();
        assertThat(captor.getValue().getToStatus()).isEqualTo(TicketStatus.COMPLETED.name());
        verify(cacheService).evictStatistics();
    }

    @Test
    @DisplayName("并发下状态被他人改变时不重复确认，也不写记录")
    void shouldSkipTicketChangedByOthers() {
        given(configService.getBoolean("ticket.auto_confirm_enabled", true)).willReturn(true);
        given(configService.getInt("ticket.confirm_timeout_hours", 72)).willReturn(72);
        given(ticketMapper.selectList(any())).willReturn(List.of(pendingConfirm(9L)));
        given(ticketMapper.update(any(), any())).willReturn(0);

        assertThat(scheduledService.autoConfirmExpiredTickets()).isZero();

        verify(recordMapper, never()).insert(any(TicketProcessRecord.class));
        verify(cacheService, never()).evictStatistics();
    }

    @Test
    @DisplayName("没有逾期工单时提醒返回 0 且不查询明细")
    void shouldSkipReminderWhenNoOverdueTickets() {
        given(statisticsMapper.countOverdue(any(), any())).willReturn(0L);

        assertThat(scheduledService.remindOverdueTickets()).isZero();

        verify(ticketMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("存在逾期工单时提醒返回真实数量")
    void shouldReportOverdueCount() {
        given(statisticsMapper.countOverdue(any(), any())).willReturn(3L);
        given(ticketMapper.selectList(any())).willReturn(List.of(pendingConfirm(1L)));

        assertThat(scheduledService.remindOverdueTickets()).isEqualTo(3L);
    }
}
