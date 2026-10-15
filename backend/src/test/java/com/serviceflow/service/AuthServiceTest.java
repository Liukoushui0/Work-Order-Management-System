package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.auth.LoginRequest;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.security.CustomUserDetailsService;
import com.serviceflow.security.JwtTokenProvider;
import com.serviceflow.security.TokenBlacklistService;
import com.serviceflow.security.TokenDetails;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.support.MybatisTestSupport;
import com.serviceflow.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthService authService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(SysUser.class);
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(sysUserMapper, passwordEncoder, tokenProvider,
                blacklistService, userDetailsService, loginAttemptService);
    }

    private SysUser user(int status, int failCount, LocalDateTime lockUntil) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setPassword("$2a$10$hash");
        user.setRealName("测试用户");
        user.setStatus(status);
        user.setLoginFailCount(failCount);
        user.setLockUntil(lockUntil);
        user.setDeleted(0L);
        return user;
    }

    private LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setUsername("tester");
        request.setPassword("Serviceflow@2026");
        return request;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    @Test
    @DisplayName("密码正确时签发令牌并重置失败计数")
    void shouldLoginSuccessfully() {
        SysUser entity = user(1, 2, null);
        UserPrincipal principal = new UserPrincipal(1L, "tester", "hash", "测试用户", null, true,
                Set.of("ROLE_ADMIN"), Set.of("user:view"));
        given(sysUserMapper.selectOne(any())).willReturn(entity);
        given(passwordEncoder.matches("Serviceflow@2026", entity.getPassword())).willReturn(true);
        given(userDetailsService.loadByUserId(1L)).willReturn(principal);
        given(tokenProvider.createAccessToken(principal)).willReturn("access-token");
        given(tokenProvider.createRefreshToken(principal)).willReturn("refresh-token");
        given(tokenProvider.getAccessTokenSeconds()).willReturn(7200L);
        given(sysUserMapper.selectById(1L)).willReturn(entity);

        LoginVO vo = authService.login(request(), null);

        assertThat(vo.getAccessToken()).isEqualTo("access-token");
        assertThat(vo.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(vo.getExpiresIn()).isEqualTo(7200L);
        assertThat(vo.getUser().getRoles()).containsExactly("ROLE_ADMIN");
        verify(loginAttemptService).recordSuccess(eq(1L), any());
    }

    @Test
    @DisplayName("密码错误时抛出 1001 并累计失败次数")
    void shouldRejectWrongPasswordAndCountFailure() {
        SysUser entity = user(1, 0, null);
        given(sysUserMapper.selectOne(any())).willReturn(entity);
        given(passwordEncoder.matches(any(), any())).willReturn(false);
        assertThatThrownBy(() -> authService.login(request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(AuthServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.LOGIN_FAILED);

        verify(loginAttemptService).recordFailure(1L);
        verify(userDetailsService, never()).loadByUserId(any());
    }

    @Test
    @DisplayName("连续失败达到阈值时锁定账号")
    void shouldLockAccountWhenFailLimitReached() {
        SysUser entity = user(1, 4, null);
        given(sysUserMapper.selectOne(any())).willReturn(entity);
        given(passwordEncoder.matches(any(), any())).willReturn(false);

        assertThatThrownBy(() -> authService.login(request(), null))
                .isInstanceOf(BusinessException.class);

        verify(loginAttemptService, times(1)).recordFailure(1L);
    }

    @Test
    @DisplayName("账号处于锁定期时直接拒绝登录，不校验密码")
    void shouldRejectLockedAccount() {
        given(sysUserMapper.selectOne(any())).willReturn(user(1, 0, LocalDateTime.now().plusMinutes(10)));

        assertThatThrownBy(() -> authService.login(request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(AuthServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("账号被禁用时拒绝登录")
    void shouldRejectDisabledAccount() {
        given(sysUserMapper.selectOne(any())).willReturn(user(0, 0, null));

        assertThatThrownBy(() -> authService.login(request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(AuthServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("账号不存在时统一返回 1001，避免暴露账号存在性")
    void shouldReturnGenericErrorWhenUserNotFound() {
        given(sysUserMapper.selectOne(any())).willReturn(null);

        assertThatThrownBy(() -> authService.login(request(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(AuthServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("刷新令牌无效或已注销时抛出 1004")
    void shouldRejectInvalidRefreshToken() {
        given(tokenProvider.parse("bad-token")).willReturn(null);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(AuthServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("退出登录时把当前令牌写入黑名单")
    void shouldBlacklistTokenOnLogout() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        authService.logout(new TokenDetails("jti-9", expiresAt));

        verify(blacklistService).blacklist("jti-9", expiresAt);
    }
}
