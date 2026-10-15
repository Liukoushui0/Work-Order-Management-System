package com.serviceflow.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceflow.common.api.Result;
import com.serviceflow.common.util.RequestUtils;
import com.serviceflow.entity.SysOperationLog;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 操作审计切面：记录成功与失败的关键操作，写入 sys_operation_log。
 *
 * <p>参数中的 password、token、secret 等字段会被脱敏，日志内容截断到 2000 字符。</p>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_PARAM_LENGTH = 2000;
    private static final List<String> SENSITIVE_KEYWORDS = List.of("password", "token", "secret", "credential");

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogService operationLogService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operationLogRecord)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogRecord operationLogRecord) throws Throwable {
        long start = System.currentTimeMillis();
        String errorMsg = null;
        Integer responseCode = null;
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Result<?> apiResult) {
                responseCode = apiResult.getCode();
            }
            return result;
        } catch (Throwable ex) {
            errorMsg = ex.getMessage();
            responseCode = ex instanceof com.serviceflow.common.exception.BusinessException businessException
                    ? businessException.getErrorCode().getCode() : null;
            throw ex;
        } finally {
            try {
                saveLog(operationLogRecord, joinPoint.getArgs(), responseCode, errorMsg,
                        System.currentTimeMillis() - start);
            } catch (Exception ex) {
                // 审计日志失败不能影响业务主流程
                log.warn("写入操作日志失败：{}", ex.getMessage());
            }
        }
    }

    private void saveLog(OperationLogRecord annotation, Object[] args, Integer responseCode,
                         String errorMsg, long duration) {
        HttpServletRequest request = RequestUtils.currentRequest();
        SysOperationLog entity = new SysOperationLog();
        entity.setModule(annotation.module());
        entity.setOperation(annotation.operation());
        entity.setUserId(null);
        entity.setUsername(null);
        try {
            UserPrincipal principal = SecurityUtils.currentUser();
            entity.setUserId(principal.getUserId());
            entity.setUsername(principal.getUsername());
        } catch (Exception ignored) {
            // 匿名接口（如登录）没有主体，保持为空
        }
        if (request != null) {
            entity.setRequestMethod(request.getMethod());
            entity.setRequestUri(request.getRequestURI());
            entity.setIp(RequestUtils.clientIp(request));
            entity.setUserAgent(RequestUtils.userAgent(request));
        }
        entity.setRequestParams(annotation.saveParams() ? serializeParams(args) : null);
        entity.setResponseCode(responseCode);
        entity.setSuccess(errorMsg == null ? 1 : 0);
        entity.setErrorMsg(errorMsg == null ? null : truncate(errorMsg, 500));
        entity.setDurationMs(duration);
        entity.setTraceId(MDC.get("traceId"));
        operationLogService.save(entity);
    }

    private String serializeParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            StringBuilder builder = new StringBuilder();
            for (Object arg : args) {
                if (arg == null || isInfrastructureArg(arg)) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(';');
                }
                String json = objectMapper.writeValueAsString(arg);
                builder.append(maskSensitive(json));
            }
            return builder.length() == 0 ? null : truncate(builder.toString(), MAX_PARAM_LENGTH);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isInfrastructureArg(Object arg) {
        String name = arg.getClass().getName();
        return name.startsWith("jakarta.servlet") || name.startsWith("org.springframework.");
    }

    /**
     * 将敏感字段的值替换为 ***，避免密码、令牌进入审计表。
     */
    private String maskSensitive(String json) {
        String masked = json;
        for (String keyword : SENSITIVE_KEYWORDS) {
            masked = masked.replaceAll("(?i)(\"" + keyword + "[^\"]*\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        }
        return masked;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
