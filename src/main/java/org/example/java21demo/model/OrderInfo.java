package org.example.java21demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息 - 用于 Structured Concurrency 演示
 */
public record OrderInfo(Long orderId, BigDecimal amount, String status, LocalDateTime createTime) {
}
