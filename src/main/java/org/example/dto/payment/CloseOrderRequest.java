package org.example.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record CloseOrderRequest(
        @NotBlank(message = "订单号(orderNo)不能为空")
        String orderNo
) {}
