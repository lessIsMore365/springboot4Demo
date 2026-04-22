package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Permission;
import org.example.service.PermissionService;
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
 * 权限控制器
 * 演示MyBatis Plus在虚拟线程环境下的使用
 */
@Slf4j
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 创建权限（需要管理员权限）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Map<String, Object> createPermission(@RequestBody Permission permission) {
        log.info("创建权限请求: {}", permission.getCode());

        boolean success = permissionService.addPermission(permission);

        return Map.of(
                "success", success,
                "message", success ? "权限创建成功" : "权限创建失败",
                "data", success ? permission : null,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步创建权限 - 使用虚拟线程
     */
    @PostMapping("/async")
    public CompletableFuture<Map<String, Object>> createPermissionAsync(@RequestBody Permission permission) {
        log.info("异步创建权限请求: {}", permission.getCode());

        return permissionService.addPermissionAsync(permission)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? "权限异步创建成功" : "权限异步创建失败",
                        "data", success ? permission : null,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 批量创建测试权限
     */
    @PostMapping("/batch")
    public Map<String, Object> batchCreatePermissions(@RequestParam(defaultValue = "10") int count) {
        log.info("批量创建 {} 个测试权限", count);

        List<Permission> permissions = IntStream.range(0, count)
                .mapToObj(i -> {
                    Permission permission = new Permission();
                    permission.setName("测试权限_" + i);
                    permission.setCode("PERMISSION_TEST_" + i);
                    permission.setType("API");
                    permission.setDescription("批量创建测试权限 #" + i);
                    permission.setUrl("/api/test/" + i);
                    permission.setMethod(i % 2 == 0 ? "GET" : "POST");
                    permission.setSortOrder(i);
                    permission.setEnabled(true);
                    return permission;
                })
                .collect(Collectors.toList());

        boolean success = permissionService.batchAddPermissions(permissions);

        return Map.of(
                "success", success,
                "message", success ? String.format("成功批量创建 %d 个权限", count) : "批量创建失败",
                "count", success ? count : 0,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步批量创建权限 - 使用虚拟线程
     */
    @PostMapping("/batch/async")
    public CompletableFuture<Map<String, Object>> batchCreatePermissionsAsync(@RequestParam(defaultValue = "10") int count) {
        log.info("异步批量创建 {} 个测试权限", count);

        List<Permission> permissions = IntStream.range(0, count)
                .mapToObj(i -> {
                    Permission permission = new Permission();
                    permission.setName("异步测试权限_" + i);
                    permission.setCode("PERMISSION_ASYNC_TEST_" + i);
                    permission.setType("API");
                    permission.setDescription("异步批量创建测试权限 #" + i);
                    permission.setUrl("/api/async/test/" + i);
                    permission.setMethod(i % 2 == 0 ? "GET" : "POST");
                    permission.setSortOrder(i);
                    permission.setEnabled(true);
                    return permission;
                })
                .collect(Collectors.toList());

        return permissionService.batchAddPermissionsAsync(permissions)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? String.format("成功异步批量创建 %d 个权限", count) : "异步批量创建失败",
                        "count", success ? count : 0,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 分页查询权限
     */
    @GetMapping
    public Map<String, Object> getPermissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("分页查询权限 - 页码: {}, 大小: {}", page, size);

        Page<Permission> permissionPage = permissionService.getPermissionsByPage(page, size);

        return Map.of(
                "success", true,
                "data", permissionPage.getRecords(),
                "pagination", Map.of(
                        "page", permissionPage.getCurrent(),
                        "size", permissionPage.getSize(),
                        "total", permissionPage.getTotal(),
                        "pages", permissionPage.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步分页查询权限 - 使用虚拟线程
     */
    @GetMapping("/async")
    public CompletableFuture<Map<String, Object>> getPermissionsAsync(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("异步分页查询权限 - 页码: {}, 大小: {}", page, size);

        return permissionService.getPermissionsByPageAsync(page, size)
                .thenApply(permissionPage -> Map.of(
                        "success", true,
                        "data", permissionPage.getRecords(),
                        "pagination", Map.of(
                                "page", permissionPage.getCurrent(),
                                "size", permissionPage.getSize(),
                                "total", permissionPage.getTotal(),
                                "pages", permissionPage.getPages()
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 获取权限统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getPermissionStats() {
        log.info("获取权限统计信息");

        long totalCount = permissionService.countPermissions();

        return Map.of(
                "success", true,
                "stats", Map.of(
                        "totalPermissions", totalCount
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步获取权限统计信息 - 使用虚拟线程
     */
    @GetMapping("/stats/async")
    public CompletableFuture<Map<String, Object>> getPermissionStatsAsync() {
        log.info("异步获取权限统计信息");

        return permissionService.countPermissionsAsync()
                .thenApply(totalCount -> Map.of(
                        "success", true,
                        "stats", Map.of(
                                "totalPermissions", totalCount
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 根据编码查询权限
     */
    @GetMapping("/code/{code}")
    public Map<String, Object> getPermissionByCode(@PathVariable String code) {
        log.info("根据编码查询权限 - 编码: {}", code);

        Permission permission = permissionService.getPermissionByCode(code);

        if (permission == null) {
            return Map.of(
                    "success", false,
                    "message", "权限不存在",
                    "timestamp", System.currentTimeMillis()
            );
        }

        return Map.of(
                "success", true,
                "data", permission,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 根据类型查询权限列表
     */
    @GetMapping("/type/{type}")
    public Map<String, Object> getPermissionsByType(@PathVariable String type) {
        log.info("根据类型查询权限列表 - 类型: {}", type);

        List<Permission> permissions = permissionService.getPermissionsByType(type);

        return Map.of(
                "success", true,
                "data", permissions,
                "count", permissions.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 根据父级ID查询权限列表
     */
    @GetMapping("/parent/{parentId}")
    public Map<String, Object> getPermissionsByParentId(@PathVariable Long parentId) {
        log.info("根据父级ID查询权限列表 - 父级ID: {}", parentId);

        List<Permission> permissions = permissionService.getPermissionsByParentId(parentId);

        return Map.of(
                "success", true,
                "data", permissions,
                "count", permissions.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 根据用户ID查询权限列表
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getPermissionsByUserId(@PathVariable Long userId) {
        log.info("根据用户ID查询权限列表 - 用户ID: {}", userId);

        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);

        return Map.of(
                "success", true,
                "data", permissions,
                "count", permissions.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 根据角色ID查询权限列表
     */
    @GetMapping("/role/{roleId}")
    public Map<String, Object> getPermissionsByRoleId(@PathVariable Long roleId) {
        log.info("根据角色ID查询权限列表 - 角色ID: {}", roleId);

        List<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);

        return Map.of(
                "success", true,
                "data", permissions,
                "count", permissions.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 检查用户是否拥有某个权限
     */
    @GetMapping("/check")
    public Map<String, Object> checkUserHasPermission(
            @RequestParam Long userId,
            @RequestParam String permissionCode) {
        log.info("检查用户是否拥有某个权限 - 用户ID: {}, 权限编码: {}", userId, permissionCode);

        boolean hasPermission = permissionService.userHasPermission(userId, permissionCode);

        return Map.of(
                "success", true,
                "hasPermission", hasPermission,
                "message", hasPermission ? "用户拥有该权限" : "用户不拥有该权限",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 检查用户是否拥有某个URL和方法的权限
     */
    @GetMapping("/check-url")
    public Map<String, Object> checkUserHasUrlPermission(
            @RequestParam Long userId,
            @RequestParam String url,
            @RequestParam String method) {
        log.info("检查用户是否拥有某个URL和方法的权限 - 用户ID: {}, URL: {}, 方法: {}", userId, url, method);

        boolean hasPermission = permissionService.userHasUrlPermission(userId, url, method);

        return Map.of(
                "success", true,
                "hasPermission", hasPermission,
                "message", hasPermission ? "用户拥有该URL权限" : "用户不拥有该URL权限",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.info("权限服务健康检查");

        try {
            long count = permissionService.countPermissions();
            return Map.of(
                    "status", "UP",
                    "database", "PostgreSQL with MyBatis Plus",
                    "permissionCount", count,
                    "message", "权限服务运行正常",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("权限服务健康检查失败", e);
            return Map.of(
                    "status", "DOWN",
                    "database", "PostgreSQL with MyBatis Plus",
                    "error", e.getMessage(),
                    "message", "权限服务健康检查失败",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}