package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作模块/标题 */
    private String title;

    /** 业务类型（INSERT/UPDATE/DELETE/...） */
    private String businessType;

    /** 请求方法名（类名.方法名） */
    private String method;

    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 操作人类别（MANAGE/MOBILE） */
    private String operatorType;

    /** 操作人员 */
    private String operName;

    /** 请求URL */
    private String operUrl;

    /** 请求IP */
    private String operIp;

    /** IP归属地 */
    private String operLocation;

    /** 请求参数 */
    private String operParam;

    /** 返回结果 */
    private String jsonResult;

    /** 状态 0=成功 1=失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
