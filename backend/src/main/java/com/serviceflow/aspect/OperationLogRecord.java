package com.serviceflow.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入操作审计日志的接口方法。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogRecord {

    /** 所属模块。 */
    String module();

    /** 操作描述。 */
    String operation();

    /** 是否记录请求参数（默认记录，参数中的敏感字段会被脱敏）。 */
    boolean saveParams() default true;
}
