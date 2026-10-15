package com.serviceflow.vo;

import com.serviceflow.entity.SysOperationLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "操作日志")
public class OperationLogVO {

    private Long id;
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
    private Long durationMs;
    private String traceId;
    private LocalDateTime createTime;

    public static OperationLogVO from(SysOperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUri(log.getRequestUri());
        vo.setRequestParams(log.getRequestParams());
        vo.setResponseCode(log.getResponseCode());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setIp(log.getIp());
        vo.setDurationMs(log.getDurationMs());
        vo.setTraceId(log.getTraceId());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
