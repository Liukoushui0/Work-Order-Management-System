package com.serviceflow.common.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态与允许的状态流转。
 *
 * <p>主流程：待分派 → 待接单 → 处理中 → 待确认 → 已完成；
 * 特殊流转：退回（待接单 → 待分派）、转派（保持待接单）、退回重做（待确认 → 处理中）、
 * 重新打开（已完成 → 待分派）、取消（非终态 → 已取消）。</p>
 */
public enum TicketStatus {

    PENDING_ASSIGN("待分派"),
    PENDING_ACCEPT("待接单"),
    PROCESSING("处理中"),
    PENDING_CONFIRM("待确认"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PENDING_ASSIGN, EnumSet.of(PENDING_ACCEPT, CANCELLED));
        // 转派后仍处于待接单，因此允许指向自身
        ALLOWED_TRANSITIONS.put(PENDING_ACCEPT, EnumSet.of(PROCESSING, PENDING_ACCEPT, PENDING_ASSIGN, CANCELLED));
        ALLOWED_TRANSITIONS.put(PROCESSING, EnumSet.of(PENDING_CONFIRM, PENDING_ACCEPT, CANCELLED));
        ALLOWED_TRANSITIONS.put(PENDING_CONFIRM, EnumSet.of(COMPLETED, PROCESSING, CANCELLED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.of(PENDING_ASSIGN));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(TicketStatus.class));
    }

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 是否允许从当前状态流转到目标状态。
     */
    public boolean canTransitionTo(TicketStatus target) {
        return target != null && ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * 终态工单不允许再做任何变更。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 是否属于"未结束"状态（用于逾期统计与客户/设备的删除保护）。
     */
    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }

    public static TicketStatus of(String value) {
        for (TicketStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
