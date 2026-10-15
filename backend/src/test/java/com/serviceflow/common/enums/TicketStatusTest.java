package com.serviceflow.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单状态机转换矩阵测试。
 */
class TicketStatusTest {

    @Test
    @DisplayName("主流程逐级流转被允许")
    void shouldAllowMainFlow() {
        assertThat(TicketStatus.PENDING_ASSIGN.canTransitionTo(TicketStatus.PENDING_ACCEPT)).isTrue();
        assertThat(TicketStatus.PENDING_ACCEPT.canTransitionTo(TicketStatus.PROCESSING)).isTrue();
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.PENDING_CONFIRM)).isTrue();
        assertThat(TicketStatus.PENDING_CONFIRM.canTransitionTo(TicketStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("特殊流转：退回、转派、退回重做、重新打开、取消被允许")
    void shouldAllowSpecialTransitions() {
        // 退回：待接单 → 待分派
        assertThat(TicketStatus.PENDING_ACCEPT.canTransitionTo(TicketStatus.PENDING_ASSIGN)).isTrue();
        // 转派：待接单/处理中 → 待接单
        assertThat(TicketStatus.PENDING_ACCEPT.canTransitionTo(TicketStatus.PENDING_ACCEPT)).isTrue();
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.PENDING_ACCEPT)).isTrue();
        // 退回重做：待确认 → 处理中
        assertThat(TicketStatus.PENDING_CONFIRM.canTransitionTo(TicketStatus.PROCESSING)).isTrue();
        // 重新打开：已完成 → 待分派
        assertThat(TicketStatus.COMPLETED.canTransitionTo(TicketStatus.PENDING_ASSIGN)).isTrue();
        // 取消：非终态 → 已取消
        assertThat(TicketStatus.PENDING_ASSIGN.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("非法跳转被拒绝")
    void shouldRejectIllegalTransitions() {
        // 待分派不能直接接单或完成
        assertThat(TicketStatus.PENDING_ASSIGN.canTransitionTo(TicketStatus.PROCESSING)).isFalse();
        assertThat(TicketStatus.PENDING_ASSIGN.canTransitionTo(TicketStatus.COMPLETED)).isFalse();
        // 待接单不能直接完成或确认
        assertThat(TicketStatus.PENDING_ACCEPT.canTransitionTo(TicketStatus.COMPLETED)).isFalse();
        assertThat(TicketStatus.PENDING_ACCEPT.canTransitionTo(TicketStatus.PENDING_CONFIRM)).isFalse();
        // 处理中不能跳过待确认直接完成
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.COMPLETED)).isFalse();
        // 待确认不能回到待分派
        assertThat(TicketStatus.PENDING_CONFIRM.canTransitionTo(TicketStatus.PENDING_ASSIGN)).isFalse();
    }

    @Test
    @DisplayName("已取消是终态，不允许任何后续流转")
    void cancelledIsFinalState() {
        assertThat(TicketStatus.CANCELLED.isTerminal()).isTrue();
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatus.CANCELLED.canTransitionTo(target))
                    .as("已取消 → %s 不应被允许", target)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("终态与未结束状态判定正确")
    void shouldJudgeTerminalAndOpenStates() {
        assertThat(TicketStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(TicketStatus.COMPLETED.isOpen()).isFalse();
        assertThat(TicketStatus.PENDING_ASSIGN.isOpen()).isTrue();
        assertThat(TicketStatus.PENDING_CONFIRM.isOpen()).isTrue();
    }

    @Test
    @DisplayName("字符串解析忽略大小写，未知值返回 null")
    void shouldParseStatusText() {
        assertThat(TicketStatus.of("processing")).isEqualTo(TicketStatus.PROCESSING);
        assertThat(TicketStatus.of("PENDING_CONFIRM")).isEqualTo(TicketStatus.PENDING_CONFIRM);
        assertThat(TicketStatus.of("NOT_A_STATUS")).isNull();
    }
}
