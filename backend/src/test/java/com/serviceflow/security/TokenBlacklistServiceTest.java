package com.serviceflow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("注销令牌时按剩余有效期写入黑名单")
    void shouldWriteBlacklistEntryWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        service.blacklist("jti-1", Instant.now().plusSeconds(600));

        verify(valueOperations).set(eq("sf:token:blacklist:jti-1"), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("令牌已过期时不再写入黑名单")
    void shouldSkipExpiredToken() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        service.blacklist("jti-2", Instant.now().minusSeconds(10));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("黑名单命中时判定令牌已失效")
    void shouldDetectBlacklistedToken() {
        given(redisTemplate.hasKey("sf:token:blacklist:jti-3")).willReturn(true);
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        assertThat(service.isBlacklisted("jti-3")).isTrue();
    }

    @Test
    @DisplayName("Redis 不可用时降级：不抛异常且按未失效处理")
    void shouldDegradeWhenRedisUnavailable() {
        given(redisTemplate.hasKey("sf:token:blacklist:jti-4"))
                .willThrow(new IllegalStateException("redis down"));
        given(redisTemplate.opsForValue()).willThrow(new IllegalStateException("redis down"));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        assertThat(service.isBlacklisted("jti-4")).isFalse();
        assertThatCode(() -> service.blacklist("jti-4", Instant.now().plusSeconds(60)))
                .doesNotThrowAnyException();
    }
}
