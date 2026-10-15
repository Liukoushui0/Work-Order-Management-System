package com.serviceflow.common.enums;

/**
 * 工单处理记录中的动作类型，用于处理历史展示与审计。
 */
public enum TicketAction {

    CREATE("创建工单"),
    ASSIGN("分派"),
    ACCEPT("接单"),
    TRANSFER("转派"),
    REJECT("退回"),
    PROCESS("处理记录"),
    COMPLETE("提交处理结果"),
    CONFIRM("客户确认"),
    AUTO_CONFIRM("超期自动确认"),
    REOPEN("退回重做或重新打开"),
    EVALUATE("提交服务评价"),
    CANCEL("取消工单");

    private final String label;

    TicketAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
