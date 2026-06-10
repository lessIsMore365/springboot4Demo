package org.example.redis.core;

import java.time.Duration;

/**
 * Redis key 命名空间枚举 — 统一管理所有 key 前缀及默认 TTL。
 * 所有 Redis key 通过 {@link RedisKeyGenerator} 生成，禁止硬编码前缀。
 */
public enum RedisKeyNamespace {

    OAUTH2_AUTHORIZATION("oauth2:authorization", Duration.ofHours(1), "OAuth2 授权对象"),
    OAUTH2_ACCESS_TOKEN("oauth2:access_token", Duration.ofHours(1), "Access token → 授权ID 索引"),
    OAUTH2_REFRESH_TOKEN("oauth2:refresh_token", Duration.ofDays(30), "Refresh token → 授权ID 索引"),
    OAUTH2_AUTHORIZATION_CODE("oauth2:authorization_code", Duration.ofMinutes(5), "授权码 → 授权ID 索引"),
    OAUTH2_ID_TOKEN("oauth2:id_token", Duration.ofHours(1), "ID token → 授权ID 索引"),

    LOCK_PAYMENT("lock:payment", Duration.ofSeconds(30), "支付分布式锁"),

    IDEMPOTENT("idempotent", Duration.ofHours(24), "幂等性缓存"),

    RATE_LIMIT("ratelimit", Duration.ofSeconds(2), "限流滑动窗口"),

    CAPTCHA("captcha", Duration.ofMinutes(5), "验证码数据"),

    SYS_DICT("sys_dict", Duration.ofHours(24), "字典缓存"),

    CACHE_METRICS("cache:metrics", Duration.ofDays(7), "缓存命中/失效率统计"),

    INTERNAL_LOCK("lock:internal", Duration.ofSeconds(10), "内部操作锁");

    private final String prefix;
    private final Duration defaultTtl;
    private final String description;

    RedisKeyNamespace(String prefix, Duration defaultTtl, String description) {
        this.prefix = prefix;
        this.defaultTtl = defaultTtl;
        this.description = description;
    }

    public String prefix() { return prefix; }

    public Duration defaultTtl() { return defaultTtl; }

    public String description() { return description; }
}
