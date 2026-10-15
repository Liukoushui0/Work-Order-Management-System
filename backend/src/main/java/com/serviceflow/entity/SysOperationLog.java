package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志表 sys_operation_log。
 */
@Getter
@Setter
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String requestMethod;
    private String requestUri;
    private String requestParams;
    private Integer responseCode;
    private Integer success;
    private String errorMsg;
    private String ip;
    private String userAgent;
    private Long durationMs;
    private String traceId;
    private LocalDateTime createTime;
}
