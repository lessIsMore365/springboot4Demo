package org.example.payment.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisLock;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 分布式锁（兼容旧 API）— 委托给 {@link RedisLock}。
 * @deprecated 推荐直接注入 {@link RedisLock}
 */
@Slf4j
@Component
@Deprecated
public class DistributedLock {

    private final RedisLock redisLock;
    /** token → LockToken 映射（兼容旧 String token API） */
    private final Map<String, RedisLock.LockToken> tokenMap = new ConcurrentHashMap<>();

    public DistributedLock(RedisLock redisLock) {
        this.redisLock = redisLock;
    }

    public String tryLock(String lockKey, int ttlSeconds) {
        RedisLock.LockToken token = redisLock.tryLock(RedisKeyNamespace.LOCK_PAYMENT, lockKey,
                Duration.ofSeconds(ttlSeconds));
        if (token == null) return null;
        String oldToken = lockKey + ":" + token.owner();
        tokenMap.put(oldToken, token);
        return oldToken;
    }

    public void unlock(String lockKey, String oldToken) {
        if (oldToken == null) return;
        RedisLock.LockToken token = tokenMap.remove(oldToken);
        if (token != null) {
            redisLock.unlock(token);
        }
    }

    public <T> T executeWithLock(String lockKey, int ttlSeconds, java.util.function.Supplier<T> action) {
        RedisLock.LockToken token = redisLock.tryLock(RedisKeyNamespace.LOCK_PAYMENT, lockKey,
                Duration.ofSeconds(ttlSeconds));
        if (token == null) return null;
        try {
            return action.get();
        } finally {
            redisLock.unlock(token);
        }
    }
}
