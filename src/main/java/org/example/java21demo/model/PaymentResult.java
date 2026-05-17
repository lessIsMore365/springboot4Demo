package org.example.java21demo.model;

/**
 * 支付结果 - 用于 Structured Concurrency 演示
 */
public record PaymentResult(boolean success, String transactionId, String message) {
}
