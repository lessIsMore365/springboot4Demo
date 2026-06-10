package org.example.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RefundRequest(
        @NotBlank(message = "订单号(orderNo)不能为空")
        String orderNo,

        @NotNull(message = "退款金额(amount)不能为空")
        @Positive(message = "退款金额必须大于0")
        BigDecimal amount,

        String reason
) {}
