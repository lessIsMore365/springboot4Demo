package org.example.payment.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功事件 — 发布后被 WebhookNotificationListener 异步消费
 * 用于通知下游系统（订单服务、履约系统等）
 */
public class PaymentSuccessEvent extends ApplicationEvent {

    private final String orderNo;
    private final String paymentMethod;
    private final BigDecimal amount;
    private final String tradeNo;
    private final LocalDateTime paidTime;

    public PaymentSuccessEvent(Object source, String orderNo, String paymentMethod,
                               BigDecimal amount, String tradeNo, LocalDateTime paidTime) {
        super(source);
        this.orderNo = orderNo;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.tradeNo = tradeNo;
        this.paidTime = paidTime;
    }

    public String getOrderNo() { return orderNo; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getTradeNo() { return tradeNo; }
    public LocalDateTime getPaidTime() { return paidTime; }

    public java.util.Map<String, Object> toEventData() {
        return java.util.Map.of(
                "event", "PAYMENT.SUCCESS",
                "orderNo", orderNo,
                "paymentMethod", paymentMethod,
                "amount", amount.toString(),
                "tradeNo", tradeNo != null ? tradeNo : "",
                "paidTime", paidTime != null ? paidTime.toString() : ""
        );
    }
}
