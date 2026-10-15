package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类：连接真实的 MySQL（serviceflow_test）与 Redis，通过 HTTP 接口驱动业务。
 *
 * <p>测试数据使用 it_ 前缀，结束后统一清理，避免污染开发库与历史数据。</p>
 *
 * <p>运行方式：{@code mvn verify -Pintegration}（需要本地 MySQL 与 Redis 已启动）。</p>
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /** 演示账号统一密码（与 db/data.sql 中的 BCrypt 哈希对应）。 */
    protected static final String PASSWORD = "Serviceflow@2026";

    /** 与 data.sql 中演示账号相同明文密码的 BCrypt 哈希，供测试自建账号使用。 */
    protected static final String TEST_USER_HASH =
            "$2a$10$kK8mSbnIo3PVy/0wPXcgeOAprcD5TUICFKQM6t2p2SDl1ExwfYxv.";

    protected static final long ROLE_ADMIN = 1L;
    protected static final long ROLE_ENGINEER = 3L;
    protected static final long ROLE_CUSTOMER = 4L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur INNER JOIN sys_user u ON u.id = ur.user_id "
                + "WHERE u.username LIKE 'it\\_%'");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'it\\_%'");
        jdbcTemplate.update("DELETE FROM fault_category WHERE category_code LIKE 'IT-%'");
    }

    /**
     * 登录并返回 access token（断言登录成功）。
     */
    protected String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    /**
     * 发起任意请求并断言 HTTP 状态码，返回响应 JSON。
     */
    protected JsonNode call(HttpMethod method, String url, String token, Object requestBody,
                            int expectedStatus) throws Exception {
        var builder = request(method, url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (requestBody != null) {
            builder.content(objectMapper.writeValueAsString(requestBody));
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().is(expectedStatus)).andReturn();
        return body(result);
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 直接用 SQL 创建测试账号（绕过接口，便于构造锁定、禁用等前置状态）。
     */
    protected Long createTestUser(String username, long roleId, int status) {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, real_name, status, remark, deleted) "
                + "VALUES (?, ?, ?, ?, 'integration-test', 0)", username, TEST_USER_HASH, "集成测试用户", status);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }
}
