package com.serviceflow.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketNoGeneratorTest {

    private final TicketNoGenerator generator = new TicketNoGenerator();

    @Test
    @DisplayName("编号格式为 SO + yyyyMMdd + 6 位当日序列")
    void shouldGenerateExpectedFormat() {
        LocalDate date = LocalDate.of(2026, 10, 15);

        assertThat(generator.prefixOf(date)).isEqualTo("SO20261015");
        assertThat(generator.generate(date, 1)).isEqualTo("SO20261015000001");
        assertThat(generator.generate(date, 123456)).isEqualTo("SO20261015123456");
        assertThat(generator.generate(date, 42)).matches("^SO20261015\\d{6}$");
    }

    @Test
    @DisplayName("当日序列超过上限时拒绝生成，避免编号溢出")
    void shouldRejectSequenceOverflow() {
        assertThatThrownBy(() -> generator.generate(LocalDate.of(2026, 10, 15), 1_000_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("上限");
    }
}
