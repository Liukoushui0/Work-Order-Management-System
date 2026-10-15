package com.serviceflow.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工单编号生成器：SO + yyyyMMdd + 6 位当日序列，例如 SO20261015000001。
 *
 * 序列号由数据库查询当日最大值得到，最终由唯一索引 uk_ticket_no 兜底；
 * 并发冲突时由调用方重试，保证编号唯一。
 */
@Component
public class TicketNoGenerator {

    private static final String PREFIX = "SO";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SEQUENCE_LENGTH = 6;
    private static final long MAX_SEQUENCE = 999999L;

    /**
     * 当日编号前缀，如 SO20261015。
     */
    public String prefixOf(LocalDate date) {
        return PREFIX + date.format(DATE_FORMATTER);
    }

    /**
     * 按当日序列号生成完整工单编号。
     */
    public String generate(LocalDate date, long sequence) {
        if (sequence > MAX_SEQUENCE) {
            throw new IllegalStateException("当日工单数量已达上限，请联系管理员");
        }
        return prefixOf(date) + String.format("%0" + SEQUENCE_LENGTH + "d", sequence);
    }
}
