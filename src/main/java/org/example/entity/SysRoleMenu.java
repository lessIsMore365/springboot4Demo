package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private Long menuId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
