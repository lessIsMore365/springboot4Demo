package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyGenerator;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisOps;
import org.example.service.OnlineUserService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private final RedisOps redisOps;
    private final ObjectMapper objectMapper;

    public OnlineUserServiceImpl(RedisOps redisOps) {
        this.redisOps = redisOps;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> getOnlineUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenUsers = new HashSet<>();

        // 使用 SCAN 替代 KEYS（非阻塞），遍历所有 access_token 索引
        Set<String> tokenKeys = redisOps.scanToSet(RedisKeyGenerator.pattern(RedisKeyNamespace.OAUTH2_ACCESS_TOKEN));
        if (tokenKeys.isEmpty()) return result;

        for (String tokenKey : tokenKeys) {
            try {
                String authorizationId = redisOps.get(tokenKey);
                if (authorizationId == null) continue;

                String authKey = RedisKeyGenerator.key(RedisKeyNamespace.OAUTH2_AUTHORIZATION, authorizationId);
                String authJson = redisOps.get(authKey);
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
                    if (remaining <= 0) continue;
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
        String authKey = RedisKeyGenerator.key(RedisKeyNamespace.OAUTH2_AUTHORIZATION, authorizationId);
        String authJson = redisOps.get(authKey);
        if (authJson != null) {
            try {
                JsonNode root = objectMapper.readTree(authJson);
                String accessTokenValue = root.path("accessToken").path("token").path("tokenValue").asText("");
                if (!accessTokenValue.isEmpty()) {
                    redisOps.delete(RedisKeyGenerator.key(RedisKeyNamespace.OAUTH2_ACCESS_TOKEN, accessTokenValue));
                }
                String refreshTokenValue = root.path("refreshToken").path("token").path("tokenValue").asText("");
                if (!refreshTokenValue.isEmpty()) {
                    redisOps.delete(RedisKeyGenerator.key(RedisKeyNamespace.OAUTH2_REFRESH_TOKEN, refreshTokenValue));
                }
            } catch (Exception e) {
                log.warn("解析授权信息失败: {}", e.getMessage());
            }
        }
        redisOps.delete(authKey);
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
