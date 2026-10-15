package com.serviceflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 统计结果缓存。
 *
 * 设计要点：
 * 1. 键命名 sf:{env}:{module}:{name}[:{scope}]，其中 scope 区分全局(all)与按用户(u{userId})，
 *    避免把"某工程师的个人统计"当全局数据返回给其他人。
 * 2. 只缓存统计聚合结果与系统配置等高频读取、计算代价高的数据，不做全量查询缓存。
 * 3. 写操作（建单、状态流转、评价、配置修改）后精确删除相关键，TTL 作为兜底。
 * 4. Redis 不可用时全部降级为直接查库，仅记录 WARN，不影响业务。
 */
@Service
public class StatisticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsCacheService.class);
    public static final String SCOPE_ALL = "all";

    /** TTL：总览变化最频繁，满意度变化最慢。 */
    private static final Duration TTL_OVERVIEW = Duration.ofSeconds(60);
    private static final Duration TTL_STATISTICS = Duration.ofSeconds(300);
    private static final Duration TTL_SATISFACTION = Duration.ofSeconds(600);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public StatisticsCacheService(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${spring.profiles.active:dev}") String profile) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = "sf:" + profile + ":";
    }

    public String overviewKey(String scope) {
        return keyPrefix + "dashboard:overview:" + scope;
    }

    public String trendKey(String scope, String granularity) {
        return keyPrefix + "stats:trend:" + scope + ":" + granularity;
    }

    public String faultDistributionKey(String scope) {
        return keyPrefix + "stats:fault-distribution:" + scope;
    }

    public String priorityDistributionKey(String scope) {
        return keyPrefix + "stats:priority-distribution:" + scope;
    }

    public String engineerWorkloadKey(String scope) {
        return keyPrefix + "stats:engineer-workload:" + scope;
    }

    public String satisfactionKey(String scope) {
        return keyPrefix + "stats:satisfaction:" + scope;
    }

    /**
     * 读取缓存；命中返回 Optional，未命中或 Redis 异常返回空。
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception ex) {
            log.warn("读取统计缓存失败（按未命中处理）：key={} error={}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 写入缓存，失败只记录日志。
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("写入统计缓存失败：key={} error={}", key, ex.getMessage());
        }
    }

    public Duration overviewTtl() {
        return TTL_OVERVIEW;
    }

    public Duration statisticsTtl() {
        return TTL_STATISTICS;
    }

    public Duration satisfactionTtl() {
        return TTL_SATISFACTION;
    }

    /**
     * 工单或评价发生写操作后清空统计缓存（键空间很小，使用 KEYS 精确清理；
     * 生产环境可替换为 SCAN 或维护键索引集合）。
     */
    public void evictStatistics() {
        evictByPattern(keyPrefix + "dashboard:*");
        evictByPattern(keyPrefix + "stats:*");
    }

    /**
     * 事务提交后再清理缓存：避免"缓存已删、事务尚未提交"期间被并发读请求用旧数据回填。
     * 非事务场景（如定时任务、单元测试）直接清理。
     */
    public void evictStatisticsAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictStatistics();
                }
            });
        } else {
            evictStatistics();
        }
    }

    /**
     * 只清理与配置相关的缓存（配置变更后调用）。
     */
    public void evictConfig() {
        evictByPattern(keyPrefix + "config:*");
    }

    private void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("清理统计缓存 {} 个：{}", keys.size(), pattern);
            }
        } catch (Exception ex) {
            log.warn("清理统计缓存失败：pattern={} error={}", pattern, ex.getMessage());
        }
    }
}
