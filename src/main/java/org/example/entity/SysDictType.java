package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dict_type")
public class SysDictType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 字典名称（如"支付方式"） */
    private String dictName;

    /** 字典类型编码（如"payment_method"） */
    private String dictType;

    /** 状态（0=正常 1=停用） */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
