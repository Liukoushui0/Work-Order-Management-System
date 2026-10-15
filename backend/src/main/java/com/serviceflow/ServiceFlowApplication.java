package com.serviceflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ServiceFlow 企业售后工单管理系统启动类。
 */
@EnableScheduling
@SpringBootApplication
public class ServiceFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceFlowApplication.class, args);
    }
}
