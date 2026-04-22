package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.service.RoleService;
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
 * 角色控制器
 * 演示MyBatis Plus在虚拟线程环境下的使用
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 创建角色（需要管理员权限）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Map<String, Object> createRole(@RequestBody Role role) {
        log.info("创建角色请求: {}", role.getCode());

        boolean success = roleService.addRole(role);

        return Map.of(
                "success", success,
                "message", success ? "角色创建成功" : "角色创建失败",
                "data", success ? role : null,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步创建角色 - 使用虚拟线程
     */
    @PostMapping("/async")
    public CompletableFuture<Map<String, Object>> createRoleAsync(@RequestBody Role role) {
        log.info("异步创建角色请求: {}", role.getCode());

        return roleService.addRoleAsync(role)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? "角色异步创建成功" : "角色异步创建失败",
                        "data", success ? role : null,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 批量创建测试角色
     */
    @PostMapping("/batch")
    public Map<String, Object> batchCreateRoles(@RequestParam(defaultValue = "5") int count) {
        log.info("批量创建 {} 个测试角色", count);

        List<Role> roles = IntStream.range(0, count)
                .mapToObj(i -> {
                    Role role = new Role();
                    role.setName("测试角色_" + i);
                    role.setCode("ROLE_TEST_" + i);
                    role.setDescription("批量创建测试角色 #" + i);
                    role.setSortOrder(i);
                    role.setEnabled(true);
                    return role;
                })
                .collect(Collectors.toList());

        boolean success = roleService.batchAddRoles(roles);

        return Map.of(
                "success", success,
                "message", success ? String.format("成功批量创建 %d 个角色", count) : "批量创建失败",
                "count", success ? count : 0,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步批量创建角色 - 使用虚拟线程
     */
    @PostMapping("/batch/async")
    public CompletableFuture<Map<String, Object>> batchCreateRolesAsync(@RequestParam(defaultValue = "5") int count) {
        log.info("异步批量创建 {} 个测试角色", count);

        List<Role> roles = IntStream.range(0, count)
                .mapToObj(i -> {
                    Role role = new Role();
                    role.setName("异步测试角色_" + i);
                    role.setCode("ROLE_ASYNC_TEST_" + i);
                    role.setDescription("异步批量创建测试角色 #" + i);
                    role.setSortOrder(i);
                    role.setEnabled(true);
                    return role;
                })
                .collect(Collectors.toList());

        return roleService.batchAddRolesAsync(roles)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? String.format("成功异步批量创建 %d 个角色", count) : "异步批量创建失败",
                        "count", success ? count : 0,
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 分页查询角色
     */
    @GetMapping
    public Map<String, Object> getRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("分页查询角色 - 页码: {}, 大小: {}", page, size);

        Page<Role> rolePage = roleService.getRolesByPage(page, size);

        return Map.of(
                "success", true,
                "data", rolePage.getRecords(),
                "pagination", Map.of(
                        "page", rolePage.getCurrent(),
                        "size", rolePage.getSize(),
                        "total", rolePage.getTotal(),
                        "pages", rolePage.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步分页查询角色 - 使用虚拟线程
     */
    @GetMapping("/async")
    public CompletableFuture<Map<String, Object>> getRolesAsync(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("异步分页查询角色 - 页码: {}, 大小: {}", page, size);

        return roleService.getRolesByPageAsync(page, size)
                .thenApply(rolePage -> Map.of(
                        "success", true,
                        "data", rolePage.getRecords(),
                        "pagination", Map.of(
                                "page", rolePage.getCurrent(),
                                "size", rolePage.getSize(),
                                "total", rolePage.getTotal(),
                                "pages", rolePage.getPages()
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 获取角色统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getRoleStats() {
        log.info("获取角色统计信息");

        long totalCount = roleService.countRoles();

        return Map.of(
                "success", true,
                "stats", Map.of(
                        "totalRoles", totalCount
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步获取角色统计信息 - 使用虚拟线程
     */
    @GetMapping("/stats/async")
    public CompletableFuture<Map<String, Object>> getRoleStatsAsync() {
        log.info("异步获取角色统计信息");

        return roleService.countRolesAsync()
                .thenApply(totalCount -> Map.of(
                        "success", true,
                        "stats", Map.of(
                                "totalRoles", totalCount
                        ),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 根据编码查询角色
     */
    @GetMapping("/code/{code}")
    public Map<String, Object> getRoleByCode(@PathVariable String code) {
        log.info("根据编码查询角色 - 编码: {}", code);

        Role role = roleService.getRoleByCode(code);

        if (role == null) {
            return Map.of(
                    "success", false,
                    "message", "角色不存在",
                    "timestamp", System.currentTimeMillis()
            );
        }

        return Map.of(
                "success", true,
                "data", role,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 为用户分配角色
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public Map<String, Object> assignRolesToUser(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) request.get("roleIds");
        log.info("为用户分配角色 - 用户ID: {}, 角色ID列表: {}", userId, roleIds);

        boolean success = roleService.assignRolesToUser(userId, roleIds);

        return Map.of(
                "success", success,
                "message", success ? "角色分配成功" : "角色分配失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 异步为用户分配角色 - 使用虚拟线程
     */
    @PostMapping("/assign/async")
    public CompletableFuture<Map<String, Object>> assignRolesToUserAsync(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) request.get("roleIds");
        log.info("异步为用户分配角色 - 用户ID: {}, 角色ID列表: {}", userId, roleIds);

        return roleService.assignRolesToUserAsync(userId, roleIds)
                .thenApply(success -> Map.of(
                        "success", success,
                        "message", success ? "角色异步分配成功" : "角色异步分配失败",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 获取用户的角色列表
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getRolesByUserId(@PathVariable Long userId) {
        log.info("获取用户的角色列表 - 用户ID: {}", userId);

        List<Role> roles = roleService.getRolesByUserId(userId);

        return Map.of(
                "success", true,
                "data", roles,
                "count", roles.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 检查用户是否拥有某个角色
     */
    @GetMapping("/check")
    public Map<String, Object> checkUserHasRole(
            @RequestParam Long userId,
            @RequestParam String roleCode) {
        log.info("检查用户是否拥有某个角色 - 用户ID: {}, 角色编码: {}", userId, roleCode);

        boolean hasRole = roleService.userHasRole(userId, roleCode);

        return Map.of(
                "success", true,
                "hasRole", hasRole,
                "message", hasRole ? "用户拥有该角色" : "用户不拥有该角色",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 为角色分配权限
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/permissions/assign")
    public Map<String, Object> assignPermissionsToRole(@RequestBody Map<String, Object> request) {
        Long roleId = Long.valueOf(request.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Long> permissionIds = (List<Long>) request.get("permissionIds");
        log.info("为角色分配权限 - 角色ID: {}, 权限ID列表: {}", roleId, permissionIds);

        boolean success = roleService.assignPermissionsToRole(roleId, permissionIds);

        return Map.of(
                "success", success,
                "message", success ? "权限分配成功" : "权限分配失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取角色的权限ID列表
     */
    @GetMapping("/{roleId}/permissions")
    public Map<String, Object> getRolePermissions(@PathVariable Long roleId) {
        log.info("获取角色的权限ID列表 - 角色ID: {}", roleId);

        List<Long> permissionIds = roleService.getPermissionIdsByRoleId(roleId);

        return Map.of(
                "success", true,
                "data", permissionIds,
                "count", permissionIds.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.info("角色服务健康检查");

        try {
            long count = roleService.countRoles();
            return Map.of(
                    "status", "UP",
                    "database", "PostgreSQL with MyBatis Plus",
                    "roleCount", count,
                    "message", "角色服务运行正常",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("角色服务健康检查失败", e);
            return Map.of(
                    "status", "DOWN",
                    "database", "PostgreSQL with MyBatis Plus",
                    "error", e.getMessage(),
                    "message", "角色服务健康检查失败",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}