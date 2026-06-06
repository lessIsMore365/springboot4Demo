package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 父菜单ID，0为根节点 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 菜单图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder = 0;

    /** 菜单类型: M=目录, C=菜单, F=按钮 */
    private String menuType;

    /** 权限标识（如 user:list） */
    private String permission;

    /** 是否可见: 0=显示, 1=隐藏 */
    private Integer visible = 0;

    /** 状态: 0=启用, 1=停用 */
    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
