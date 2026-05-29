package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_notify_log")
public class PaymentNotifyLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 支付方式: ALIPAY / WECHAT */
    private String paymentMethod;

    /** 解析出的订单号 */
    private String orderNo;

    /** 原始回调数据(JSON/Form) */
    private String notifyBody;

    /** 验签是否通过 */
    private Boolean signatureValid;

    /** 处理状态: RECEIVED/PROCESSED/FAILED/DUPLICATE/ORDER_NOT_FOUND/SIGN_INVALID */
    private String processStatus;

    /** 错误信息 */
    private String errorMsg;

    /** 回调来源 IP */
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
