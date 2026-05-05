package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商户订单号 */
    private String orderNo;

    /** 支付方式: ALIPAY / WECHAT */
    private String paymentMethod;

    /** 订单金额 */
    private BigDecimal amount;

    /** 商品标题 */
    private String subject;

    /** 商品描述 */
    private String body;

    /** 订单状态: PENDING/SUCCESS/CLOSED/REFUND */
    private String status;

    /** 支付平台交易号 */
    private String tradeNo;

    /** 买家标识 */
    private String buyerId;

    /** 支付完成时间 */
    private LocalDateTime paidTime;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 通知原始数据(JSON) */
    private String notifyData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
