package com.serviceflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 轻量分布式锁，用于保护并发接单这类同一时刻只允许一个成功的操作。
 *
 * 降级策略：Redis 不可用时 tryLock 返回 true 放行，正确性由数据库条件更新与唯一约束兜底，
 * 保证缓存不可用不会导致核心业务中断。
 */
@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);
    private static final String KEY_PREFIX = "sf:lock:";

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试加锁。
     *
     * @param key 业务键，方法内部自动加 sf:lock: 前缀
     * @param ttl 锁自动过期时间，避免异常情况下死锁
     * @return true 表示加锁成功
     */
    public boolean tryLock(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, "1", ttl));
        } catch (Exception ex) {
            log.warn("获取分布式锁失败（Redis 不可用），降级为数据库条件更新：key={} error={}", key, ex.getMessage());
            return true;
        }
    }

    /**
     * 释放锁。
     */
    public void unlock(String key) {
        try {
            redisTemplate.delete(KEY_PREFIX + key);
        } catch (Exception ex) {
            log.warn("释放分布式锁失败：key={} error={}", key, ex.getMessage());
        }
    }
}
