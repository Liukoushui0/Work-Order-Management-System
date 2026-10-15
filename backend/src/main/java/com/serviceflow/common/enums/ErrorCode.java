package com.serviceflow.common.enums;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。区间划分见 docs/TECHNICAL_DESIGN.md 第 3.4 节。
 */
public enum ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),

    // 1000~1099 认证
    LOGIN_FAILED(1001, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(1002, "账号已被禁用", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(1003, "账号已被锁定，请稍后重试", HttpStatus.FORBIDDEN),
    TOKEN_INVALID(1004, "登录状态无效或已过期，请重新登录", HttpStatus.UNAUTHORIZED),

    // 1100~1199 鉴权
    ACCESS_DENIED(1101, "无权访问该资源", HttpStatus.FORBIDDEN),
    DATA_SCOPE_DENIED(1102, "无权访问该数据", HttpStatus.FORBIDDEN),

    // 1200~1299 参数与校验
    PARAM_INVALID(1201, "请求参数校验失败", HttpStatus.BAD_REQUEST),
    SORT_FIELD_INVALID(1202, "非法的排序字段", HttpStatus.BAD_REQUEST),
    REQUEST_BODY_INVALID(1203, "请求体格式错误", HttpStatus.BAD_REQUEST),

    // 1300~1399 客户与设备
    CUSTOMER_NOT_FOUND(1301, "客户不存在", HttpStatus.NOT_FOUND),
    DEVICE_NOT_FOUND(1302, "设备不存在", HttpStatus.NOT_FOUND),
    DEVICE_CUSTOMER_MISMATCH(1303, "设备不属于该客户", HttpStatus.BAD_REQUEST),

    // 1400~1499 工单
    TICKET_NOT_FOUND(1401, "工单不存在", HttpStatus.NOT_FOUND),
    TICKET_STATUS_INVALID(1402, "当前工单状态不允许该操作", HttpStatus.CONFLICT),
    TICKET_ALREADY_ACCEPTED(1403, "工单已被接单", HttpStatus.CONFLICT),
    TICKET_CANCELLED(1404, "工单已取消", HttpStatus.CONFLICT),
    TICKET_ACCESS_DENIED(1405, "无权操作该工单", HttpStatus.FORBIDDEN),

    // 1500~1599 评价与附件
    TICKET_NOT_COMPLETED(1501, "工单未完成，暂不可评价", HttpStatus.CONFLICT),
    TICKET_ALREADY_EVALUATED(1502, "该工单已评价", HttpStatus.CONFLICT),
    FILE_TYPE_NOT_ALLOWED(1503, "不支持的文件类型", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1504, "文件大小超出限制", HttpStatus.BAD_REQUEST),
    ATTACHMENT_NOT_FOUND(1505, "附件不存在", HttpStatus.NOT_FOUND),

    // 1600~1699 系统与缓存
    INTERNAL_ERROR(1601, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    CONCURRENT_CONFLICT(1602, "数据已被其他操作修改，请重试", HttpStatus.CONFLICT),
    DEPENDENCY_UNAVAILABLE(1603, "依赖服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE),
    NOT_FOUND(1604, "请求的资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(1605, "请求方法不被支持", HttpStatus.METHOD_NOT_ALLOWED);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
