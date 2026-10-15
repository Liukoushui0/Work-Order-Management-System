package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 工程师工作量聚合结果。
 */
@Getter
@Setter
public class EngineerWorkloadRow {

    private Long ownerId;
    private Long total;
    private Long completed;
    private BigDecimal avgHours;
}
