package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

/**
 * 按枚举文本聚合的计数结果（状态、优先级等）。
 */
@Getter
@Setter
public class LabelCount {

    private String label;
    private Long total;
}
