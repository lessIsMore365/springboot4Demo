package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 表示用户和角色的多对多关系
 */
@Data
@TableName("sys_user_role")
public class UserRole {

    /**
     * 主键ID
     * 使用MyBatis Plus的雪花算法生成ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

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
}