package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务接口
 * 演示虚拟线程环境下的数据库操作
 */
public interface UserService extends IService<User> {

    /**
     * 添加用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean addUser(User user);

    /**
     * 异步添加用户 - 使用虚拟线程
     * @param user 用户信息
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> addUserAsync(User user);

    /**
     * 批量添加用户
     * @param users 用户列表
     * @return 是否成功
     */
    boolean batchAddUsers(List<User> users);

    /**
     * 异步批量添加用户 - 使用虚拟线程
     * @param users 用户列表
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> batchAddUsersAsync(List<User> users);

    /**
     * 分页查询用户
     * @param page 页码
     * @param size 每页大小
     * @return 用户分页数据
     */
    Page<User> getUsersByPage(int page, int size);

    /**
     * 异步分页查询用户 - 使用虚拟线程
     * @param page 页码
     * @param size 每页大小
     * @return CompletableFuture
     */
    CompletableFuture<Page<User>> getUsersByPageAsync(int page, int size);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户列表
     */
    List<User> getUsersByUsername(String username);

    /**
     * 统计用户数量
     * @return 用户数量
     */
    long countUsers();

    /**
     * 异步统计用户数量 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<Long> countUsersAsync();

    /**
     * 验证用户密码
     * @param username 用户名
     * @param password 密码
     * @return 是否验证成功
     */
    boolean verifyPassword(String username, String password);

    /**
     * 测试数据库连接和性能
     * @return 测试结果
     */
    String testDatabasePerformance();
}