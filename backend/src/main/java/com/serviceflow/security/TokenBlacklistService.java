package com.serviceflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 令牌黑名单：退出登录或修改密码后，把令牌 ID 写入 Redis 直至其自然过期。
 *
 * <p>降级策略：Redis 不可用时记录告警并放行（返回 false），保证核心业务不中断；
 * 由于 access token 有效期较短，风险可控。</p>
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "sf:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将令牌加入黑名单，TTL 为令牌剩余有效期（已过期则不再写入）。
     */
    public void blacklist(String tokenId, Instant expiresAt) {
        if (tokenId == null || expiresAt == null) {
            return;
        }
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("写入令牌黑名单失败（Redis 不可用）：{}", ex.getMessage());
        }
    }

    /**
     * 判断令牌是否已失效。
     */
    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
        } catch (Exception ex) {
            log.warn("查询令牌黑名单失败（Redis 不可用），按未失效处理：{}", ex.getMessage());
            return false;
        }
    }
}
