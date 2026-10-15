package com.serviceflow.common.enums;

/**
 * 工单优先级与默认 SLA（小时）。创建工单未显式指定截止时间时按此计算。
 */
public enum TicketPriority {

    LOW("低", 48),
    MEDIUM("中", 24),
    HIGH("高", 8),
    URGENT("紧急", 4);

    private final String label;
    private final int slaHours;

    TicketPriority(String label, int slaHours) {
        this.label = label;
        this.slaHours = slaHours;
    }

    public String getLabel() {
        return label;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public static TicketPriority of(String value) {
        for (TicketPriority priority : values()) {
            if (priority.name().equalsIgnoreCase(value)) {
                return priority;
            }
        }
        return null;
    }
}
