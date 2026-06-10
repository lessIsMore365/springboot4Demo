package org.example.redis.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 底层 Redis 操作服务 — 对 {@link org.springframework.data.redis.core.StringRedisTemplate} 的增强封装。
 * 提供 Pipeline、非阻塞 SCAN、连接复用等高级特性。
 */
public interface RedisOps {

    // ========== 基础键值 ==========

    void set(String key, String value, Duration ttl);
    void set(String key, String value);
    String get(String key);
    boolean delete(String key);
    long deleteBatch(Collection<String> keys);
    boolean exists(String key);
    boolean expire(String key, Duration ttl);
    long getTtlSeconds(String key);

    // ========== SET NX (原子性 if-absent) ==========

    boolean setIfAbsent(String key, String value, Duration ttl);

    // ========== Pipeline 批量 ==========

    /** 通过 Pipeline 批量写入 key→value 映射，所有 key 共享同一个 TTL */
    void pipelineSet(Map<String, String> entries, Duration ttl);

    /** 通过 Pipeline 批量读取 */
    List<String> pipelineGet(List<String> keys);

    // ========== SCAN (非阻塞) ==========

    /** 使用 SCAN 遍历匹配给定 pattern 的所有 key，通过回调处理，返回扫描到的 key 总数 */
    long scan(String pattern, Consumer<String> keyHandler);

    /** SCAN 并收集所有 key（仅用于小型数据集） */
    Set<String> scanToSet(String pattern);

    // ========== Redis 信息 ==========

    String ping();
    long dbSize();
    Map<String, String> info(String section);
    boolean flushDb();

    // ========== 异步方法（虚拟线程） ==========

    CompletableFuture<String> pingAsync();
    CompletableFuture<Map<String, String>> infoAsync(String section);
}
