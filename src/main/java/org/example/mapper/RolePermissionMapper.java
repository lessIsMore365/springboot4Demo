package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.RolePermission;

/**
 * 角色权限关联Mapper接口
 * 继承MyBatis Plus的BaseMapper，获得基础的CRUD方法
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 自定义查询方法示例
     * 可以根据需要添加自定义的查询方法
     */

    /**
     * 根据角色ID删除所有权限关联
     * 注意：这个方法需要在对应的XML文件中实现
     * @param roleId 角色ID
     * @return 删除的行数
     */
    // int deleteByRoleId(Long roleId);

    /**
     * 根据权限ID删除所有角色关联
     * 注意：这个方法需要在对应的XML文件中实现
     * @param permissionId 权限ID
     * @return 删除的行数
     */
    // int deleteByPermissionId(Long permissionId);

    /**
     * 检查角色是否拥有某个权限
     * 注意：这个方法需要在对应的XML文件中实现
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 是否存在关联
     */
    // boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}