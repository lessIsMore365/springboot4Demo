package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.RedisService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis服务实现类
 * 演示虚拟线程环境下的Redis操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 基本键值操作 ==========

    @Override
    public boolean set(String key, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("设置Redis键值对 - 键: {}, 值类型: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, value.getClass().getSimpleName(), currentThread, currentThread.isVirtual());

        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置Redis键值对失败 - 键: {}", key, e);
            return false;
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> setAsync(String key, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("异步设置Redis键值对 - 键: {}, 值类型: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, value.getClass().getSimpleName(), currentThread, currentThread.isVirtual());

        boolean result = this.set(key, value);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean setWithExpire(String key, Object value, long timeout, TimeUnit unit) {
        Thread currentThread = Thread.currentThread();
        log.info("设置Redis键值对并指定过期时间 - 键: {}, 过期时间: {} {}, 当前线程: {}, 是否虚拟线程: {}",
                key, timeout, unit, currentThread, currentThread.isVirtual());

        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("设置Redis键值对并指定过期时间失败 - 键: {}", key, e);
            return false;
        }
    }

    @Override
    public Object get(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis值 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForValue().get(key);
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Object> getAsync(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("异步获取Redis值 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Object result = this.get(key);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = this.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (type.isInstance(value)) {
                return (T) value;
            }
            // 尝试使用Jackson转换
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("转换Redis值为指定类型失败 - 键: {}, 目标类型: {}", key, type.getName(), e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("删除Redis键 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Boolean result = redisTemplate.delete(key);
        return result != null && result;
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> deleteAsync(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("异步删除Redis键 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        boolean result = this.delete(key);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public long deleteBatch(List<String> keys) {
        Thread currentThread = Thread.currentThread();
        log.info("批量删除Redis键 - 键数量: {}, 当前线程: {}, 是否虚拟线程: {}",
                keys.size(), currentThread, currentThread.isVirtual());

        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }

    @Override
    public boolean exists(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("检查Redis键是否存在 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Thread currentThread = Thread.currentThread();
        log.info("设置Redis键过期时间 - 键: {}, 过期时间: {} {}, 当前线程: {}, 是否虚拟线程: {}",
                key, timeout, unit, currentThread, currentThread.isVirtual());

        Boolean result = redisTemplate.expire(key, timeout, unit);
        return result != null && result;
    }

    @Override
    public long getExpire(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis键过期时间 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2; // -2表示键不存在
    }

    // ========== 哈希操作 ==========

    @Override
    public boolean hSet(String key, String field, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("设置Redis哈希字段值 - 键: {}, 字段: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, field, currentThread, currentThread.isVirtual());

        try {
            redisTemplate.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            log.error("设置Redis哈希字段值失败 - 键: {}, 字段: {}", key, field, e);
            return false;
        }
    }

    @Override
    public Object hGet(String key, String field) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis哈希字段值 - 键: {}, 字段: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, field, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForHash().get(key, field);
    }

    @Override
    public Map<Object, Object> hGetAll(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis哈希所有字段和值 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public long hDelete(String key, Object... fields) {
        Thread currentThread = Thread.currentThread();
        log.info("删除Redis哈希字段 - 键: {}, 字段数量: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, fields.length, currentThread, currentThread.isVirtual());

        Long count = redisTemplate.opsForHash().delete(key, fields);
        return count != null ? count : 0;
    }

    // ========== 列表操作 ==========

    @Override
    public long lPush(String key, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("向左推入Redis列表 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Long length = redisTemplate.opsForList().leftPush(key, value);
        return length != null ? length : 0;
    }

    @Override
    public long rPush(String key, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("向右推入Redis列表 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Long length = redisTemplate.opsForList().rightPush(key, value);
        return length != null ? length : 0;
    }

    @Override
    public Object lPop(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("从左弹出Redis列表 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public List<Object> lRange(String key, long start, long end) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis列表范围 - 键: {}, 起始索引: {}, 结束索引: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, start, end, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForList().range(key, start, end);
    }

    // ========== 集合操作 ==========

    @Override
    public long sAdd(String key, Object... values) {
        Thread currentThread = Thread.currentThread();
        log.info("添加Redis集合元素 - 键: {}, 元素数量: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, values.length, currentThread, currentThread.isVirtual());

        Long count = redisTemplate.opsForSet().add(key, values);
        return count != null ? count : 0;
    }

    @Override
    public Set<Object> sMembers(String key) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis集合所有元素 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public boolean sIsMember(String key, Object value) {
        Thread currentThread = Thread.currentThread();
        log.info("判断Redis集合元素是否存在 - 键: {}, 当前线程: {}, 是否虚拟线程: {}",
                key, currentThread, currentThread.isVirtual());

        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        return result != null && result;
    }

    // ========== 统计和工具方法 ==========

    @Override
    public String getRedisInfo() {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis信息 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            Properties info = connection.info();
            // 将Properties转换为字符串
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Object, Object> entry : info.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return "Redis信息获取成功:\n" + sb.toString();
        } catch (Exception e) {
            log.error("获取Redis信息失败", e);
            return "获取Redis信息失败: " + e.getMessage();
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<String> getRedisInfoAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步获取Redis信息 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        String result = this.getRedisInfo();
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Set<String> keys(String pattern) {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis键（匹配模式） - 模式: {}, 当前线程: {}, 是否虚拟线程: {}",
                pattern, currentThread, currentThread.isVirtual());

        Set<String> keys = new HashSet<>();
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.error("获取Redis键失败 - 模式: {}", pattern, e);
        }
        return keys;
    }

    @Override
    public boolean flushDb() {
        Thread currentThread = Thread.currentThread();
        log.info("清空Redis当前数据库 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.flushDb();
            return true;
        } catch (Exception e) {
            log.error("清空Redis数据库失败", e);
            return false;
        }
    }

    @Override
    public String testConnection() {
        Thread currentThread = Thread.currentThread();
        log.info("测试Redis连接 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        try {
            String result = redisTemplate.execute((RedisConnection connection) ->
                    "Redis连接测试成功，PING: " + connection.ping()
            );
            return result;
        } catch (Exception e) {
            log.error("Redis连接测试失败", e);
            return "Redis连接测试失败: " + e.getMessage();
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<String> testConnectionAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步测试Redis连接 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        String result = this.testConnection();
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Map<String, Object> getStats() {
        Thread currentThread = Thread.currentThread();
        log.info("获取Redis统计信息 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取键数量
            Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
            stats.put("totalKeys", dbSize != null ? dbSize : 0);

            // 获取内存信息
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                Properties info = connection.info("memory");
                stats.put("memoryInfo", info);
            }

            // 获取客户端信息
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                Properties clientInfo = connection.info("clients");
                stats.put("clientInfo", clientInfo);
            }

            // 获取服务器信息
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                Properties serverInfo = connection.info("server");
                stats.put("serverInfo", serverInfo);
            }

            stats.put("success", true);
            stats.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("获取Redis统计信息失败", e);
            stats.put("success", false);
            stats.put("error", e.getMessage());
            stats.put("timestamp", System.currentTimeMillis());
        }

        return stats;
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Map<String, Object>> getStatsAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步获取Redis统计信息 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        Map<String, Object> result = this.getStats();
        return CompletableFuture.completedFuture(result);
    }
}