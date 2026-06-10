package org.example.redis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyGenerator;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisRateLimiter;
import org.example.redis.util.LuaScripts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

@Slf4j
public class RedisRateLimiterImpl implements RedisRateLimiter {

    private final StringRedisTemplate template;
    private final DefaultRedisScript<List> tokenBucketScript;

    @SuppressWarnings("unchecked")
    public RedisRateLimiterImpl(StringRedisTemplate template) {
        this.template = template;
        this.tokenBucketScript = new DefaultRedisScript<>(LuaScripts.RATE_LIMIT_TOKEN_BUCKET, List.class);
    }

    @Override
    public RateLimitResult tryAcquire(String resourceKey, int permitsPerSecond) {
        String key = RedisKeyGenerator.key(RedisKeyNamespace.RATE_LIMIT, resourceKey);
        long now = System.currentTimeMillis();

        List<Long> result = template.execute(tokenBucketScript, List.of(key),
                String.valueOf(permitsPerSecond),
                String.valueOf(now),
                String.valueOf(permitsPerSecond));

        if (result == null || result.size() < 3) {
            return new RateLimitResult(false, 0, now + 1000);
        }

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetAtMs = result.get(2);

        if (!allowed) {
            log.debug("限流触发: key={}, permitsPerSecond={}", resourceKey, permitsPerSecond);
        }

        return new RateLimitResult(allowed, remaining, resetAtMs);
    }

    @Override
    public RateLimitResult tryAcquire(String resourceKey, int permitsPerSecond, Duration timeout) {
        RateLimitResult result = tryAcquire(resourceKey, permitsPerSecond);
        if (result.allowed()) return result;

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        long sleepMs = Math.min(50, timeout.toMillis() / 10);

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new RateLimitResult(false, 0, 0);
            }
            result = tryAcquire(resourceKey, permitsPerSecond);
            if (result.allowed()) return result;
        }
        return result;
    }
}
