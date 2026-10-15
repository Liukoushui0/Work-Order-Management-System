package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 客户满意度聚合结果。
 */
@Getter
@Setter
public class SatisfactionRow {

    private Long total;
    private BigDecimal avgScore;
    private Long fiveStar;
    private Long fourStar;
    private Long threeStar;
    private Long lowStar;
}
