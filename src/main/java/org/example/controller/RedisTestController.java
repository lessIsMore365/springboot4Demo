package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.RedisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Redis测试控制器
 * 提供Redis操作测试端点
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisService redisService;

    /**
     * 测试Redis连接
     */
    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        log.info("测试Redis连接");

        String result = redisService.testConnection();

        return Map.of(
                "success", true,
                "message", result,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步测试Redis连接 - 使用虚拟线程
     */
    @GetMapping("/test/async")
    public CompletableFuture<Map<String, Object>> testConnectionAsync() {
        log.info("异步测试Redis连接");

        return redisService.testConnectionAsync()
                .thenApply(result -> Map.of(
                        "success", true,
                        "message", result,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 设置键值对
     */
    @PostMapping("/set")
    public Map<String, Object> setKeyValue(@RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        Object value = request.get("value");
        Long timeout = request.containsKey("timeout") ? ((Number) request.get("timeout")).longValue() : null;
        String timeUnitStr = (String) request.get("timeUnit");

        log.info("设置Redis键值对 - 键: {}, 值: {}, 超时: {}, 时间单位: {}", key, value, timeout, timeUnitStr);

        boolean success;
        if (timeout != null && timeUnitStr != null) {
            TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr.toUpperCase());
            success = redisService.setWithExpire(key, value, timeout, timeUnit);
        } else {
            success = redisService.set(key, value);
        }

        return Map.of(
                "success", success,
                "message", success ? "设置成功" : "设置失败",
                "key", key,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步设置键值对 - 使用虚拟线程
     */
    @PostMapping("/set/async")
    public CompletableFuture<Map<String, Object>> setKeyValueAsync(@RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        Object value = request.get("value");

        log.info("异步设置Redis键值对 - 键: {}, 值: {}", key, value);

        return redisService.setAsync(key, value)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? "异步设置成功" : "异步设置失败",
                        "key", key,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 获取键值对
     */
    @GetMapping("/get/{key}")
    public Map<String, Object> getValue(@PathVariable String key) {
        log.info("获取Redis键值对 - 键: {}", key);

        Object value = redisService.get(key);

        return Map.of(
                "success", true,
                "key", key,
                "value", value,
                "exists", value != null,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步获取键值对 - 使用虚拟线程
     */
    @GetMapping("/get/{key}/async")
    public CompletableFuture<Map<String, Object>> getValueAsync(@PathVariable String key) {
        log.info("异步获取Redis键值对 - 键: {}", key);

        return redisService.getAsync(key)
                .thenApply(value -> Map.of(
                        "success", true,
                        "key", key,
                        "value", value,
                        "exists", value != null,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 删除键值对
     */
    @DeleteMapping("/delete/{key}")
    public Map<String, Object> deleteKey(@PathVariable String key) {
        log.info("删除Redis键 - 键: {}", key);

        boolean success = redisService.delete(key);

        return Map.of(
                "success", success,
                "message", success ? "删除成功" : "删除失败",
                "key", key,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 检查键是否存在
     */
    @GetMapping("/exists/{key}")
    public Map<String, Object> existsKey(@PathVariable String key) {
        log.info("检查Redis键是否存在 - 键: {}", key);

        boolean exists = redisService.exists(key);

        return Map.of(
                "success", true,
                "key", key,
                "exists", exists,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 设置过期时间
     */
    @PostMapping("/expire/{key}")
    public Map<String, Object> setExpire(
            @PathVariable String key,
            @RequestParam long timeout,
            @RequestParam(defaultValue = "SECONDS") String timeUnit) {
        log.info("设置Redis键过期时间 - 键: {}, 超时: {}, 时间单位: {}", key, timeout, timeUnit);

        TimeUnit unit = TimeUnit.valueOf(timeUnit.toUpperCase());
        boolean success = redisService.expire(key, timeout, unit);

        return Map.of(
                "success", success,
                "message", success ? "设置过期时间成功" : "设置过期时间失败",
                "key", key,
                "timeout", timeout,
                "timeUnit", timeUnit,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取Redis信息
     */
    @GetMapping("/info")
    public Map<String, Object> getRedisInfo() {
        log.info("获取Redis信息");

        String info = redisService.getRedisInfo();

        return Map.of(
                "success", true,
                "info", info,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取Redis统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getRedisStats() {
        log.info("获取Redis统计信息");

        Map<String, Object> stats = redisService.getStats();

        return Map.of(
                "success", true,
                "stats", stats,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取所有键（匹配模式）
     */
    @GetMapping("/keys")
    public Map<String, Object> getKeys(@RequestParam(defaultValue = "*") String pattern) {
        log.info("获取Redis键 - 模式: {}", pattern);

        var keys = redisService.keys(pattern);

        return Map.of(
                "success", true,
                "pattern", pattern,
                "keys", keys,
                "count", keys.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 哈希操作 - 设置字段值
     */
    @PostMapping("/hash/{key}/{field}")
    public Map<String, Object> hashSet(
            @PathVariable String key,
            @PathVariable String field,
            @RequestBody Object value) {
        log.info("设置Redis哈希字段值 - 键: {}, 字段: {}, 值: {}", key, field, value);

        boolean success = redisService.hSet(key, field, value);

        return Map.of(
                "success", success,
                "message", success ? "设置哈希字段值成功" : "设置哈希字段值失败",
                "key", key,
                "field", field,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 哈希操作 - 获取字段值
     */
    @GetMapping("/hash/{key}/{field}")
    public Map<String, Object> hashGet(
            @PathVariable String key,
            @PathVariable String field) {
        log.info("获取Redis哈希字段值 - 键: {}, 字段: {}", key, field);

        Object value = redisService.hGet(key, field);

        return Map.of(
                "success", true,
                "key", key,
                "field", field,
                "value", value,
                "exists", value != null,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 列表操作 - 向左推入
     */
    @PostMapping("/list/{key}/lpush")
    public Map<String, Object> listLeftPush(
            @PathVariable String key,
            @RequestBody Object value) {
        log.info("向左推入Redis列表 - 键: {}, 值: {}", key, value);

        long length = redisService.lPush(key, value);

        return Map.of(
                "success", true,
                "message", "向左推入成功",
                "key", key,
                "value", value,
                "newLength", length,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 列表操作 - 获取范围
     */
    @GetMapping("/list/{key}")
    public Map<String, Object> listRange(
            @PathVariable String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end) {
        log.info("获取Redis列表范围 - 键: {}, 起始索引: {}, 结束索引: {}", key, start, end);

        List<Object> values = redisService.lRange(key, start, end);

        return Map.of(
                "success", true,
                "key", key,
                "values", values,
                "count", values.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 集合操作 - 添加元素
     */
    @PostMapping("/set/{key}")
    public Map<String, Object> setAdd(
            @PathVariable String key,
            @RequestBody List<Object> values) {
        log.info("添加Redis集合元素 - 键: {}, 元素数量: {}", key, values.size());

        long addedCount = redisService.sAdd(key, values.toArray());

        return Map.of(
                "success", true,
                "message", "添加集合元素成功",
                "key", key,
                "addedCount", addedCount,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 性能测试 - 批量设置键值对
     */
    @PostMapping("/performance/batch-set")
    public Map<String, Object> batchSetPerformance(@RequestParam(defaultValue = "100") int count) {
        log.info("Redis性能测试 - 批量设置 {} 个键值对", count);

        long startTime = System.currentTimeMillis();
        int successCount = 0;

        for (int i = 0; i < count; i++) {
            String key = "perf_test_" + i;
            Map<String, Object> value = Map.of(
                    "index", i,
                    "timestamp", System.currentTimeMillis(),
                    "data", "性能测试数据 " + i
            );
            if (redisService.set(key, value)) {
                successCount++;
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        return Map.of(
                "success", true,
                "message", "性能测试完成",
                "stats", Map.of(
                        "total", count,
                        "success", successCount,
                        "failed", count - successCount,
                        "durationMs", duration,
                        "throughput", count * 1000.0 / duration,
                        "averageLatencyMs", (double) duration / count
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 并发测试 - 模拟多个并发Redis操作
     */
    @GetMapping("/concurrent-test")
    public CompletableFuture<Map<String, Object>> concurrentTest(
            @RequestParam(defaultValue = "10") int concurrentCount) {
        log.info("Redis并发测试 - 并发数: {}", concurrentCount);

        // 创建多个并发操作
        var futures = java.util.stream.IntStream.range(0, concurrentCount)
                .mapToObj(i -> redisService.setAsync("concurrent_test_" + i, Map.of(
                        "threadIndex", i,
                        "timestamp", System.currentTimeMillis(),
                        "data", "并发测试数据 " + i
                )))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int successCount = 0;
                    for (var future : futures) {
                        if (future.join()) {
                            successCount++;
                        }
                    }

                    return Map.of(
                            "success", true,
                            "message", "并发测试完成",
                            "stats", Map.of(
                                    "concurrentCount", concurrentCount,
                                    "successCount", successCount,
                                    "failedCount", concurrentCount - successCount,
                                    "successRate", (double) successCount / concurrentCount * 100
                            ),
                            "timestamp", System.currentTimeMillis()
                    );
                });
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.info("Redis服务健康检查");

        try {
            String testResult = redisService.testConnection();
            boolean isHealthy = testResult.contains("成功");

            return Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "service", "Redis",
                    "testResult", testResult,
                    "message", isHealthy ? "Redis服务运行正常" : "Redis服务异常",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return Map.of(
                    "status", "DOWN",
                    "service", "Redis",
                    "error", e.getMessage(),
                    "message", "Redis健康检查失败",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 清空当前数据库（需要管理员权限）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/flush")
    public Map<String, Object> flushDatabase() {
        log.info("清空Redis当前数据库");

        boolean success = redisService.flushDb();

        return Map.of(
                "success", success,
                "message", success ? "数据库清空成功" : "数据库清空失败",
                "warning", "此操作会删除所有数据，请谨慎使用",
                "timestamp", System.currentTimeMillis()
        );
    }
}