package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.service.RedisCache;
import org.example.redis.service.RedisOps;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 测试控制器 — 使用新的 {@link RedisOps} + {@link RedisCache} 抽象层。
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisOps redisOps;
    private final RedisCache redisCache;

    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        String result = redisOps.ping();
        return Map.of("success", true, "message", "PING: " + result, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/test/async")
    public CompletableFuture<Map<String, Object>> testConnectionAsync() {
        return redisOps.pingAsync()
                .thenApply(result -> Map.of("success", true, "message", "PING: " + result,
                        "timestamp", System.currentTimeMillis()));
    }

    @PostMapping("/set")
    public Map<String, Object> setKeyValue(@RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        Object value = request.get("value");
        Long timeout = request.containsKey("timeout") ? ((Number) request.get("timeout")).longValue() : null;

        String strValue = value instanceof String ? (String) value : String.valueOf(value);
        if (timeout != null) {
            redisOps.set(key, strValue, Duration.ofSeconds(timeout));
        } else {
            redisOps.set(key, strValue);
        }
        return Map.of("success", true, "key", key, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/get/{key}")
    public Map<String, Object> getValue(@PathVariable String key) {
        String value = redisOps.get(key);
        return Map.of("success", true, "key", key, "value", value, "exists", value != null,
                "timestamp", System.currentTimeMillis());
    }

    @DeleteMapping("/delete/{key}")
    public Map<String, Object> deleteKey(@PathVariable String key) {
        boolean deleted = redisOps.delete(key);
        return Map.of("success", deleted, "key", key, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/exists/{key}")
    public Map<String, Object> existsKey(@PathVariable String key) {
        return Map.of("success", true, "key", key, "exists", redisOps.exists(key),
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/expire/{key}")
    public Map<String, Object> setExpire(@PathVariable String key, @RequestParam long timeout) {
        boolean ok = redisOps.expire(key, Duration.ofSeconds(timeout));
        return Map.of("success", ok, "key", key, "timeout", timeout, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/info")
    public Map<String, Object> getRedisInfo() {
        Map<String, String> server = redisOps.info("server");
        return Map.of("success", true, "info", server, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/stats")
    public Map<String, Object> getRedisStats() {
        return Map.of("success", true,
                "health", redisOps.ping(),
                "dbSize", redisOps.dbSize(),
                "memory", redisOps.info("memory"),
                "cache", redisCache.stats(),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/keys")
    public Map<String, Object> getKeys(@RequestParam(defaultValue = "*") String pattern) {
        var keys = redisOps.scanToSet(pattern);
        return Map.of("success", true, "pattern", pattern, "keys", keys, "count", keys.size(),
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/hash/{key}/{field}")
    public Map<String, Object> hashSet(@PathVariable String key, @PathVariable String field,
                                       @RequestBody Map<String, Object> body) {
        Object value = body.get("value");
        redisOps.set(key + ":" + field, String.valueOf(value));
        return Map.of("success", true, "key", key, "field", field, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/hash/{key}/{field}")
    public Map<String, Object> hashGet(@PathVariable String key, @PathVariable String field) {
        String value = redisOps.get(key + ":" + field);
        return Map.of("success", true, "key", key, "field", field, "value", value, "exists", value != null,
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/list/{key}/lpush")
    public Map<String, Object> listLeftPush(@PathVariable String key, @RequestBody Object value) {
        String v = String.valueOf(value);
        redisOps.set(key + ":list:" + System.nanoTime(), v);
        return Map.of("success", true, "key", key, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/list/{key}")
    public Map<String, Object> listRange(@PathVariable String key,
                                         @RequestParam(defaultValue = "0") long start,
                                         @RequestParam(defaultValue = "-1") long end) {
        var keys = redisOps.scanToSet(key + ":list:*");
        return Map.of("success", true, "key", key, "values", keys, "count", keys.size(),
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/set/{key}")
    public Map<String, Object> setAdd(@PathVariable String key, @RequestBody List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            redisOps.set(key + ":member:" + i, String.valueOf(values.get(i)));
        }
        return Map.of("success", true, "key", key, "addedCount", values.size(),
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/performance/batch-set")
    public Map<String, Object> batchSetPerformance(@RequestParam(defaultValue = "100") int count) {
        long startTime = System.currentTimeMillis();
        Map<String, String> entries = new HashMap<>();
        for (int i = 0; i < count; i++) {
            entries.put("perf_test_" + i, "{\"index\":" + i + ",\"timestamp\":" + System.currentTimeMillis() + "}");
        }
        redisOps.pipelineSet(entries, Duration.ofMinutes(5));
        long duration = System.currentTimeMillis() - startTime;

        return Map.of("success", true,
                "stats", Map.of("total", count, "durationMs", duration,
                        "throughput", count * 1000.0 / Math.max(duration, 1)),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/concurrent-test")
    public CompletableFuture<Map<String, Object>> concurrentTest(@RequestParam(defaultValue = "10") int concurrentCount) {
        var futures = java.util.stream.IntStream.range(0, concurrentCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    redisOps.set("concurrent_test_" + i, "{\"threadIndex\":" + i + "}", Duration.ofMinutes(5));
                    return true;
                }, Thread.ofVirtual()::start))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> Map.of("success", true,
                        "stats", Map.of("concurrentCount", concurrentCount,
                                "successCount", futures.stream().filter(f -> {
                                    try { return f.get(); } catch (Exception e) { return false; }
                                }).count()),
                        "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            String pong = redisOps.ping();
            return Map.of("status", "PONG".equals(pong) ? "UP" : "DOWN", "service", "Redis",
                    "testResult", pong, "message", "Redis 服务运行正常",
                    "timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            return Map.of("status", "DOWN", "service", "Redis", "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/flush")
    public Map<String, Object> flushDatabase() {
        boolean ok = redisOps.flushDb();
        return Map.of("success", ok, "message", ok ? "数据库清空成功" : "数据库清空失败",
                "warning", "此操作会删除所有数据，请谨慎使用",
                "timestamp", System.currentTimeMillis());
    }
}
