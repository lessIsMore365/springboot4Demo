package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private String refundTradeNo;
    private BigDecimal refundAmount;
    private String reason;
    private String status;
    private String remoteRefundNo;
    private String operator;
    private String operatorIp;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
