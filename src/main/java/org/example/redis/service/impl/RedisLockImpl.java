package org.example.redis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyGenerator;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisLock;
import org.example.redis.util.LuaScripts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RedisLockImpl implements RedisLock {

    private final StringRedisTemplate template;
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    private final DefaultRedisScript<Long> acquireScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final DefaultRedisScript<Long> extendScript;

    public RedisLockImpl(StringRedisTemplate template) {
        this.template = template;
        this.acquireScript = new DefaultRedisScript<>(LuaScripts.LOCK_ACQUIRE, Long.class);
        this.releaseScript = new DefaultRedisScript<>(LuaScripts.LOCK_RELEASE, Long.class);
        this.extendScript = new DefaultRedisScript<>(LuaScripts.LOCK_EXTEND, Long.class);
    }

    @Override
    public LockToken tryLock(RedisKeyNamespace ns, String resourceKey, Duration ttl) {
        String lockKey = RedisKeyGenerator.key(ns, resourceKey);
        String owner = Thread.currentThread().threadId() + ":" + instanceId;
        long ttlMs = ttl.toMillis();

        Long result = template.execute(acquireScript, List.of(lockKey), owner, String.valueOf(ttlMs));
        if (result != null && result == 1) {
            log.debug("获取锁成功: key={}, owner={}", lockKey, owner);
            return new LockToken(lockKey, owner);
        }
        log.debug("获取锁失败（被占用）: key={}", lockKey);
        return null;
    }

    @Override
    public boolean unlock(LockToken token) {
        if (token == null) return false;
        Long result = template.execute(releaseScript, List.of(token.lockKey()), token.owner());
        boolean released = result != null && result >= 1;
        if (released) {
            log.debug("释放锁成功: key={}", token.lockKey());
        }
        return released;
    }

    @Override
    public boolean extendLease(LockToken token, Duration additional) {
        if (token == null) return false;
        Long result = template.execute(extendScript, List.of(token.lockKey()),
                token.owner(), String.valueOf(additional.toMillis()));
        boolean extended = result != null && result == 1;
        if (extended) {
            log.debug("锁续期成功: key={}", token.lockKey());
        }
        return extended;
    }
}
