package com.serviceflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceflow.vo.dashboard.DashboardOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 统计缓存测试：命中/未命中、按范围隔离键名、以及 Redis 不可用时的降级。
 */
@ExtendWith(MockitoExtension.class)
class StatisticsCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private StatisticsCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new StatisticsCacheService(redisTemplate, new ObjectMapper(), "test");
    }

    @Test
    @DisplayName("缓存键包含环境前缀与数据范围，避免不同用户之间串数据")
    void cacheKeysAreScoped() {
        assertThat(cacheService.overviewKey("all")).isEqualTo("sf:test:dashboard:overview:all");
        assertThat(cacheService.overviewKey("u3")).isEqualTo("sf:test:dashboard:overview:u3");
        assertThat(cacheService.trendKey("all", "WEEK")).isEqualTo("sf:test:stats:trend:all:WEEK");
        assertThat(cacheService.satisfactionKey("u4")).isEqualTo("sf:test:stats:satisfaction:u4");
        assertThat(cacheService.overviewTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(cacheService.statisticsTtl()).isEqualTo(Duration.ofSeconds(300));
        assertThat(cacheService.satisfactionTtl()).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    @DisplayName("缓存命中时反序列化为原对象")
    void shouldReturnCachedValue() throws Exception {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setTotal(8L);
        vo.setCached(false);
        String json = new ObjectMapper().writeValueAsString(vo);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("sf:test:dashboard:overview:all")).willReturn(json);

        Optional<DashboardOverviewVO> cached = cacheService.get("sf:test:dashboard:overview:all",
                DashboardOverviewVO.class);

        assertThat(cached).isPresent();
        assertThat(cached.get().getTotal()).isEqualTo(8L);
    }

    @Test
    @DisplayName("未命中时返回空 Optional，写入时携带 TTL")
    void shouldMissAndWriteWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("sf:test:dashboard:overview:all")).willReturn(null);

        assertThat(cacheService.get("sf:test:dashboard:overview:all", DashboardOverviewVO.class)).isEmpty();

        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setTotal(1L);
        cacheService.put("sf:test:dashboard:overview:all", vo, Duration.ofSeconds(60));

        verify(valueOperations).set(eq("sf:test:dashboard:overview:all"), any(String.class),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Redis 不可用时读缓存按未命中处理，写缓存与清理都不抛异常")
    void shouldDegradeWhenRedisUnavailable() {
        given(redisTemplate.opsForValue()).willThrow(new IllegalStateException("redis down"));
        given(redisTemplate.keys(any())).willThrow(new IllegalStateException("redis down"));

        assertThat(cacheService.get("sf:test:any", DashboardOverviewVO.class)).isEmpty();
        assertThatCode(() -> cacheService.put("sf:test:any", new DashboardOverviewVO(), Duration.ofSeconds(60)))
                .doesNotThrowAnyException();
        assertThatCode(cacheService::evictStatistics).doesNotThrowAnyException();
        verify(redisTemplate, never()).delete(any(java.util.Collection.class));
    }
}
