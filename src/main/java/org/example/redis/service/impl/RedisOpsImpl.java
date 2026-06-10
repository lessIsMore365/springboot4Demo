package org.example.redis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.redis.service.RedisOps;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class RedisOpsImpl implements RedisOps {

    private final StringRedisTemplate template;

    public RedisOpsImpl(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        template.opsForValue().set(key, value, ttl);
    }

    @Override
    public void set(String key, String value) {
        template.opsForValue().set(key, value);
    }

    @Override
    public String get(String key) {
        return template.opsForValue().get(key);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(template.delete(key));
    }

    @Override
    public long deleteBatch(Collection<String> keys) {
        Long count = template.delete(keys);
        return count != null ? count : 0;
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(template.hasKey(key));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(template.expire(key, ttl));
    }

    @Override
    public long getTtlSeconds(String key) {
        Long ttl = template.getExpire(key);
        return ttl != null ? ttl : -2;
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(template.opsForValue().setIfAbsent(key, value, ttl));
    }

    @Override
    public void pipelineSet(Map<String, String> entries, Duration ttl) {
        template.executePipelined((RedisConnection connection) -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            entries.forEach((k, v) -> {
                if (ttl != null) {
                    stringConn.set(k, v);
                    stringConn.expire(k, ttl.getSeconds());
                } else {
                    stringConn.set(k, v);
                }
            });
            return null;
        });
    }

    @Override
    public List<String> pipelineGet(List<String> keys) {
        List<Object> results = template.executePipelined((RedisConnection connection) -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (String key : keys) {
                stringConn.get(key);
            }
            return null;
        });
        if (results == null) return List.of();
        return results.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @Override
    public long scan(String pattern, Consumer<String> keyHandler) {
        long count = 0;
        try (RedisConnection connection = Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()) {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keyHandler.accept(new String(cursor.next()));
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("SCAN 操作失败: pattern={}, error={}", pattern, e.getMessage());
        }
        return count;
    }

    @Override
    public Set<String> scanToSet(String pattern) {
        Set<String> result = new HashSet<>();
        scan(pattern, result::add);
        return result;
    }

    @Override
    public String ping() {
        try (RedisConnection connection = Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()) {
            return connection.ping();
        }
    }

    @Override
    public long dbSize() {
        try (RedisConnection connection = Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()) {
            Long size = connection.dbSize();
            return size != null ? size : 0;
        }
    }

    @Override
    public Map<String, String> info(String section) {
        Map<String, String> result = new LinkedHashMap<>();
        try (RedisConnection connection = Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()) {
            Properties props = connection.info(section);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public boolean flushDb() {
        try (RedisConnection connection = Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()) {
            connection.flushDb();
            return true;
        } catch (Exception e) {
            log.error("FlushDB 失败", e);
            return false;
        }
    }

    @Override
    public CompletableFuture<String> pingAsync() {
        return CompletableFuture.supplyAsync(this::ping, Thread.ofVirtual()::start);
    }

    @Override
    public CompletableFuture<Map<String, String>> infoAsync(String section) {
        return CompletableFuture.supplyAsync(() -> info(section), Thread.ofVirtual()::start);
    }
}
