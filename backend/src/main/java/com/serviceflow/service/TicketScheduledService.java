package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.TicketAction;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.TicketProcessRecord;
import com.serviceflow.mapper.ServiceTicketMapper;
import com.serviceflow.mapper.StatisticsMapper;
import com.serviceflow.mapper.TicketProcessRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单定时任务：超期自动确认与逾期提醒。
 *
 * 自动确认：待确认工单超过 ticket.confirm_timeout_hours（默认 72 小时）仍未确认时由系统确认完成，
 * 并写入 AUTO_CONFIRM 处理记录（操作人记为 SYSTEM）。是否启用由 ticket.auto_confirm_enabled 控制。
 * 逾期提醒：按小时扫描逾期工单并输出结构化告警日志，便于运维查看或后续接入通知渠道。
 */
@Service
public class TicketScheduledService {

    private static final Logger log = LoggerFactory.getLogger(TicketScheduledService.class);
    private static final String CONFIG_AUTO_CONFIRM_ENABLED = "ticket.auto_confirm_enabled";
    private static final String CONFIG_CONFIRM_TIMEOUT_HOURS = "ticket.confirm_timeout_hours";
    private static final String SYSTEM_OPERATOR = "SYSTEM";
    private static final int BATCH_LIMIT = 200;
    private static final int REMINDER_SAMPLE_LIMIT = 20;

    private final ServiceTicketMapper ticketMapper;
    private final TicketProcessRecordMapper recordMapper;
    private final StatisticsMapper statisticsMapper;
    private final ConfigService configService;
    private final StatisticsCacheService cacheService;

    public TicketScheduledService(ServiceTicketMapper ticketMapper,
                                  TicketProcessRecordMapper recordMapper,
                                  StatisticsMapper statisticsMapper,
                                  ConfigService configService,
                                  StatisticsCacheService cacheService) {
        this.ticketMapper = ticketMapper;
        this.recordMapper = recordMapper;
        this.statisticsMapper = statisticsMapper;
        this.configService = configService;
        this.cacheService = cacheService;
    }

    /**
     * 超期自动确认，默认每 10 分钟执行一次。
     *
     * @return 本次自动确认的工单数量
     */
    @Scheduled(cron = "${app.schedule.auto-confirm-cron:0 0/10 * * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public int autoConfirmExpiredTickets() {
        if (!configService.getBoolean(CONFIG_AUTO_CONFIRM_ENABLED, true)) {
            log.debug("超期自动确认已通过系统配置关闭");
            return 0;
        }
        int timeoutHours = Math.max(configService.getInt(CONFIG_CONFIRM_TIMEOUT_HOURS, 72), 1);
        LocalDateTime threshold = LocalDateTime.now().minusHours(timeoutHours);
        List<ServiceTicket> candidates = ticketMapper.selectList(Wrappers.<ServiceTicket>lambdaQuery()
                .eq(ServiceTicket::getStatus, TicketStatus.PENDING_CONFIRM.name())
                .isNotNull(ServiceTicket::getFinishTime)
                .le(ServiceTicket::getFinishTime, threshold)
                .last("LIMIT " + BATCH_LIMIT));
        int confirmed = 0;
        for (ServiceTicket ticket : candidates) {
            LocalDateTime now = LocalDateTime.now();
            int rows = ticketMapper.update(null, Wrappers.<ServiceTicket>lambdaUpdate()
                    .eq(ServiceTicket::getId, ticket.getId())
                    .eq(ServiceTicket::getStatus, TicketStatus.PENDING_CONFIRM.name())
                    .eq(ServiceTicket::getVersion, ticket.getVersion())
                    .set(ServiceTicket::getStatus, TicketStatus.COMPLETED.name())
                    .set(ServiceTicket::getConfirmTime, now)
                    .set(ServiceTicket::getClosedTime, now)
                    .setSql("version = version + 1"));
            if (rows == 0) {
                continue;
            }
            confirmed = confirmed + 1;
            recordMapper.insert(buildSystemRecord(ticket, timeoutHours));
        }
        if (confirmed > 0) {
            log.info("超期自动确认完成 {} 张工单（阈值 {} 小时）", confirmed, timeoutHours);
            cacheService.evictStatistics();
        }
        return confirmed;
    }

    /**
     * 逾期提醒，默认每小时执行一次。
     *
     * @return 当前逾期工单数量
     */
    @Scheduled(cron = "${app.schedule.overdue-reminder-cron:0 0 * * * ?}")
    @Transactional(readOnly = true)
    public long remindOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        long overdueCount = statisticsMapper.countOverdue(null, now);
        if (overdueCount == 0) {
            log.debug("当前没有逾期工单");
            return 0;
        }
        List<ServiceTicket> samples = ticketMapper.selectList(Wrappers.<ServiceTicket>lambdaQuery()
                .notIn(ServiceTicket::getStatus, TicketStatus.COMPLETED.name(), TicketStatus.CANCELLED.name())
                .isNotNull(ServiceTicket::getDeadlineTime)
                .lt(ServiceTicket::getDeadlineTime, now)
                .orderByAsc(ServiceTicket::getDeadlineTime)
                .last("LIMIT " + REMINDER_SAMPLE_LIMIT));
        log.warn("逾期工单提醒：共 {} 张，最早的 {} 张如下", overdueCount, samples.size());
        for (ServiceTicket ticket : samples) {
            log.warn("逾期工单 ticketNo={} status={} deadline={} assigneeId={}",
                    ticket.getTicketNo(), ticket.getStatus(), ticket.getDeadlineTime(), ticket.getAssigneeId());
        }
        return overdueCount;
    }

    private TicketProcessRecord buildSystemRecord(ServiceTicket ticket, int timeoutHours) {
        TicketProcessRecord record = new TicketProcessRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorId(null);
        record.setOperatorName(SYSTEM_OPERATOR);
        record.setOperatorRole(SYSTEM_OPERATOR);
        record.setAction(TicketAction.AUTO_CONFIRM.name());
        record.setFromStatus(TicketStatus.PENDING_CONFIRM.name());
        record.setToStatus(TicketStatus.COMPLETED.name());
        record.setContent("超过 " + timeoutHours + " 小时未确认，系统自动确认完成");
        return record;
    }
}
