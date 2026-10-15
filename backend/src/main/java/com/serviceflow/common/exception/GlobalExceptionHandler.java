package com.serviceflow.common.exception;

import com.serviceflow.common.api.Result;
import com.serviceflow.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转换为 {@link Result} 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：按错误码对应的 HTTP 状态返回，只记录 warn 级别摘要。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = ex.getMessage() == null ? errorCode.getMessage() : ex.getMessage();
        log.warn("业务异常 uri={} code={} message={}", request.getRequestURI(), errorCode.getCode(), message);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Result.failure(errorCode, message));
    }

    /**
     * Body 参数校验失败（@Valid）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 uri={} message={}", request.getRequestURI(), message);
        return buildResponse(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 表单/查询参数绑定校验失败。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 uri={} message={}", request.getRequestURI(), message);
        return buildResponse(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 方法参数级校验失败（@Validated + @RequestParam）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数约束失败 uri={} message={}", request.getRequestURI(), message);
        return buildResponse(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        String message = "缺少必要参数：" + ex.getParameterName();
        log.warn("缺少参数 uri={} message={}", request.getRequestURI(), message);
        return buildResponse(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest request) {
        String message = "参数类型错误：" + ex.getName();
        log.warn("参数类型错误 uri={} message={}", request.getRequestURI(), message);
        return buildResponse(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        log.warn("请求体解析失败 uri={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.REQUEST_BODY_INVALID, ErrorCode.REQUEST_BODY_INVALID.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        log.warn("请求方法不支持 uri={} method={}", request.getRequestURI(), ex.getMethod());
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException ex,
                                                              HttpServletRequest request) {
        log.warn("资源不存在 uri={}", request.getRequestURI());
        return buildResponse(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 兜底异常：记录完整堆栈，对外只返回统一错误信息，避免泄露内部实现。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("系统异常 uri={} method={}", request.getRequestURI(), request.getMethod(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private static ResponseEntity<Result<Void>> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Result.failure(errorCode, message));
    }

    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
