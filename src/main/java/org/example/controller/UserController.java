package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 用户控制器
 * 演示MyBatis Plus在虚拟线程环境下的使用
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建用户（需要管理员权限）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        log.info("创建用户请求: {}", user.getUsername());

        boolean success = userService.addUser(user);

        return Map.of(
                "success", success,
                "message", success ? "用户创建成功" : "用户创建失败",
                "data", success ? user : null,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步创建用户 - 使用虚拟线程
     */
    @PostMapping("/async")
    public CompletableFuture<Map<String, Object>> createUserAsync(@RequestBody User user) {
        log.info("异步创建用户请求: {}", user.getUsername());

        return userService.addUserAsync(user)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? "用户异步创建成功" : "用户异步创建失败",
                        "data", success ? user : null,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 批量创建测试用户
     */
    @PostMapping("/batch")
    public Map<String, Object> batchCreateUsers(@RequestParam(defaultValue = "10") int count) {
        log.info("批量创建 {} 个测试用户", count);

        List<User> users = IntStream.range(0, count)
                .mapToObj(i -> {
                    User user = new User();
                    user.setUsername("test_user_" + System.currentTimeMillis() + "_" + i);
                    user.setEmail("user" + i + "@example.com");
                    user.setAge(20 + i);
                    user.setRemark("批量创建测试用户 #" + i);
                    return user;
                })
                .collect(Collectors.toList());

        boolean success = userService.batchAddUsers(users);

        return Map.of(
                "success", success,
                "message", success ? String.format("成功批量创建 %d 个用户", count) : "批量创建失败",
                "count", success ? count : 0,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步批量创建用户 - 使用虚拟线程
     */
    @PostMapping("/batch/async")
    public CompletableFuture<Map<String, Object>> batchCreateUsersAsync(@RequestParam(defaultValue = "10") int count) {
        log.info("异步批量创建 {} 个测试用户", count);

        List<User> users = IntStream.range(0, count)
                .mapToObj(i -> {
                    User user = new User();
                    user.setUsername("async_user_" + System.currentTimeMillis() + "_" + i);
                    user.setEmail("async" + i + "@example.com");
                    user.setAge(25 + i);
                    user.setRemark("异步批量创建测试用户 #" + i);
                    return user;
                })
                .collect(Collectors.toList());

        return userService.batchAddUsersAsync(users)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? String.format("成功异步批量创建 %d 个用户", count) : "异步批量创建失败",
                        "count", success ? count : 0,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("分页查询用户 - 页码: {}, 大小: {}", page, size);

        Page<User> userPage = userService.getUsersByPage(page, size);

        return Map.of(
                "success", true,
                "data", userPage.getRecords(),
                "pagination", Map.of(
                        "page", userPage.getCurrent(),
                        "size", userPage.getSize(),
                        "total", userPage.getTotal(),
                        "pages", userPage.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步分页查询用户 - 使用虚拟线程
     */
    @GetMapping("/async")
    public CompletableFuture<Map<String, Object>> getUsersAsync(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("异步分页查询用户 - 页码: {}, 大小: {}", page, size);

        return userService.getUsersByPageAsync(page, size)
                .thenApply(userPage -> Map.of(
                        "success", true,
                        "data", userPage.getRecords(),
                        "pagination", Map.of(
                                "page", userPage.getCurrent(),
                                "size", userPage.getSize(),
                                "total", userPage.getTotal(),
                                "pages", userPage.getPages()
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getUserStats() {
        log.info("获取用户统计信息");

        long totalCount = userService.countUsers();

        return Map.of(
                "success", true,
                "stats", Map.of(
                        "totalUsers", totalCount
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步获取用户统计信息 - 使用虚拟线程
     */
    @GetMapping("/stats/async")
    public CompletableFuture<Map<String, Object>> getUserStatsAsync() {
        log.info("异步获取用户统计信息");

        return userService.countUsersAsync()
                .thenApply(totalCount -> Map.of(
                        "success", true,
                        "stats", Map.of(
                                "totalUsers", totalCount
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 数据库性能测试
     */
    @GetMapping("/performance")
    public Map<String, Object> testPerformance() {
        log.info("开始数据库性能测试");

        String performanceResult = userService.testDatabasePerformance();

        return Map.of(
                "success", true,
                "message", "数据库性能测试完成",
                "performance", performanceResult,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 并发测试 - 模拟多个并发请求
     */
    @GetMapping("/concurrent-test")
    public CompletableFuture<Map<String, Object>> concurrentTest(
            @RequestParam(defaultValue = "5") int concurrentCount) {
        log.info("开始并发测试，并发数: {}", concurrentCount);

        List<CompletableFuture<Long>> futures = IntStream.range(0, concurrentCount)
                .mapToObj(i -> userService.countUsersAsync())
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Long> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    long total = results.stream().mapToLong(Long::longValue).sum();
                    long average = results.isEmpty() ? 0 : total / results.size();

                    return Map.of(
                            "success", true,
                            "message", String.format("并发测试完成，并发数: %d", concurrentCount),
                            "results", Map.of(
                                    "concurrentCount", concurrentCount,
                                    "totalFromAllRequests", total,
                                    "averageCount", average,
                                    "individualResults", results
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
        log.info("用户服务健康检查");

        try {
            long count = userService.countUsers();
            return Map.of(
                    "status", "UP",
                    "database", "PostgreSQL with MyBatis Plus",
                    "userCount", count,
                    "message", "用户服务运行正常",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("用户服务健康检查失败", e);
            return Map.of(
                    "status", "DOWN",
                    "database", "PostgreSQL with MyBatis Plus",
                    "error", e.getMessage(),
                    "message", "用户服务健康检查失败",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}