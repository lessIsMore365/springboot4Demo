package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_webhook")
public class PaymentWebhook {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String webhookUrl;
    private String eventTypes;
    private String secret;
    private Boolean enabled;
    private Integer maxRetries;
    private Integer retryCount;
    private String lastStatus;
    private LocalDateTime lastCalledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
