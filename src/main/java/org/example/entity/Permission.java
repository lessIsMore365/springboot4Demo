package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体类
 * 用于RBAC权限管理系统
 */
@Data
@TableName("sys_permission")
public class Permission {

    /**
     * 主键ID
     * 使用MyBatis Plus的雪花算法生成ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码（唯一标识）
     * 例如：user:create, user:read, user:update, user:delete
     */
    private String code;

    /**
     * 权限类型
     * MENU-菜单权限，BUTTON-按钮权限，API-接口权限，DATA-数据权限
     */
    private String type;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 请求URL（对于API权限）
     */
    private String url;

    /**
     * 请求方法（对于API权限）
     * GET, POST, PUT, DELETE, PATCH, ALL
     */
    private String method;

    /**
     * 父级权限ID（用于菜单层级）
     */
    private Long parentId;

    /**
     * 权限图标（用于菜单）
     */
    private String icon;

    /**
     * 排序字段
     */
    private Integer sortOrder = 0;

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
     * 权限状态
     * true-启用，false-禁用
     */
    private Boolean enabled = true;
}