package org.example.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解，标注在 Controller 方法上自动记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 操作模块/标题 */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 操作人类别 */
    OperatorType operatorType() default OperatorType.MANAGE;

    /** 是否保存请求参数 */
    boolean saveRequestData() default true;

    /** 是否保存响应结果 */
    boolean saveResponseData() default true;

    enum BusinessType {
        INSERT, UPDATE, DELETE, GRANT, EXPORT, IMPORT, OTHER
    }

    enum OperatorType {
        MANAGE, MOBILE
    }
}
