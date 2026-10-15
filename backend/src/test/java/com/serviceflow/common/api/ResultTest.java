package com.serviceflow.common.api;

import com.serviceflow.common.enums.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("成功响应携带业务数据且 code 为 0")
    void shouldBuildSuccessResult() {
        Result<List<String>> result = Result.success(List.of("a", "b"));

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).containsExactly("a", "b");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("失败响应使用错误码并携带 traceId 与时间戳")
    void shouldBuildFailureResultWithTraceId() {
        MDC.put("traceId", "trace-123");

        Result<Void> result = Result.failure(ErrorCode.TICKET_NOT_FOUND);

        assertThat(result.getCode()).isEqualTo(1401);
        assertThat(result.getMessage()).isEqualTo("工单不存在");
        assertThat(result.getData()).isNull();
        assertThat(result.getTraceId()).isEqualTo("trace-123");
        assertThat(result.getTimestamp()).isNotBlank();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("分页响应根据总数与每页条数计算页数")
    void shouldCalculatePages() {
        PageResult<String> empty = PageResult.empty(1, 10);
        assertThat(empty.getPages()).isZero();
        assertThat(empty.getRecords()).isEmpty();

        PageResult<String> pageResult = new PageResult<>(List.of("a"), 21, 2, 10);
        assertThat(pageResult.getPages()).isEqualTo(3);
        assertThat(pageResult.getPageNum()).isEqualTo(2);
        assertThat(pageResult.getTotal()).isEqualTo(21);
    }
}
