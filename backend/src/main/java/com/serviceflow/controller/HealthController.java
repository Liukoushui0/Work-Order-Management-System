package com.serviceflow.controller;

import com.serviceflow.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，用于本地开发阶段确认应用、数据库与 Redis 的连通性。
 */
@Tag(name = "系统健康检查")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @Value("${spring.application.name:serviceflow}")
    private String applicationName;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Operation(summary = "应用健康检查", description = "返回应用基本信息，并实际探测数据库与 Redis 连通性")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", applicationName);
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().format(FORMATTER));
        data.put("database", checkDatabase());
        data.put("redis", checkRedis());
        return Result.success(data);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(3) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("database health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }

    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("redis health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}
