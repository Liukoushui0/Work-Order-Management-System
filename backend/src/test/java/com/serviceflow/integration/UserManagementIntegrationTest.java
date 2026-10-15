package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户管理集成测试：通过真实 HTTP 接口验证新增用户、禁用、角色分配与操作审计。
 */
class UserManagementIntegrationTest extends BaseIntegrationTest {

    private static final String NEW_ENGINEER = "it_new_engineer";
    private static final String DUPLICATE_USER = "it_duplicate";
    private static final String DISABLED_USER = "it_disabled_user";
    private static final String ROLE_USER = "it_role_user";
    private static final String LOG_USER = "it_log_user";
    private static final String INITIAL_PASSWORD = "Abcd1234";

    private Map<String, Object> createUserBody(String username, long roleId) {
        return Map.of(
                "username", username,
                "password", INITIAL_PASSWORD,
                "realName", "接口创建用户",
                "phone", "13900000001",
                "email", username + "@serviceflow.local",
                "roleIds", List.of(roleId));
    }

    @Test
    @DisplayName("管理员新增用户后，新账号可正常登录并查到自己的角色")
    void adminCreatesUserAndNewUserCanLogin() throws Exception {
        String adminToken = login("admin", PASSWORD);

        JsonNode created = call(HttpMethod.POST, "/api/v1/users", adminToken,
                createUserBody(NEW_ENGINEER, ROLE_ENGINEER), 200);

        assertThat(created.path("code").asInt()).isZero();
        assertThat(created.path("data").path("username").asText()).isEqualTo(NEW_ENGINEER);
        assertThat(created.path("data").path("roles").toString()).contains("ROLE_ENGINEER");
        // 响应中不得包含任何密码字段
        assertThat(created.path("data").has("password")).isFalse();

        String newUserToken = login(NEW_ENGINEER, INITIAL_PASSWORD);
        JsonNode me = call(HttpMethod.GET, "/api/v1/auth/me", newUserToken, null, 200);
        assertThat(me.path("data").path("roles").toString()).contains("ROLE_ENGINEER");
    }

    @Test
    @DisplayName("账号重复时拒绝新增")
    void duplicateUsernameIsRejected() throws Exception {
        String adminToken = login("admin", PASSWORD);
        createTestUser(DUPLICATE_USER, ROLE_ENGINEER, 1);

        JsonNode response = call(HttpMethod.POST, "/api/v1/users", adminToken,
                createUserBody(DUPLICATE_USER, ROLE_ENGINEER), 400);

        assertThat(response.path("code").asInt()).isEqualTo(1201);
        assertThat(response.path("message").asText()).contains("登录账号已存在");
    }

    @Test
    @DisplayName("禁用账号后该用户无法登录")
    void disabledUserCannotLogin() throws Exception {
        String adminToken = login("admin", PASSWORD);
        JsonNode created = call(HttpMethod.POST, "/api/v1/users", adminToken,
                createUserBody(DISABLED_USER, ROLE_ENGINEER), 200);
        long userId = created.path("data").path("id").asLong();
        assertThat(login(DISABLED_USER, INITIAL_PASSWORD)).isNotBlank();

        call(HttpMethod.PUT, "/api/v1/users/" + userId + "/status", adminToken, Map.of("status", 0), 200);

        JsonNode loginAttempt = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", DISABLED_USER, "password", INITIAL_PASSWORD), 403);
        assertThat(loginAttempt.path("code").asInt()).isEqualTo(1002);
    }

    @Test
    @DisplayName("管理员不能禁用自己")
    void adminCannotDisableSelf() throws Exception {
        String adminToken = login("admin", PASSWORD);
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'admin'", Long.class);

        JsonNode response = call(HttpMethod.PUT, "/api/v1/users/" + adminId + "/status", adminToken,
                Map.of("status", 0), 400);

        assertThat(response.path("message").asText()).contains("不能禁用当前登录账号");
    }

    @Test
    @DisplayName("角色变更后，权限立即按新角色生效")
    void assignedRolesTakeEffectImmediately() throws Exception {
        String adminToken = login("admin", PASSWORD);
        JsonNode created = call(HttpMethod.POST, "/api/v1/users", adminToken,
                createUserBody(ROLE_USER, ROLE_ENGINEER), 200);
        long userId = created.path("data").path("id").asLong();

        String engineerToken = login(ROLE_USER, INITIAL_PASSWORD);
        assertThat(call(HttpMethod.GET, "/api/v1/configs", engineerToken, null, 403).path("code").asInt())
                .isEqualTo(1101);

        call(HttpMethod.PUT, "/api/v1/users/" + userId + "/roles", adminToken,
                Map.of("roleIds", List.of(ROLE_ADMIN)), 200);

        String promotedToken = login(ROLE_USER, INITIAL_PASSWORD);
        assertThat(call(HttpMethod.GET, "/api/v1/configs", promotedToken, null, 200).path("code").asInt())
                .isZero();
    }

    @Test
    @DisplayName("管理员重置密码后新密码生效，原密码失效")
    void adminResetPasswordTakesEffect() throws Exception {
        String adminToken = login("admin", PASSWORD);
        JsonNode created = call(HttpMethod.POST, "/api/v1/users", adminToken,
                createUserBody(DUPLICATE_USER, ROLE_ENGINEER), 200);
        long userId = created.path("data").path("id").asLong();

        call(HttpMethod.POST, "/api/v1/users/" + userId + "/reset-password", adminToken,
                Map.of("newPassword", "Reset2026pass"), 200);

        call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", DUPLICATE_USER, "password", INITIAL_PASSWORD), 401);
        assertThat(login(DUPLICATE_USER, "Reset2026pass")).isNotBlank();
    }

    @Test
    @DisplayName("关键操作写入审计日志，包含操作人、模块与结果")
    void criticalOperationsAreAudited() throws Exception {
        String adminToken = login("admin", PASSWORD);
        call(HttpMethod.POST, "/api/v1/users", adminToken, createUserBody(LOG_USER, ROLE_ENGINEER), 200);

        JsonNode logs = call(HttpMethod.GET,
                "/api/v1/operation-logs?module=user&username=admin&pageSize=20", adminToken, null, 200);

        assertThat(logs.path("data").path("total").asLong()).isGreaterThan(0);
        boolean found = false;
        for (JsonNode record : logs.path("data").path("records")) {
            if ("新增用户".equals(record.path("operation").asText()) && record.path("success").asInt() == 1) {
                found = true;
                assertThat(record.path("requestUri").asText()).isEqualTo("/api/v1/users");
                assertThat(record.path("traceId").asText()).isNotBlank();
            }
        }
        assertThat(found).as("应存在一条“新增用户”的成功审计记录").isTrue();
    }

    @Test
    @DisplayName("登录失败同样写入审计日志")
    void failedLoginIsAudited() throws Exception {
        createTestUser(DUPLICATE_USER, ROLE_ENGINEER, 1);

        call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", DUPLICATE_USER, "password", "WrongPassword@2026"), 401);

        String adminToken = login("admin", PASSWORD);
        JsonNode logs = call(HttpMethod.GET,
                "/api/v1/operation-logs?module=auth&success=0&pageSize=20", adminToken, null, 200);

        boolean found = false;
        for (JsonNode record : logs.path("data").path("records")) {
            if (record.path("success").asInt() == 0) {
                found = true;
            }
            // 审计日志中不得出现明文密码
            assertThat(record.path("requestParams").asText()).doesNotContain("WrongPassword@2026");
        }
        assertThat(found).as("应存在一条登录失败的审计记录").isTrue();
    }
}
