package org.example.redis.service;

import org.example.redis.core.RedisKeyNamespace;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁服务 — 可重入 Lua 实现。
 * 使用示例:
 * <pre>{@code
 *   LockToken token = redisLock.tryLock(RedisKeyNamespace.LOCK_PAYMENT, orderNo, Duration.ofSeconds(10));
 *   if (token != null) {
 *       try { ... } finally { redisLock.unlock(token); }
 *   }
 * }</pre>
 */
public interface RedisLock {

    /**
     * 尝试获取锁
     * @param ns 命名空间
     * @param resourceKey 资源标识（如订单号）
     * @param ttl 锁超时时间
     * @return 锁令牌，获取失败返回 null
     */
    LockToken tryLock(RedisKeyNamespace ns, String resourceKey, Duration ttl);

    /**
     * 释放锁
     * @return true 表示完全释放，false 表示非持有者或锁已过期
     */
    boolean unlock(LockToken token);

    /**
     * 续期锁
     * @return true 表示续期成功
     */
    boolean extendLease(LockToken token, Duration additional);

    /**
     * 获取锁并执行，自动释放
     * @return 操作结果；获取锁失败返回 null
     */
    default <T> T executeWithLock(RedisKeyNamespace ns, String resourceKey, Duration ttl, Supplier<T> action) {
        LockToken token = tryLock(ns, resourceKey, ttl);
        if (token == null) return null;
        try {
            return action.get();
        } finally {
            unlock(token);
        }
    }

    /** 锁令牌 */
    record LockToken(String lockKey, String owner) {}
}
