package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dict_data")
public class SysDictData {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 字典类型编码（关联 sys_dict_type.dict_type） */
    private String dictType;

    /** 字典标签（展示值） */
    private String dictLabel;

    /** 字典键值（存储值） */
    private String dictValue;

    /** 排序 */
    private Integer dictSort;

    /** 样式属性（CSS类名） */
    private String cssClass;

    /** 表格回显样式 */
    private String listClass;

    /** 是否默认 */
    private String isDefault;

    /** 状态（0=正常 1=停用） */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
