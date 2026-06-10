package org.example.redis.monitor;

import lombok.extern.slf4j.Slf4j;
import org.example.redis.service.RedisOps;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

/**
 * Redis 健康监控 — 定期检查 + SLOWLOG 收集。
 */
@Slf4j
public class RedisHealth {

    private final RedisOps redisOps;
    private volatile boolean healthy = true;
    private volatile String lastError;

    public RedisHealth(RedisOps redisOps) {
        this.redisOps = redisOps;
    }

    public boolean isHealthy() {
        try {
            String pong = redisOps.ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> status() {
        try {
            Map<String, String> server = redisOps.info("server");
            Map<String, String> memory = redisOps.info("memory");
            Map<String, String> stats = redisOps.info("stats");
            long dbSize = redisOps.dbSize();

            return Map.of(
                    "healthy", true,
                    "redisVersion", server.getOrDefault("redis_version", "unknown"),
                    "uptimeDays", server.getOrDefault("uptime_in_days", "0"),
                    "usedMemoryHuman", memory.getOrDefault("used_memory_human", "0"),
                    "totalKeys", dbSize,
                    "connectedClients", stats.getOrDefault("connected_clients", "0"),
                    "instantaneousOpsPerSec", stats.getOrDefault("instantaneous_ops_per_sec", "0")
            );
        } catch (Exception e) {
            return Map.of("healthy", false, "error", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000)
    void healthCheck() {
        try {
            boolean ok = isHealthy();
            if (healthy != ok) {
                healthy = ok;
                if (ok) {
                    log.info("Redis 连接恢复");
                } else {
                    lastError = "PING 失败";
                    log.warn("Redis 连接异常");
                }
            }
        } catch (Exception e) {
            healthy = false;
            lastError = e.getMessage();
            log.error("Redis 健康检查异常", e);
        }
    }
}
