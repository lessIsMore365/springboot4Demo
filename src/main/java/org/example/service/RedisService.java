package org.example.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务接口
 * 提供Redis基本操作，支持虚拟线程异步操作
 */
public interface RedisService {

    // ========== 基本键值操作 ==========

    /**
     * 设置键值对
     * @param key 键
     * @param value 值
     * @return 是否成功
     */
    boolean set(String key, Object value);

    /**
     * 异步设置键值对 - 使用虚拟线程
     * @param key 键
     * @param value 值
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> setAsync(String key, Object value);

    /**
     * 设置键值对并指定过期时间
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    boolean setWithExpire(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取值
     * @param key 键
     * @return 值
     */
    Object get(String key);

    /**
     * 异步获取值 - 使用虚拟线程
     * @param key 键
     * @return CompletableFuture
     */
    CompletableFuture<Object> getAsync(String key);

    /**
     * 获取值并转换为指定类型
     * @param key 键
     * @param type 类型
     * @return 值
     */
    <T> T get(String key, Class<T> type);

    /**
     * 删除键
     * @param key 键
     * @return 是否成功
     */
    boolean delete(String key);

    /**
     * 异步删除键 - 使用虚拟线程
     * @param key 键
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> deleteAsync(String key);

    /**
     * 批量删除键
     * @param keys 键列表
     * @return 成功删除的数量
     */
    long deleteBatch(List<String> keys);

    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取过期时间
     * @param key 键
     * @return 剩余过期时间（秒）
     */
    long getExpire(String key);

    // ========== 哈希操作 ==========

    /**
     * 设置哈希字段值
     * @param key 键
     * @param field 字段
     * @param value 值
     * @return 是否成功
     */
    boolean hSet(String key, String field, Object value);

    /**
     * 获取哈希字段值
     * @param key 键
     * @param field 字段
     * @return 值
     */
    Object hGet(String key, String field);

    /**
     * 获取哈希所有字段和值
     * @param key 键
     * @return 字段-值映射
     */
    Map<Object, Object> hGetAll(String key);

    /**
     * 删除哈希字段
     * @param key 键
     * @param fields 字段列表
     * @return 成功删除的数量
     */
    long hDelete(String key, Object... fields);

    // ========== 列表操作 ==========

    /**
     * 向左推入列表
     * @param key 键
     * @param value 值
     * @return 列表长度
     */
    long lPush(String key, Object value);

    /**
     * 向右推入列表
     * @param key 键
     * @param value 值
     * @return 列表长度
     */
    long rPush(String key, Object value);

    /**
     * 从左弹出列表
     * @param key 键
     * @return 值
     */
    Object lPop(String key);

    /**
     * 获取列表范围
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 值列表
     */
    List<Object> lRange(String key, long start, long end);

    // ========== 集合操作 ==========

    /**
     * 添加集合元素
     * @param key 键
     * @param values 值数组
     * @return 成功添加的数量
     */
    long sAdd(String key, Object... values);

    /**
     * 获取集合所有元素
     * @param key 键
     * @return 元素集合
     */
    Set<Object> sMembers(String key);

    /**
     * 判断元素是否在集合中
     * @param key 键
     * @param value 值
     * @return 是否存在
     */
    boolean sIsMember(String key, Object value);

    // ========== 统计和工具方法 ==========

    /**
     * 获取Redis信息
     * @return Redis信息字符串
     */
    String getRedisInfo();

    /**
     * 异步获取Redis信息 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<String> getRedisInfoAsync();

    /**
     * 获取所有键（匹配模式）
     * @param pattern 模式
     * @return 键集合
     */
    Set<String> keys(String pattern);

    /**
     * 清空当前数据库
     * @return 是否成功
     */
    boolean flushDb();

    /**
     * 测试Redis连接
     * @return 测试结果
     */
    String testConnection();

    /**
     * 异步测试Redis连接 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<String> testConnectionAsync();

    /**
     * 获取Redis统计信息
     * @return 统计信息映射
     */
    Map<String, Object> getStats();

    /**
     * 异步获取Redis统计信息 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<Map<String, Object>> getStatsAsync();
}