package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

/**
 * 按归属维度聚合的计数结果（批量查询，避免 N+1）。
 */
@Getter
@Setter
public class CountByOwner {

    private Long ownerId;
    private Long total;
}
