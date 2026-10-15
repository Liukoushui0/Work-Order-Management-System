package com.serviceflow.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    private JwtTokenProvider provider(long accessMinutes) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenMinutes(accessMinutes);
        properties.setRefreshTokenDays(7);
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);
        tokenProvider.init();
        return tokenProvider;
    }

    private UserPrincipal principal() {
        return new UserPrincipal(7L, "engineer01", "hash", "工程师一号", 100L, true,
                Set.of("ROLE_ENGINEER"), Set.of("ticket:view", "ticket:accept"));
    }

    @Test
    @DisplayName("签发的 access token 可被解析，且携带用户 ID 与用户名")
    void shouldIssueAndParseAccessToken() {
        JwtTokenProvider tokenProvider = provider(30);

        String token = tokenProvider.createAccessToken(principal());
        Claims claims = tokenProvider.parse(token);

        assertThat(claims).isNotNull();
        assertThat(tokenProvider.isAccessToken(claims)).isTrue();
        assertThat(tokenProvider.isRefreshToken(claims)).isFalse();
        assertThat(tokenProvider.getUsername(claims)).isEqualTo("engineer01");
        assertThat(tokenProvider.getUserId(claims)).isEqualTo(7L);
        assertThat(tokenProvider.getTokenId(claims)).isNotBlank();
    }

    @Test
    @DisplayName("access token 与 refresh token 类型互不混淆")
    void shouldDistinguishTokenTypes() {
        JwtTokenProvider tokenProvider = provider(30);

        Claims refreshClaims = tokenProvider.parse(tokenProvider.createRefreshToken(principal()));

        assertThat(tokenProvider.isRefreshToken(refreshClaims)).isTrue();
        assertThat(tokenProvider.isAccessToken(refreshClaims)).isFalse();
    }

    @Test
    @DisplayName("被篡改的令牌解析失败")
    void shouldRejectTamperedToken() {
        JwtTokenProvider tokenProvider = provider(30);
        String token = tokenProvider.createAccessToken(principal());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(tokenProvider.parse(tampered)).isNull();
        assertThat(tokenProvider.parse("not-a-jwt")).isNull();
    }

    @Test
    @DisplayName("过期令牌解析失败")
    void shouldRejectExpiredToken() {
        JwtTokenProvider tokenProvider = provider(-1);

        String expired = tokenProvider.createAccessToken(principal());

        assertThat(tokenProvider.parse(expired)).isNull();
    }

    @Test
    @DisplayName("密钥不足 32 字节时拒绝启动，避免使用弱密钥")
    void shouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);

        assertThatThrownBy(tokenProvider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("至少 32 字节");
    }

    @Test
    @DisplayName("未配置密钥时自动生成随机密钥，保证本地开发可启动")
    void shouldGenerateRandomSecretWhenAbsent() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("  ");
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);

        tokenProvider.init();

        String token = tokenProvider.createAccessToken(principal());
        assertThat(tokenProvider.parse(token)).isNotNull();
    }
}
