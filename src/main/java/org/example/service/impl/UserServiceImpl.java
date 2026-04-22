package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务实现类
 * 演示虚拟线程环境下的MyBatis Plus操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 添加用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(User user) {
        Thread currentThread = Thread.currentThread();
        log.info("添加用户 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        return this.save(user);
    }

    /**
     * 异步添加用户 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> addUserAsync(User user) {
        Thread currentThread = Thread.currentThread();
        log.info("异步添加用户 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        boolean result = this.save(user);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量添加用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddUsers(List<User> users) {
        Thread currentThread = Thread.currentThread();
        log.info("批量添加 {} 个用户 - 当前线程: {}, 是否虚拟线程: {}",
                users.size(), currentThread, currentThread.isVirtual());

        return this.saveBatch(users);
    }

    /**
     * 异步批量添加用户 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> batchAddUsersAsync(List<User> users) {
        Thread currentThread = Thread.currentThread();
        log.info("异步批量添加 {} 个用户 - 当前线程: {}, 是否虚拟线程: {}",
                users.size(), currentThread, currentThread.isVirtual());

        boolean result = this.saveBatch(users);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分页查询用户
     */
    @Override
    public Page<User> getUsersByPage(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("分页查询用户 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<User> pageParam = new Page<>(page, size);
        return this.page(pageParam);
    }

    /**
     * 异步分页查询用户 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<User>> getUsersByPageAsync(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("异步分页查询用户 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<User> result = this.getUsersByPage(page, size);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public List<User> getUsersByUsername(String username) {
        Thread currentThread = Thread.currentThread();
        log.info("根据用户名查询用户 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                username, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return this.list(queryWrapper);
    }

    /**
     * 统计用户数量
     */
    @Override
    public long countUsers() {
        Thread currentThread = Thread.currentThread();
        log.info("统计用户数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        return this.count();
    }

    /**
     * 异步统计用户数量 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Long> countUsersAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步统计用户数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        long count = this.count();
        return CompletableFuture.completedFuture(count);
    }

    /**
     * 测试数据库连接和性能
     */
    @Override
    public String testDatabasePerformance() {
        Thread currentThread = Thread.currentThread();
        log.info("开始数据库性能测试 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        Instant start = Instant.now();

        // 测试1: 统计数量
        long count = this.count();
        log.info("数据库统计测试 - 用户总数: {}", count);

        // 测试2: 分页查询
        Page<User> pageResult = this.getUsersByPage(1, 10);
        log.info("数据库分页测试 - 总记录数: {}, 当前页数据: {}",
                pageResult.getTotal(), pageResult.getRecords().size());

        // 测试3: 插入测试数据
        User testUser = new User();
        testUser.setUsername("test_user_" + System.currentTimeMillis());
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
        testUser.setRemark("性能测试用户");

        boolean insertResult = this.save(testUser);
        log.info("数据库插入测试 - 插入结果: {}, 用户ID: {}", insertResult, testUser.getId());

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        String result = String.format("""
                数据库性能测试完成:
                - 测试线程: %s (虚拟线程: %s)
                - 用户总数: %d
                - 分页查询: %d 条记录
                - 插入测试: %s (ID: %d)
                - 总耗时: %d 毫秒
                - 平均耗时: %.2f 毫秒/操作
                """,
                currentThread.getName(),
                currentThread.isVirtual(),
                count,
                pageResult.getRecords().size(),
                insertResult ? "成功" : "失败",
                testUser.getId(),
                duration.toMillis(),
                duration.toMillis() / 4.0);

        log.info("数据库性能测试结果:\n{}", result);
        return result;
    }

    /**
     * 验证用户密码
     */
    @Override
    public boolean verifyPassword(String username, String password) {
        Thread currentThread = Thread.currentThread();
        log.info("验证用户密码 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                username, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = this.getOne(queryWrapper);

        if (user == null || user.getPassword() == null) {
            return false;
        }

        boolean matches = passwordEncoder.matches(password, user.getPassword());
        log.info("密码验证结果 - 用户名: {}, 结果: {}", username, matches);
        return matches;
    }
}