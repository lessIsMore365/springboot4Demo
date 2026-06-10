package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_event_log")
public class PaymentEventLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String operatorIp;
    private String eventData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
