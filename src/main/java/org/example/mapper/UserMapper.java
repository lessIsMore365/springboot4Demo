package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.User;

/**
 * 用户Mapper接口
 * 继承MyBatis Plus的BaseMapper，获得基础的CRUD方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 自定义查询方法示例
     * 可以根据需要添加自定义的查询方法
     */

    /**
     * 根据用户名查询用户
     * 注意：这个方法需要在对应的XML文件中实现
     * @param username 用户名
     * @return 用户对象
     */
    // User selectByUsername(String username);

    /**
     * 统计用户数量
     * 注意：这个方法需要在对应的XML文件中实现
     * @return 用户数量
     */
    // Long countUsers();
}