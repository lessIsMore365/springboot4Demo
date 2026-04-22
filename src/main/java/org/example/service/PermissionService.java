package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Permission;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 权限服务接口
 * 演示虚拟线程环境下的权限管理操作
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 添加权限
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean addPermission(Permission permission);

    /**
     * 异步添加权限 - 使用虚拟线程
     * @param permission 权限信息
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> addPermissionAsync(Permission permission);

    /**
     * 批量添加权限
     * @param permissions 权限列表
     * @return 是否成功
     */
    boolean batchAddPermissions(List<Permission> permissions);

    /**
     * 异步批量添加权限 - 使用虚拟线程
     * @param permissions 权限列表
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> batchAddPermissionsAsync(List<Permission> permissions);

    /**
     * 分页查询权限
     * @param page 页码
     * @param size 每页大小
     * @return 权限分页数据
     */
    Page<Permission> getPermissionsByPage(int page, int size);

    /**
     * 异步分页查询权限 - 使用虚拟线程
     * @param page 页码
     * @param size 每页大小
     * @return CompletableFuture
     */
    CompletableFuture<Page<Permission>> getPermissionsByPageAsync(int page, int size);

    /**
     * 根据权限编码查询权限
     * @param code 权限编码
     * @return 权限
     */
    Permission getPermissionByCode(String code);

    /**
     * 根据权限类型查询权限列表
     * @param type 权限类型
     * @return 权限列表
     */
    List<Permission> getPermissionsByType(String type);

    /**
     * 根据父级权限ID查询子权限列表
     * @param parentId 父级权限ID
     * @return 子权限列表
     */
    List<Permission> getPermissionsByParentId(Long parentId);

    /**
     * 统计权限数量
     * @return 权限数量
     */
    long countPermissions();

    /**
     * 异步统计权限数量 - 使用虚拟线程
     * @return CompletableFuture
     */
    CompletableFuture<Long> countPermissionsAsync();

    /**
     * 根据用户ID查询权限列表
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);

    /**
     * 根据角色ID查询权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 检查用户是否拥有某个权限
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有
     */
    boolean userHasPermission(Long userId, String permissionCode);

    /**
     * 检查用户是否拥有某个URL和方法的权限
     * @param userId 用户ID
     * @param url 请求URL
     * @param method 请求方法
     * @return 是否拥有权限
     */
    boolean userHasUrlPermission(Long userId, String url, String method);
}