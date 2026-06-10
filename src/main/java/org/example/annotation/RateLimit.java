package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解 — 基于 Redis 滑动窗口的 QPS 控制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流 key，用于区分接口 */
    String key();

    /** 每秒允许的请求数 */
    int permitsPerSecond() default 10;

    /** 获取许可的超时时间（秒），0 表示非阻塞 */
    int timeoutSeconds() default 1;
}
