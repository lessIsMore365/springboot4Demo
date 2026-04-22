package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Role;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 角色服务接口
 * 演示虚拟线程环境下的角色管理操作
 */
public interface RoleService extends IService<Role> {

    /**
     * 添加角色
     * @param role 角色信息
     * @return 是否成功
     */
    boolean addRole(Role role);

    /**
     * 异步添加角色 - 使用虚拟线程
     * @param role 角色信息
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> addRoleAsync(Role role);

    /**
     * 批量添加角色
     * @param roles 角色列表
     * @return 是否成功
     */
    boolean batchAddRoles(List<Role> roles);

    /**
     * 异步批量添加角色 - 使用虚拟线程
     * @param roles 角色列表
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> batchAddRolesAsync(List<Role> roles);

    /**
     * 分页查询角色
     * @param page 页码
     * @param size 每页大小
     * @return 角色分页数据
     */
    Page<Role> getRolesByPage(int page, int size);

    /**
     * 异步分页查询角色 - 使用虚拟线程
     * @param page 页码
     * @param size 每页大小
     * @return CompletableFuture
     */
    CompletableFuture<Page<Role>> getRolesByPageAsync(int page, int size);

    /**
     * 根据角色编码查询角色
     * @param code 角色编码
     * @return 角色
     */
    Role getRoleByCode(String code);

    /**
     * 根据角色名称查询角色列表
     * @param name 角色名称
     * @return 角色列表
     */
    List<Role> getRolesByName(String name);

    /**
     * 统计角色数量
     * @return 角色数量
     */
    long countRoles();

    /**
     * 异步统计角色数量 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<Long> countRolesAsync();

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 异步为用户分配角色 - 使用虚拟线程
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> assignRolesToUserAsync(Long userId, List<Long> roleIds);

    /**
     * 移除用户的角色
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean removeRolesFromUser(Long userId, List<Long> roleIds);

    /**
     * 获取用户的角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 为角色分配权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 移除角色的权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean removePermissionsFromRole(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的权限ID列表
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 检查用户是否拥有某个角色
     * @param userId 用户ID
     * @param roleCode 角色编码
     * @return 是否拥有
     */
    boolean userHasRole(Long userId, String roleCode);
}