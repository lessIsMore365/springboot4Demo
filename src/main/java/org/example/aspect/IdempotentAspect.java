package org.example.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.annotation.Idempotent;
import org.example.payment.exception.PaymentIdempotentException;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisCache;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 幂等性切面 — 委托给 {@link RedisCache} 实现 SET NX EX 语义。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisCache redisCache;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return pjp.proceed();
        }

        String redisKey = idempotent.prefix() + ":" + idempotencyKey;
        var cached = redisCache.getString(RedisKeyNamespace.IDEMPOTENT, redisKey);

        if (cached.isPresent() && !"processing".equals(cached.get())) {
            log.info("幂等请求，返回缓存结果 — key={}", redisKey);
            return objectMapper.readValue(cached.get(), Object.class);
        }

        Duration ttl = Duration.ofSeconds(idempotent.ttlSeconds());
        boolean locked = redisCache.setIfAbsent(RedisKeyNamespace.IDEMPOTENT, redisKey, "processing", ttl);

        if (!locked) {
            Thread.sleep(200);
            cached = redisCache.getString(RedisKeyNamespace.IDEMPOTENT, redisKey);
            if (cached.isPresent() && !"processing".equals(cached.get())) {
                return objectMapper.readValue(cached.get(), Object.class);
            }
            throw new PaymentIdempotentException("请求处理中，请稍后重试");
        }

        try {
            Object result = pjp.proceed();
            String resultJson = objectMapper.writeValueAsString(result);
            redisCache.put(RedisKeyNamespace.IDEMPOTENT, redisKey, resultJson, ttl);
            log.debug("幂等结果已缓存 — key={}", redisKey);
            return result;
        } catch (Exception e) {
            redisCache.evict(RedisKeyNamespace.IDEMPOTENT, redisKey);
            throw e;
        }
    }
}
