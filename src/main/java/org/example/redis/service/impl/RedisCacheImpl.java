package org.example.redis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyGenerator;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.monitor.RedisMetrics;
import org.example.redis.service.RedisCache;
import org.example.redis.service.RedisOps;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RedisCacheImpl implements RedisCache {

    private final RedisOps redisOps;
    private final ObjectMapper objectMapper;
    private final RedisMetrics metrics;
    private static final String HIT_MARKER = "1";

    public RedisCacheImpl(RedisOps redisOps, ObjectMapper objectMapper, RedisMetrics metrics) {
        this.redisOps = redisOps;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    // ========== 基础读写 ==========

    @Override
    public <T> Optional<T> get(RedisKeyNamespace ns, String key, Class<T> type) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        String json = redisOps.get(fullKey);
        if (json == null) {
            metrics.recordMiss();
            return Optional.empty();
        }
        metrics.recordHit();
        try {
            // String 类型直接返回，不需要反序列化
            if (type == String.class) {
                @SuppressWarnings("unchecked")
                T value = (T) json;
                return Optional.of(value);
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("缓存反序列化失败: ns={}, key={}, type={}", ns.prefix(), key, type.getName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getString(RedisKeyNamespace ns, String key) {
        return get(ns, key, String.class);
    }

    @Override
    public <T> void put(RedisKeyNamespace ns, String key, T value, Duration ttl) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        String json;
        try {
            json = value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("缓存序列化失败: ns={}, key={}", ns.prefix(), key);
            return;
        }
        redisOps.set(fullKey, json, ttl != null ? ttl : ns.defaultTtl());
        metrics.recordPut();
    }

    // ========== Cache-Aside ==========

    @Override
    public <T> T getOrFetch(RedisKeyNamespace ns, String key, Class<T> type,
                            Supplier<T> loader, Duration ttl) {
        Optional<T> cached = get(ns, key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = loader.get();
        if (value != null) {
            put(ns, key, value, ttl);
        }
        return value;
    }

    // ========== 列表缓存 ==========

    @Override
    public <T> void putAll(RedisKeyNamespace ns, String key, List<T> values, Duration ttl) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        if (values.isEmpty()) return;
        try {
            for (T v : values) {
                String json = v instanceof String ? (String) v : objectMapper.writeValueAsString(v);
                redisOps.pipelineSet(Map.of(fullKey + ":idx:" + values.indexOf(v), json), ttl);
            }
            // 用索引 key 记录列表长度
            redisOps.set(fullKey + ":size", String.valueOf(values.size()), ttl);
            metrics.recordPut(values.size());
        } catch (Exception e) {
            log.warn("列表缓存写入失败: ns={}, key={}", ns.prefix(), key);
        }
    }

    @Override
    public <T> List<T> getList(RedisKeyNamespace ns, String key, Class<T> elementType) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        String sizeStr = redisOps.get(fullKey + ":size");
        if (sizeStr == null) {
            metrics.recordMiss();
            return Collections.emptyList();
        }
        int size;
        try {
            size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
        List<String> idxKeys = java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> fullKey + ":idx:" + i)
                .collect(Collectors.toList());
        List<String> jsons = redisOps.pipelineGet(idxKeys);
        if (jsons.isEmpty()) {
            metrics.recordMiss();
            return Collections.emptyList();
        }
        metrics.recordHit();
        return jsons.stream()
                .map(j -> {
                    try {
                        if (elementType == String.class) {
                            @SuppressWarnings("unchecked")
                            T v = (T) j;
                            return v;
                        }
                        return objectMapper.readValue(j, elementType);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());
    }

    // ========== 删除 ==========

    @Override
    public boolean evict(RedisKeyNamespace ns, String key) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        boolean deleted = redisOps.delete(fullKey);
        if (deleted) metrics.recordEvict();
        return deleted;
    }

    @Override
    public long evictByPrefix(RedisKeyNamespace ns, String prefix) {
        String pattern = prefix != null && !prefix.isEmpty()
                ? RedisKeyGenerator.pattern(ns, prefix)
                : RedisKeyGenerator.pattern(ns);
        Set<String> keys = redisOps.scanToSet(pattern);
        if (keys.isEmpty()) return 0;
        long count = redisOps.deleteBatch(keys);
        metrics.recordEvict((int) count);
        return count;
    }

    // ========== SET NX ==========

    @Override
    public boolean setIfAbsent(RedisKeyNamespace ns, String key, String value, Duration ttl) {
        String fullKey = RedisKeyGenerator.key(ns, key);
        return redisOps.setIfAbsent(fullKey, value, ttl != null ? ttl : ns.defaultTtl());
    }

    // ========== 统计 ==========

    @Override
    public CacheStats stats() {
        return new CacheStats(
                metrics.hits(),
                metrics.misses(),
                metrics.hitRate(),
                metrics.putCount(),
                metrics.evictCount()
        );
    }
}
