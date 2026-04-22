package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.UserRole;

/**
 * 用户角色关联Mapper接口
 * 继承MyBatis Plus的BaseMapper，获得基础的CRUD方法
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 自定义查询方法示例
     * 可以根据需要添加自定义的查询方法
     */

    /**
     * 根据用户ID删除所有角色关联
     * 注意：这个方法需要在对应的XML文件中实现
     * @param userId 用户ID
     * @return 删除的行数
     */
    // int deleteByUserId(Long userId);

    /**
     * 根据角色ID删除所有用户关联
     * 注意：这个方法需要在对应的XML文件中实现
     * @param roleId 角色ID
     * @return 删除的行数
     */
    // int deleteByRoleId(Long roleId);

    /**
     * 检查用户是否拥有某个角色
     * 注意：这个方法需要在对应的XML文件中实现
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否存在关联
     */
    // boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}