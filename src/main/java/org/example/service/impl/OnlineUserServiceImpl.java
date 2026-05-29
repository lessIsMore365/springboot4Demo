package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.service.OnlineUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ACCESS_TOKEN_PREFIX = "oauth2:access_token:";
    private static final String AUTH_PREFIX = "oauth2:authorization:";

    public OnlineUserServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> getOnlineUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenUsers = new HashSet<>();

        // 扫描所有 access_token 索引
        Set<String> tokenKeys = stringRedisTemplate.keys(ACCESS_TOKEN_PREFIX + "*");
        if (tokenKeys == null || tokenKeys.isEmpty()) return result;

        for (String tokenKey : tokenKeys) {
            try {
                String authorizationId = stringRedisTemplate.opsForValue().get(tokenKey);
                if (authorizationId == null) continue;

                // 直接读取 authorization 的 JSON，避免 Spring Security 7 allowlist 问题
                String authJson = stringRedisTemplate.opsForValue().get(AUTH_PREFIX + authorizationId);
                if (authJson == null) continue;

                JsonNode root = objectMapper.readTree(authJson);

                String username = path(root, "principalName").asText("");
                if (username.isEmpty() || !seenUsers.add(username)) continue;

                JsonNode accessToken = root.path("accessToken").path("token");
                long issuedAtSec = accessToken.path("issuedAt").asLong(0);
                long expiresAtSec = accessToken.path("expiresAt").asLong(0);
                String tokenType = accessToken.path("tokenType").path("value").asText("Bearer");
                String clientId = path(root, "registeredClientId").asText("");
                String authId = path(root, "id").asText("");

                Instant expiresAt = expiresAtSec > 0 ? Instant.ofEpochSecond(expiresAtSec) : null;
                Instant issuedAt = issuedAtSec > 0 ? Instant.ofEpochSecond(issuedAtSec) : null;

                Map<String, Object> user = new LinkedHashMap<>();
                user.put("authorizationId", authId);
                user.put("username", username);
                user.put("loginTime", issuedAt != null
                        ? LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault())
                        : null);
                user.put("expireTime", expiresAt != null
                        ? LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
                        : null);
                user.put("tokenType", tokenType);
                user.put("registeredClientId", clientId);

                if (expiresAt != null) {
                    long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
                    if (remaining <= 0) continue; // 跳过已过期的
                    user.put("remainingSeconds", remaining);
                    user.put("remainingDisplay", formatRemaining(remaining));
                }

                result.add(user);
            } catch (Exception e) {
                log.warn("获取在线用户信息失败: {}", e.getMessage());
            }
        }

        result.sort((a, b) -> Long.compare(
                (Long) b.getOrDefault("remainingSeconds", 0L),
                (Long) a.getOrDefault("remainingSeconds", 0L)));
        return result;
    }

    private static JsonNode path(JsonNode node, String dotPath) {
        for (String part : dotPath.split("\\.")) {
            node = node.path(part);
        }
        return node;
    }

    @Override
    public void forceLogout(String authorizationId) {
        // 删除 authorization 和关联的 token 索引
        String authKey = AUTH_PREFIX + authorizationId;
        String authJson = stringRedisTemplate.opsForValue().get(authKey);
        if (authJson != null) {
            try {
                JsonNode root = objectMapper.readTree(authJson);
                // 删除 access_token 索引
                String accessTokenValue = root.path("accessToken").path("token").path("tokenValue").asText("");
                if (!accessTokenValue.isEmpty()) {
                    stringRedisTemplate.delete(ACCESS_TOKEN_PREFIX + accessTokenValue);
                }
                // 删除 refresh_token 索引
                String refreshTokenValue = root.path("refreshToken").path("token").path("tokenValue").asText("");
                if (!refreshTokenValue.isEmpty()) {
                    stringRedisTemplate.delete("oauth2:refresh_token:" + refreshTokenValue);
                }
            } catch (Exception e) {
                log.warn("解析授权信息失败: {}", e.getMessage());
            }
        }
        stringRedisTemplate.delete(authKey);
        log.info("已强制下线用户, authorizationId: {}", authorizationId);
    }

    private String formatRemaining(long seconds) {
        if (seconds <= 0) return "已过期";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "小时" + m + "分钟";
        if (m > 0) return m + "分钟" + s + "秒";
        return s + "秒";
    }
}
