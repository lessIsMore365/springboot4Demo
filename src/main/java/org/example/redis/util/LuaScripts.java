package org.example.redis.util;

/**
 * 统一的 Lua 脚本常量。
 * 所有脚本都经过原子性验证，适合 Redis 单节点和集群（hash tag 保证同 slot）。
 */
public final class LuaScripts {

    private LuaScripts() {}

    /**
     * 可重入分布式锁 — 加锁
     * KEYS[1] = lock key
     * ARGV[1] = owner token (threadId + instanceId)
     * ARGV[2] = lock TTL millis
     * 返回: 1=加锁成功, 0=被他人持有
     */
    public static final String LOCK_ACQUIRE = """
            local owner = redis.call('HGET', KEYS[1], 'owner')
            if owner == ARGV[1] then
                redis.call('HINCRBY', KEYS[1], 'count', 1)
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
                return 1
            end
            local acquired = redis.call('SET', KEYS[1], '1', 'NX', 'PX', ARGV[2])
            if acquired then
                redis.call('DEL', KEYS[1])
                redis.call('HSET', KEYS[1], 'owner', ARGV[1], 'count', 1)
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
                return 1
            end
            return 0
            """;

    /**
     * 可重入分布式锁 — 释放
     * KEYS[1] = lock key
     * ARGV[1] = owner token
     * 返回: 1=完全释放, 0=非锁持有者或锁不存在, -N=还剩 N 次重入
     */
    public static final String LOCK_RELEASE = """
            local owner = redis.call('HGET', KEYS[1], 'owner')
            if owner ~= ARGV[1] then
                return 0
            end
            local count = redis.call('HINCRBY', KEYS[1], 'count', -1)
            if count <= 0 then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return -count
            """;

    /**
     * 分布式锁 — 续期
     * KEYS[1] = lock key
     * ARGV[1] = owner token
     * ARGV[2] = extend millis
     * 返回: 1=续期成功, 0=非锁持有者
     */
    public static final String LOCK_EXTEND = """
            local owner = redis.call('HGET', KEYS[1], 'owner')
            if owner == ARGV[1] then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
                return 1
            end
            return 0
            """;

    /**
     * 令牌桶限流
     * KEYS[1] = rate limit key
     * ARGV[1] = permits per second
     * ARGV[2] = current time millis
     * ARGV[3] = max burst (same as permits for simplicity)
     * 返回: {allowed=1|0, remaining=N, resetAt=ms}
     */
    public static final String RATE_LIMIT_TOKEN_BUCKET = """
            local rate = tonumber(ARGV[1])
            local now = tonumber(ARGV[2])
            local burst = tonumber(ARGV[3])
            local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'lastRefill')
            local tokens = tonumber(bucket[1])
            local lastRefill = tonumber(bucket[2])
            if tokens == nil then tokens = burst end
            if lastRefill then
                local elapsed = (now - lastRefill) / 1000
                tokens = math.min(burst, tokens + elapsed * rate)
            end
            if tokens >= 1 then
                tokens = tokens - 1
                redis.call('HMSET', KEYS[1], 'tokens', tokens, 'lastRefill', now)
                redis.call('PEXPIRE', KEYS[1], 2000)
                return {1, math.floor(tokens), now + 1000}
            end
            redis.call('HMSET', KEYS[1], 'tokens', tokens, 'lastRefill', now)
            redis.call('PEXPIRE', KEYS[1], 2000)
            local resetAt = math.ceil(now + math.max(1000, (1 - tokens) / rate * 1000))
            return {0, 0, resetAt}
            """;

    /**
     * SET NX EX — 简单的 if-absent 写入（幂等性、占位符等）
     * KEYS[1] = key
     * ARGV[1] = value
     * ARGV[2] = TTL seconds
     * 返回: OK=设置成功, nil=key 已存在
     */
    public static final String SET_NX_EX = """
            return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2])
            """;
}
