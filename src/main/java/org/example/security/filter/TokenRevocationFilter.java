package org.example.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 撤销检查过滤器
 * JWT 是自包含的，资源服务器默认只验证签名不查 Redis。
 * 此过滤器在每次请求时检查 Redis 中的 access_token 索引是否存在，
 * 若已被强制下线删除，则拒绝请求。
 */
@Slf4j
public class TokenRevocationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_PREFIX = "oauth2:access_token:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StringRedisTemplate stringRedisTemplate;

    public TokenRevocationFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            String tokenKey = ACCESS_TOKEN_PREFIX + token;
            String authorizationId = stringRedisTemplate.opsForValue().get(tokenKey);
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
