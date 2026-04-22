package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类
 * 用于RBAC权限管理系统
 */
@Data
@TableName("sys_role")
public class Role {

    /**
     * 主键ID
     * 使用MyBatis Plus的雪花算法生成ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码（唯一标识）
     * 例如：ROLE_ADMIN, ROLE_USER
     */
    private String code;

    /**
     * 角色描述
     */
    private String description;

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
     * 角色状态
     * true-启用，false-禁用
     */
    private Boolean enabled = true;

    /**
     * 排序字段
     */
    private Integer sortOrder = 0;
}