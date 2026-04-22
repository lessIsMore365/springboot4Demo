package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类 - MyBatis Plus 示例
 * 演示虚拟线程环境下的数据库操作
 */
@Data
@TableName("sys_user")
public class User {

    /**
     * 主键ID
     * 使用MyBatis Plus的雪花算法生成ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 创建时间
     * 使用MyBatis Plus自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 使用MyBatis Plus自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除字段
     * 0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 版本号，用于乐观锁
     */
    @Version
    private Integer version;

    /**
     * 备注
     */
    private String remark;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 角色列表，逗号分隔（例如："ROLE_USER,ROLE_ADMIN"）
     */
    private String roles;

    /**
     * 账户是否启用
     */
    private Boolean enabled = true;

    /**
     * 账户是否未锁定
     */
    private Boolean accountNonLocked = true;

    /**
     * 账户是否未过期
     */
    private Boolean accountNonExpired = true;

    /**
     * 凭证是否未过期
     */
    private Boolean credentialsNonExpired = true;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}