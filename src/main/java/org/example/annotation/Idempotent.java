package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解 — 基于 Redis 的请求去重
 * 客户端在 Idempotency-Key 请求头中传入唯一键
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** Redis key 前缀，用于区分不同业务场景 */
    String prefix() default "idempotent";

    /** 幂等键有效期（秒），默认 24 小时 */
    long ttlSeconds() default 86400;
}
