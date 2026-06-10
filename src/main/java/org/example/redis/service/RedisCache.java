package org.example.redis.service;

import org.example.redis.core.RedisKeyNamespace;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存抽象层 — Cache-Aside 模式 + 类型安全 + 命中率统计。
 * 业务代码通过此接口操作缓存，而非直接使用 RedisTemplate。
 */
public interface RedisCache {

    /**
     * 读取缓存
     * @param ns 命名空间
     * @param key 业务 key（不含前缀）
     * @param type 目标类型
     * @return Optional 包装的值
     */
    <T> Optional<T> get(RedisKeyNamespace ns, String key, Class<T> type);

    /**
     * 读取缓存（字符串类型，无需反序列化）
     */
    Optional<String> getString(RedisKeyNamespace ns, String key);

    /**
     * 写入缓存
     * @param ns 命名空间
     * @param key 业务 key
     * @param value 值（会通过 ObjectMapper 序列化）
     * @param ttl 过期时间，null 则使用命名空间默认 TTL
     */
    <T> void put(RedisKeyNamespace ns, String key, T value, Duration ttl);

    default <T> void put(RedisKeyNamespace ns, String key, T value) {
        put(ns, key, value, ns.defaultTtl());
    }

    /**
     * Cache-Aside: 先从缓存读，miss 时回源 DB 并写入缓存
     * @param ns 命名空间
     * @param key 业务 key
     * @param type 目标类型
     * @param loader DB 加载函数
     * @param ttl 过期时间
     */
    <T> T getOrFetch(RedisKeyNamespace ns, String key, Class<T> type,
                     Supplier<T> loader, Duration ttl);

    default <T> T getOrFetch(RedisKeyNamespace ns, String key, Class<T> type, Supplier<T> loader) {
        return getOrFetch(ns, key, type, loader, ns.defaultTtl());
    }

    /** 缓存预热：批量写入 */
    <T> void putAll(RedisKeyNamespace ns, String key, List<T> values, Duration ttl);

    /** 读取列表缓存 */
    <T> List<T> getList(RedisKeyNamespace ns, String key, Class<T> elementType);

    /** 删除单条缓存 */
    boolean evict(RedisKeyNamespace ns, String key);

    /** 按前缀批量删除（SCAN 实现） */
    long evictByPrefix(RedisKeyNamespace ns, String prefix);

    /** SET NX，用于幂等性占位等场景。返回 true 表示获取成功 */
    boolean setIfAbsent(RedisKeyNamespace ns, String key, String value, Duration ttl);

    /** 获取缓存统计 */
    CacheStats stats();

    /** 缓存统计快照 */
    record CacheStats(long hits, long misses, double hitRate, long putCount, long evictCount) {}
}
