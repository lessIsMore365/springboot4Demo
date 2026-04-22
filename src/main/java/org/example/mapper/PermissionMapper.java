package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.Permission;

/**
 * 权限Mapper接口
 * 继承MyBatis Plus的BaseMapper，获得基础的CRUD方法
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 自定义查询方法示例
     * 可以根据需要添加自定义的查询方法
     */

    /**
     * 根据权限编码查询权限
     * 注意：这个方法需要在对应的XML文件中实现
     * @param code 权限编码
     * @return 权限对象
     */
    // Permission selectByCode(String code);

    /**
     * 根据角色ID查询权限列表
     * 注意：这个方法需要在对应的XML文件中实现
     * @param roleId 角色ID
     * @return 权限列表
     */
    // List<Permission> selectPermissionsByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限列表
     * 注意：这个方法需要在对应的XML文件中实现
     * @param userId 用户ID
     * @return 权限列表
     */
    // List<Permission> selectPermissionsByUserId(Long userId);
}