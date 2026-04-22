package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.UserRole;
import org.example.entity.RolePermission;
import org.example.mapper.RoleMapper;
import org.example.mapper.UserRoleMapper;
import org.example.mapper.RolePermissionMapper;
import org.example.service.RoleService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * 演示虚拟线程环境下的MyBatis Plus操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 添加角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(Role role) {
        Thread currentThread = Thread.currentThread();
        log.info("添加角色 - 角色编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                role.getCode(), currentThread, currentThread.isVirtual());

        return this.save(role);
    }

    /**
     * 异步添加角色 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> addRoleAsync(Role role) {
        Thread currentThread = Thread.currentThread();
        log.info("异步添加角色 - 角色编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                role.getCode(), currentThread, currentThread.isVirtual());

        boolean result = this.save(role);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量添加角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddRoles(List<Role> roles) {
        Thread currentThread = Thread.currentThread();
        log.info("批量添加 {} 个角色 - 当前线程: {}, 是否虚拟线程: {}",
                roles.size(), currentThread, currentThread.isVirtual());

        return this.saveBatch(roles);
    }

    /**
     * 异步批量添加角色 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> batchAddRolesAsync(List<Role> roles) {
        Thread currentThread = Thread.currentThread();
        log.info("异步批量添加 {} 个角色 - 当前线程: {}, 是否虚拟线程: {}",
                roles.size(), currentThread, currentThread.isVirtual());

        boolean result = this.saveBatch(roles);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分页查询角色
     */
    @Override
    public Page<Role> getRolesByPage(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("分页查询角色 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<Role> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Role::getSortOrder).orderByDesc(Role::getCreateTime);
        return this.page(pageParam, queryWrapper);
    }

    /**
     * 异步分页查询角色 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<Role>> getRolesByPageAsync(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("异步分页查询角色 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<Role> result = this.getRolesByPage(page, size);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 根据角色编码查询角色
     */
    @Override
    public Role getRoleByCode(String code) {
        Thread currentThread = Thread.currentThread();
        log.info("根据角色编码查询角色 - 编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                code, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getCode, code);
        return this.getOne(queryWrapper);
    }

    /**
     * 根据角色名称查询角色列表
     */
    @Override
    public List<Role> getRolesByName(String name) {
        Thread currentThread = Thread.currentThread();
        log.info("根据角色名称查询角色列表 - 名称: {}, 当前线程: {}, 是否虚拟线程: {}",
                name, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Role::getName, name);
        return this.list(queryWrapper);
    }

    /**
     * 统计角色数量
     */
    @Override
    public long countRoles() {
        Thread currentThread = Thread.currentThread();
        log.info("统计角色数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        return this.count();
    }

    /**
     * 异步统计角色数量 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Long> countRolesAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步统计角色数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        long count = this.count();
        return CompletableFuture.completedFuture(count);
    }

    /**
     * 为用户分配角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        Thread currentThread = Thread.currentThread();
        log.info("为用户分配角色 - 用户ID: {}, 角色ID列表: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, roleIds, currentThread, currentThread.isVirtual());

        if (CollectionUtils.isEmpty(roleIds)) {
            return true;
        }

        // 删除用户现有的角色关联
        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, userId);
        userRoleMapper.delete(deleteWrapper);

        // 插入新的角色关联
        List<UserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        if (userRoles.isEmpty()) {
            return true;
        }
        int inserted = 0;
        for (UserRole userRole : userRoles) {
            inserted += userRoleMapper.insert(userRole);
        }
        return inserted > 0;
    }

    /**
     * 异步为用户分配角色 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> assignRolesToUserAsync(Long userId, List<Long> roleIds) {
        Thread currentThread = Thread.currentThread();
        log.info("异步为用户分配角色 - 用户ID: {}, 角色ID列表: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, roleIds, currentThread, currentThread.isVirtual());

        boolean result = this.assignRolesToUser(userId, roleIds);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 移除用户的角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRolesFromUser(Long userId, List<Long> roleIds) {
        Thread currentThread = Thread.currentThread();
        log.info("移除用户的角色 - 用户ID: {}, 角色ID列表: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, roleIds, currentThread, currentThread.isVirtual());

        if (CollectionUtils.isEmpty(roleIds)) {
            return true;
        }

        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, userId)
                .in(UserRole::getRoleId, roleIds);
        int deleted = userRoleMapper.delete(deleteWrapper);
        return deleted > 0;
    }

    /**
     * 获取用户的角色列表
     */
    @Override
    public List<Role> getRolesByUserId(Long userId) {
        Thread currentThread = Thread.currentThread();
        log.info("获取用户的角色列表 - 用户ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, currentThread, currentThread.isVirtual());

        // 查询用户角色关联
        LambdaQueryWrapper<UserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);

        if (CollectionUtils.isEmpty(userRoles)) {
            return new ArrayList<>();
        }

        // 提取角色ID列表
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        // 查询角色信息
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(Role::getId, roleIds)
                .orderByAsc(Role::getSortOrder);
        return roleMapper.selectList(roleWrapper);
    }

    /**
     * 为角色分配权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        Thread currentThread = Thread.currentThread();
        log.info("为角色分配权限 - 角色ID: {}, 权限ID列表: {}, 当前线程: {}, 是否虚拟线程: {}",
                roleId, permissionIds, currentThread, currentThread.isVirtual());

        if (CollectionUtils.isEmpty(permissionIds)) {
            return true;
        }

        // 删除角色现有的权限关联
        LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(deleteWrapper);

        // 插入新的权限关联
        List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        if (rolePermissions.isEmpty()) {
            return true;
        }
        int inserted = 0;
        for (RolePermission rolePermission : rolePermissions) {
            inserted += rolePermissionMapper.insert(rolePermission);
        }
        return inserted > 0;
    }

    /**
     * 移除角色的权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermissionsFromRole(Long roleId, List<Long> permissionIds) {
        Thread currentThread = Thread.currentThread();
        log.info("移除角色的权限 - 角色ID: {}, 权限ID列表: {}, 当前线程: {}, 是否虚拟线程: {}",
                roleId, permissionIds, currentThread, currentThread.isVirtual());

        if (CollectionUtils.isEmpty(permissionIds)) {
            return true;
        }

        LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RolePermission::getRoleId, roleId)
                .in(RolePermission::getPermissionId, permissionIds);
        int deleted = rolePermissionMapper.delete(deleteWrapper);
        return deleted > 0;
    }

    /**
     * 获取角色的权限ID列表
     */
    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        Thread currentThread = Thread.currentThread();
        log.info("获取角色的权限ID列表 - 角色ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                roleId, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);

        return rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否拥有某个角色
     */
    @Override
    public boolean userHasRole(Long userId, String roleCode) {
        Thread currentThread = Thread.currentThread();
        log.info("检查用户是否拥有某个角色 - 用户ID: {}, 角色编码: {}, 当前线程: {}, 是否虚拟线程: {}",
                userId, roleCode, currentThread, currentThread.isVirtual());

        // 查询角色
        Role role = this.getRoleByCode(roleCode);
        if (role == null) {
            return false;
        }

        // 检查用户角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, role.getId());
        return userRoleMapper.selectCount(wrapper) > 0;
    }
}