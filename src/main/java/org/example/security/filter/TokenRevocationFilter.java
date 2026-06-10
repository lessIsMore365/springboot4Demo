package org.example.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyGenerator;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisOps;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 撤销检查过滤器 — 通过 {@link RedisOps} 检查 token 是否已被强制下线。
 */
@Slf4j
public class TokenRevocationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RedisOps redisOps;

    public TokenRevocationFilter(RedisOps redisOps) {
        this.redisOps = redisOps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            String tokenKey = RedisKeyGenerator.key(RedisKeyNamespace.OAUTH2_ACCESS_TOKEN, token);
            String authorizationId = redisOps.get(tokenKey);
            if (authorizationId == null) {
                log.warn("Token 已被撤销或过期 — {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"token_revoked\",\"message\":\"Token 已被强制下线或过期\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
