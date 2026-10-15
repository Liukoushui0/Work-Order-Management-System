package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 健康检查与统一异常结构的集成测试：真实探测 MySQL 与 Redis 连通性。
 */
class HealthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("健康检查返回应用、数据库与 Redis 状态，并回传 traceId")
    void healthEndpointReportsDependencies() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        JsonNode response = body(result);

        assertThat(response.path("code").asInt()).isZero();
        assertThat(response.path("data").path("application").asText()).isEqualTo("serviceflow");
        assertThat(response.path("data").path("status").asText()).isEqualTo("UP");
        assertThat(response.path("data").path("database").asText()).isEqualTo("UP");
        assertThat(response.path("data").path("redis").asText()).isEqualTo("UP");
        assertThat(response.path("traceId").asText()).isNotBlank();
        assertThat(result.getResponse().getHeader("X-Trace-Id")).isEqualTo(response.path("traceId").asText());
    }

    @Test
    @DisplayName("已登录用户访问未知路径返回统一 404 结构而不是默认错误页")
    void unknownPathUsesUnifiedErrorStructure() throws Exception {
        String token = login("admin", PASSWORD);

        JsonNode response = call(HttpMethod.GET, "/api/v1/not-exists", token, null, 404);

        assertThat(response.path("code").asInt()).isEqualTo(1604);
        assertThat(response.path("message").asText()).isEqualTo("请求的资源不存在");
        assertThat(response.path("traceId").asText()).isNotBlank();
    }
}
