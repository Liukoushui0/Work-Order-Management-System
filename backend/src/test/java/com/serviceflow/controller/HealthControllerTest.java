package com.serviceflow.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 健康检查接口的 Web 层测试：验证统一响应结构、响应头 traceId 与依赖状态探测。
 */
@WebMvcTest(controllers = HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("依赖正常时返回 code=0 且数据库与 Redis 均为 UP")
    void shouldReturnUpWhenDependenciesHealthy() throws Exception {
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(true);

        RedisConnection redisConnection = mock(RedisConnection.class);
        given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
        given(redisConnection.ping()).willReturn("PONG");

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("数据库不可用时仍返回 200，但状态标记为 DOWN")
    void shouldReportDownWhenDatabaseFails() throws Exception {
        given(dataSource.getConnection()).willThrow(new IllegalStateException("db down"));

        RedisConnection redisConnection = mock(RedisConnection.class);
        given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
        given(redisConnection.ping()).willReturn("PONG");

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.database").value("DOWN"));
    }
}
