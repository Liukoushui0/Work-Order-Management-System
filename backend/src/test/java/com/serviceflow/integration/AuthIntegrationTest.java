package com.serviceflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证与鉴权集成测试：覆盖登录成功/失败、未登录访问、角色权限边界与退出登录。
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    private static final String WRONG_PASSWORD_USER = "it_auth_wrongpwd";
    private static final String LOCKED_USER = "it_auth_locked";
    private static final String DISABLED_USER = "it_auth_disabled";
    private static final String PASSWORD_USER = "it_auth_password";

    @Test
    @DisplayName("管理员登录成功，/auth/me 返回角色与权限集合")
    void adminCanLoginAndReadCurrentUser() throws Exception {
        String token = login("admin", PASSWORD);

        assertThat(token).isNotBlank();

        JsonNode me = call(HttpMethod.GET, "/api/v1/auth/me", token, null, 200);

        assertThat(me.path("code").asInt()).isZero();
        assertThat(me.path("data").path("username").asText()).isEqualTo("admin");
        assertThat(me.path("data").path("roles").toString()).contains("ROLE_ADMIN");
        assertThat(me.path("data").path("permissions").size()).isGreaterThan(0);
        assertThat(me.path("traceId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("密码错误返回统一 1001 结构，且不泄露账号是否存在")
    void wrongPasswordReturnsGenericError() throws Exception {
        createTestUser(WRONG_PASSWORD_USER, ROLE_ENGINEER, 1);

        JsonNode response = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", WRONG_PASSWORD_USER, "password", "WrongPassword@2026"), 401);

        assertThat(response.path("code").asInt()).isEqualTo(1001);
        assertThat(response.path("message").asText()).isEqualTo("用户名或密码错误");

        JsonNode notExists = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", "no_such_user", "password", "WrongPassword@2026"), 401);
        assertThat(notExists.path("code").asInt()).isEqualTo(1001);
    }

    @Test
    @DisplayName("连续失败达到阈值后账号被锁定，正确密码也无法登录")
    void accountIsLockedAfterRepeatedFailures() throws Exception {
        createTestUser(LOCKED_USER, ROLE_ENGINEER, 1);
        Map<String, String> wrongLogin = Map.of("username", LOCKED_USER, "password", "WrongPassword@2026");

        for (int i = 0; i < 5; i++) {
            call(HttpMethod.POST, "/api/v1/auth/login", null, wrongLogin, 401);
        }

        JsonNode locked = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", LOCKED_USER, "password", PASSWORD), 403);

        assertThat(locked.path("code").asInt()).isEqualTo(1003);
    }

    @Test
    @DisplayName("被禁用的账号无法登录")
    void disabledAccountCannotLogin() throws Exception {
        createTestUser(DISABLED_USER, ROLE_ENGINEER, 0);

        JsonNode response = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", DISABLED_USER, "password", PASSWORD), 403);

        assertThat(response.path("code").asInt()).isEqualTo(1002);
    }

    @Test
    @DisplayName("登录参数校验失败时返回统一 1201 结构")
    void loginValidationErrorsUseUnifiedStructure() throws Exception {
        JsonNode response = call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", "  ", "password", "x"), 400);

        assertThat(response.path("code").asInt()).isEqualTo(1201);
        assertThat(response.path("message").asText()).contains("登录账号不能为空");
    }

    @Test
    @DisplayName("未登录访问受保护接口返回 401 与 1004")
    void unauthenticatedRequestIsRejected() throws Exception {
        JsonNode response = call(HttpMethod.GET, "/api/v1/users", null, null, 401);

        assertThat(response.path("code").asInt()).isEqualTo(1004);
    }

    @Test
    @DisplayName("伪造令牌无法通过鉴权")
    void forgedTokenIsRejected() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalid-signature";

        JsonNode response = call(HttpMethod.GET, "/api/v1/auth/me", forged, null, 401);

        assertThat(response.path("code").asInt()).isEqualTo(1004);
    }

    @Test
    @DisplayName("客户角色访问用户管理接口返回 403 与 1101")
    void customerCannotAccessUserManagement() throws Exception {
        String token = login("customer01", PASSWORD);

        JsonNode response = call(HttpMethod.GET, "/api/v1/users", token, null, 403);

        assertThat(response.path("code").asInt()).isEqualTo(1101);
    }

    @Test
    @DisplayName("工程师可读故障分类，但不可访问系统配置与用户管理")
    void engineerRoleBoundaries() throws Exception {
        String token = login("engineer01", PASSWORD);

        JsonNode categories = call(HttpMethod.GET, "/api/v1/fault-categories/enabled", token, null, 200);
        assertThat(categories.path("data").size()).isGreaterThan(0);

        assertThat(call(HttpMethod.GET, "/api/v1/configs", token, null, 403).path("code").asInt()).isEqualTo(1101);
        assertThat(call(HttpMethod.GET, "/api/v1/users", token, null, 403).path("code").asInt()).isEqualTo(1101);
    }

    @Test
    @DisplayName("管理员可访问用户管理与操作日志")
    void adminCanAccessSystemEndpoints() throws Exception {
        String token = login("admin", PASSWORD);

        assertThat(call(HttpMethod.GET, "/api/v1/users", token, null, 200).path("code").asInt()).isZero();
        assertThat(call(HttpMethod.GET, "/api/v1/permissions", token, null, 200).path("data").size())
                .isGreaterThan(0);
        assertThat(call(HttpMethod.GET, "/api/v1/configs", token, null, 200).path("data").size())
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("退出登录后原令牌立即失效")
    void logoutInvalidatesAccessToken() throws Exception {
        String token = login("service01", PASSWORD);
        assertThat(call(HttpMethod.GET, "/api/v1/auth/me", token, null, 200).path("code").asInt()).isZero();

        call(HttpMethod.POST, "/api/v1/auth/logout", token, null, 200);

        assertThat(call(HttpMethod.GET, "/api/v1/auth/me", token, null, 401).path("code").asInt()).isEqualTo(1004);
    }

    @Test
    @DisplayName("修改密码需校验原密码，修改后旧密码失效")
    void changePasswordFlow() throws Exception {
        String username = PASSWORD_USER;
        createTestUser(username, ROLE_CUSTOMER, 1);
        String token = login(username, PASSWORD);

        JsonNode wrongOld = call(HttpMethod.PUT, "/api/v1/auth/password", token,
                Map.of("oldPassword", "Wrong@2026", "newPassword", "NewPass2026"), 400);
        assertThat(wrongOld.path("message").asText()).contains("原密码不正确");

        assertThat(call(HttpMethod.PUT, "/api/v1/auth/password", token,
                Map.of("oldPassword", PASSWORD, "newPassword", "NewPass2026"), 200).path("code").asInt())
                .isZero();

        // 旧密码失效
        call(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", username, "password", PASSWORD), 401);
        // 新密码可登录
        String newToken = login(username, "NewPass2026");
        assertThat(newToken).isNotBlank();
    }

    @Test
    @DisplayName("刷新令牌可换取新的访问令牌，且旧 refresh token 不能重复使用")
    void refreshTokenRotation() throws Exception {
        JsonNode loginResponse = body(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", PASSWORD))))
                .andReturn());
        String refreshToken = loginResponse.path("data").path("refreshToken").asText();

        JsonNode refreshed = call(HttpMethod.POST, "/api/v1/auth/refresh", null,
                Map.of("refreshToken", refreshToken), 200);
        assertThat(refreshed.path("data").path("accessToken").asText()).isNotBlank();

        call(HttpMethod.POST, "/api/v1/auth/refresh", null, Map.of("refreshToken", refreshToken), 401);
    }
}
