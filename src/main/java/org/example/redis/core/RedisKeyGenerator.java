package org.example.redis.core;

/**
 * 统一 key 生成器 — 所有 Redis key 必须通过此类生成。
 * 格式: {@code namespace:suffix1:suffix2:...}
 */
public final class RedisKeyGenerator {

    private RedisKeyGenerator() {}

    public static String key(RedisKeyNamespace ns, String... segments) {
        StringBuilder sb = new StringBuilder(ns.prefix());
        for (String s : segments) {
            sb.append(':').append(s);
        }
        return sb.toString();
    }

    /** 模式匹配 key，用于 SCAN 操作 */
    public static String pattern(RedisKeyNamespace ns) {
        return ns.prefix() + ":*";
    }

    /** 模式匹配 key（含子前缀） */
    public static String pattern(RedisKeyNamespace ns, String subPrefix) {
        return ns.prefix() + ":" + subPrefix + "*";
    }
}
