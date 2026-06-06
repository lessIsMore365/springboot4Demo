package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 父部门ID，0为根节点 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 排序号 */
    private Integer sortOrder = 0;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态: 0=启用, 1=停用 */
    @TableField(insertStrategy = FieldStrategy.NOT_NULL)
    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(insertStrategy = FieldStrategy.NOT_NULL)
    @TableLogic
    private Integer deleted = 0;

    @Version
    private Integer version;
}
