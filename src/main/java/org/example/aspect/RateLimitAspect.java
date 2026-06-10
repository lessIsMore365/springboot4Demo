package org.example.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.annotation.RateLimit;
import org.example.payment.exception.PaymentRateLimitException;
import org.example.redis.service.RedisRateLimiter;
import org.springframework.stereotype.Component;

/**
 * 限流切面 — 委托给 {@link RedisRateLimiter} 令牌桶算法。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        var result = rateLimiter.tryAcquire(rateLimit.key(), rateLimit.permitsPerSecond());
        if (!result.allowed()) {
            log.warn("接口限流触发 — key={}, 限制QPS={}", rateLimit.key(), rateLimit.permitsPerSecond());
            throw new PaymentRateLimitException("请求过于频繁，请稍后重试");
        }
        return pjp.proceed();
    }
}
