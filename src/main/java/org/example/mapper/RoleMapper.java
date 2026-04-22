package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.Role;

/**
 * 角色Mapper接口
 * 继承MyBatis Plus的BaseMapper，获得基础的CRUD方法
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 自定义查询方法示例
     * 可以根据需要添加自定义的查询方法
     */

    /**
     * 根据角色编码查询角色
     * 注意：这个方法需要在对应的XML文件中实现
     * @param code 角色编码
     * @return 角色对象
     */
    // Role selectByCode(String code);

    /**
     * 根据用户ID查询角色列表
     * 注意：这个方法需要在对应的XML文件中实现
     * @param userId 用户ID
     * @return 角色列表
     */
    // List<Role> selectRolesByUserId(Long userId);
}