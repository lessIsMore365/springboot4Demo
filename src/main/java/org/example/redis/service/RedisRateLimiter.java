package org.example.redis.service;

import java.time.Duration;

/**
 * 限流服务 — 从切面中抽出的独立限流能力，供任意调用方复用。
 * 默认使用令牌桶算法（Lua 原子实现），后续可切换为滑动窗口。
 */
public interface RedisRateLimiter {

    /**
     * 尝试获取执行许可
     * @param resourceKey 限流资源标识
     * @param permitsPerSecond 每秒允许的请求数
     * @return 限流结果
     */
    RateLimitResult tryAcquire(String resourceKey, int permitsPerSecond);

    /**
     * 带超时的限流（等待最多 timeout 时间）
     */
    RateLimitResult tryAcquire(String resourceKey, int permitsPerSecond, Duration timeout);

    /** 限流结果 */
    record RateLimitResult(boolean allowed, long remaining, long resetAtMs) {}
}
