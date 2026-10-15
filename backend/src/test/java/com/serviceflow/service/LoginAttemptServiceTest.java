package com.serviceflow.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private ConfigService configService;

    private LoginAttemptService loginAttemptService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(SysUser.class);
    }

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(sysUserMapper, configService);
    }

    private SysUser user(int failCount) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setLoginFailCount(failCount);
        user.setDeleted(0L);
        return user;
    }

    private LambdaUpdateWrapper<SysUser> captureUpdate() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(sysUserMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("未达阈值时仅累加失败次数，不锁定账号")
    void shouldOnlyIncrementFailCountBelowLimit() {
        given(sysUserMapper.selectById(1L)).willReturn(user(1));
        given(configService.getInt("security.login_fail_limit", 5)).willReturn(5);

        loginAttemptService.recordFailure(1L);

        String sqlSet = captureUpdate().getSqlSet();
        assertThat(sqlSet).contains("login_fail_count");
        assertThat(sqlSet).doesNotContain("lock_until");
    }

    @Test
    @DisplayName("达到阈值时锁定账号并把失败计数清零")
    void shouldLockAccountAtLimit() {
        given(sysUserMapper.selectById(1L)).willReturn(user(4));
        given(configService.getInt("security.login_fail_limit", 5)).willReturn(5);
        given(configService.getInt("security.login_lock_minutes", 15)).willReturn(15);

        loginAttemptService.recordFailure(1L);

        String sqlSet = captureUpdate().getSqlSet();
        assertThat(sqlSet).contains("login_fail_count").contains("lock_until");
    }

    @Test
    @DisplayName("登录成功后清零失败计数、解除锁定并记录登录信息")
    void shouldResetCountersOnSuccess() {
        loginAttemptService.recordSuccess(1L, "127.0.0.1");

        String sqlSet = captureUpdate().getSqlSet();
        assertThat(sqlSet)
                .contains("login_fail_count")
                .contains("lock_until")
                .contains("last_login_time")
                .contains("last_login_ip");
    }

    @Test
    @DisplayName("用户不存在时不写入任何更新")
    void shouldIgnoreUnknownUser() {
        given(sysUserMapper.selectById(99L)).willReturn(null);

        loginAttemptService.recordFailure(99L);

        verify(sysUserMapper, never()).update(any(), any());
    }
}
