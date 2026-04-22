package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Permission;
import org.example.entity.RolePermission;
import org.example.entity.UserRole;
import org.example.mapper.PermissionMapper;
import org.example.mapper.RolePermissionMapper;
import org.example.mapper.UserRoleMapper;
import org.example.service.PermissionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 * 演示虚拟线程环境下的MyBatis Plus操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    /**
     * 添加权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPermission(Permission permission) {
        Thread currentThread = Thread.currentThread();
        log.info("添加权限 - 权限编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                permission.getCode(), currentThread, currentThread.isVirtual());

        return this.save(permission);
    }

    /**
     * 异步添加权限 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> addPermissionAsync(Permission permission) {
        Thread currentThread = Thread.currentThread();
        log.info("异步添加权限 - 权限编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                permission.getCode(), currentThread, currentThread.isVirtual());

        boolean result = this.save(permission);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量添加权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddPermissions(List<Permission> permissions) {
        Thread currentThread = Thread.currentThread();
        log.info("批量添加 {} 个权限 - 当前线程: {}, 是否虚拟线程: {}",
                permissions.size(), currentThread, currentThread.isVirtual());

        return this.saveBatch(permissions);
    }

    /**
     * 异步批量添加权限 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> batchAddPermissionsAsync(List<Permission> permissions) {
        Thread currentThread = Thread.currentThread();
        log.info("异步批量添加 {} 个权限 - 当前线程: {}, 是否虚拟线程: {}",
                permissions.size(), currentThread, currentThread.isVirtual());

        boolean result = this.saveBatch(permissions);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分页查询权限
     */
    @Override
    public Page<Permission> getPermissionsByPage(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("分页查询权限 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<Permission> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Permission::getSortOrder).orderByDesc(Permission::getCreateTime);
        return this.page(pageParam, queryWrapper);
    }

    /**
     * 异步分页查询权限 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<Permission>> getPermissionsByPageAsync(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("异步分页查询权限 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<Permission> result = this.getPermissionsByPage(page, size);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 根据权限编码查询权限
     */
    @Override
    public Permission getPermissionByCode(String code) {
        Thread currentThread = Thread.currentThread();
        log.info("根据权限编码查询权限 - 编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                code, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Permission::getCode, code);
        return this.getOne(queryWrapper);
    }

    /**
     * 根据权限类型查询权限列表
     */
    @Override
    public List<Permission> getPermissionsByType(String type) {
        Thread currentThread = Thread.currentThread();
        log.info("根据权限类型查询权限列表 - 类型: {}, 当前线程: {}, 是否虚拟线程: {}",
                type, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Permission::getType, type)
                .orderByAsc(Permission::getSortOrder);
        return this.list(queryWrapper);
    }

    /**
     * 根据父级权限ID查询子权限列表
     */
    @Override
    public List<Permission> getPermissionsByParentId(Long parentId) {
        Thread currentThread = Thread.currentThread();
        log.info("根据父级权限ID查询子权限列表 - 父级ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                parentId, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Permission::getParentId, parentId)
                .orderByAsc(Permission::getSortOrder);
        return this.list(queryWrapper);
    }

    /**
     * 统计权限数量
     */
    @Override
    public long countPermissions() {
        Thread currentThread = Thread.currentThread();
        log.info("统计权限数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        return this.count();
    }

    /**
     * 异步统计权限数量 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Long> countPermissionsAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步统计权限数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        long count = this.count();
        return CompletableFuture.completedFuture(count);
    }

    /**
     * 根据用户ID查询权限列表
     */
    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        Thread currentThread = Thread.currentThread();
        log.info("根据用户ID查询权限列表 - 用户ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, currentThread, currentThread.isVirtual());

        // 查询用户角色关联
        LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);

        if (CollectionUtils.isEmpty(userRoles)) {
            return new ArrayList<>();
        }

        // 提取角色ID列表
        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        // 查询角色权限关联
        LambdaQueryWrapper<RolePermission> rolePermissionWrapper = new LambdaQueryWrapper<>();
        rolePermissionWrapper.in(RolePermission::getRoleId, roleIds);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(rolePermissionWrapper);

        if (CollectionUtils.isEmpty(rolePermissions)) {
            return new ArrayList<>();
        }

        // 提取权限ID列表
        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());

        // 查询权限信息
        LambdaQueryWrapper<Permission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.in(Permission::getId, permissionIds)
                .orderByAsc(Permission::getSortOrder);
        return permissionMapper.selectList(permissionWrapper);
    }

    /**
     * 根据角色ID查询权限列表
     */
    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        Thread currentThread = Thread.currentThread();
        log.info("根据角色ID查询权限列表 - 角色ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                roleId, currentThread, currentThread.isVirtual());

        // 查询角色权限关联
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(rolePermissions)) {
            return new ArrayList<>();
        }

        // 提取权限ID列表
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 查询权限信息
        LambdaQueryWrapper<Permission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.in(Permission::getId, permissionIds)
                .orderByAsc(Permission::getSortOrder);
        return permissionMapper.selectList(permissionWrapper);
    }

    /**
     * 检查用户是否拥有某个权限
     */
    @Override
    public boolean userHasPermission(Long userId, String permissionCode) {
        Thread currentThread = Thread.currentThread();
        log.info("检查用户是否拥有某个权限 - 用户ID: {}, 权限编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, permissionCode, currentThread, currentThread.isVirtual());

        // 查询权限
        Permission permission = this.getPermissionByCode(permissionCode);
        if (permission == null) {
            return false;
        }

        // 获取用户的所有权限
        List<Permission> userPermissions = this.getPermissionsByUserId(userId);
        return userPermissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    /**
     * 检查用户是否拥有某个URL和方法的权限
     */
    @Override
    public boolean userHasUrlPermission(Long userId, String url, String method) {
        Thread currentThread = Thread.currentThread();
        log.info("检查用户是否拥有某个URL和方法的权限 - 用户ID: {}, URL: {}, 方法: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, url, method, currentThread, currentThread.isVirtual());

        // 获取用户的所有权限
        List<Permission> userPermissions = this.getPermissionsByUserId(userId);

        return userPermissions.stream()
                .anyMatch(p -> {
                    boolean urlMatches = p.getUrl() != null && (p.getUrl().equals(url) || matchUrlPattern(p.getUrl(), url));
                    boolean methodMatches = p.getMethod() == null || p.getMethod().equalsIgnoreCase("ALL") || p.getMethod().equalsIgnoreCase(method);
                    return urlMatches && methodMatches;
                });
    }

    /**
     * 简单的URL模式匹配（支持通配符*）
     */
    private boolean matchUrlPattern(String pattern, String url) {
        if (pattern.equals(url)) {
            return true;
        }
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return url.matches(regex);
        }
        return false;
    }
}