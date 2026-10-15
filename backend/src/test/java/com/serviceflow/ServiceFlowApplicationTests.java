package com.serviceflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文加载测试：验证 Spring 容器（含数据源、MyBatis-Plus、Redis 客户端）可以正常启动。
 */
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class ServiceFlowApplicationTests {

    @Test
    void contextLoads() {
        // 容器成功启动即视为通过
    }
}
