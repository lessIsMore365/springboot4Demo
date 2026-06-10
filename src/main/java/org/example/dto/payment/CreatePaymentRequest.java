package org.example.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "商品标题(subject)不能为空")
        String subject,

        String body,

        @NotNull(message = "支付金额(amount)不能为空")
        @Positive(message = "支付金额必须大于0")
        BigDecimal amount,

        @NotBlank(message = "支付方式(paymentMethod)不能为空")
        String paymentMethod,

        String tradeType,

        String bizType,

        String remark,

        /** ISO 4217 币种代码，默认 CNY */
        String currency
) {}
