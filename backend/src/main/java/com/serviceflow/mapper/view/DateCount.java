package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 按日期聚合的计数结果，用于工单趋势。
 */
@Getter
@Setter
public class DateCount {

    private LocalDate statDate;
    private Long total;
}
