package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录失败计数与账号锁定。
 *
 * <p>必须使用独立事务（REQUIRES_NEW）：登录失败时 AuthService 会抛出业务异常并回滚，
 * 若失败计数加入同一事务，计数与锁定将永远无法生效。</p>
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String CONFIG_FAIL_LIMIT = "security.login_fail_limit";
    private static final String CONFIG_LOCK_MINUTES = "security.login_lock_minutes";

    private final SysUserMapper sysUserMapper;
    private final ConfigService configService;

    public LoginAttemptService(SysUserMapper sysUserMapper, ConfigService configService) {
        this.sysUserMapper = sysUserMapper;
        this.configService = configService;
    }

    /**
     * 记录一次登录失败；达到阈值时锁定账号并清零计数。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int failLimit = Math.max(configService.getInt(CONFIG_FAIL_LIMIT, 5), 1);
        int lockMinutes = Math.max(configService.getInt(CONFIG_LOCK_MINUTES, 15), 1);
        int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        var update = Wrappers.<SysUser>lambdaUpdate().eq(SysUser::getId, userId);
        if (fails >= failLimit) {
            update.set(SysUser::getLoginFailCount, 0)
                    .set(SysUser::getLockUntil, LocalDateTime.now().plusMinutes(lockMinutes));
            log.warn("账号连续登录失败达到阈值，已锁定 userId={} lockMinutes={}", userId, lockMinutes);
        } else {
            update.set(SysUser::getLoginFailCount, fails);
        }
        sysUserMapper.update(null, update);
    }

    /**
     * 登录成功后清零失败计数与锁定状态，并记录登录信息。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordSuccess(Long userId, String ip) {
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, ip));
    }
}
