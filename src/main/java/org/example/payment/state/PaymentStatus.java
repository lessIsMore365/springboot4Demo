package org.example.payment.state;

import java.util.Set;

/**
 * 支付订单状态机 — 定义合法状态转换
 * <pre>
 *   PENDING → SUCCESS / CLOSED
 *   SUCCESS → REFUND / PARTIAL_REFUND
 *   PARTIAL_REFUND → REFUND
 *   CLOSED / REFUND → 终态，不可转换
 * </pre>
 */
public enum PaymentStatus {
    PENDING(Set.of("SUCCESS", "CLOSED")),
    SUCCESS(Set.of("REFUND", "PARTIAL_REFUND")),
    PARTIAL_REFUND(Set.of("REFUND")),
    CLOSED(Set.of()),
    REFUND(Set.of());

    private final Set<String> allowedTargets;

    PaymentStatus(Set<String> allowedTargets) {
        this.allowedTargets = allowedTargets;
    }

    public boolean canTransitionTo(PaymentStatus target) {
        return allowedTargets.contains(target.name());
    }

    public void validateTransition(PaymentStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    String.format("非法状态转换: %s → %s", this.name(), target.name()));
        }
    }

    /** 从字符串解析，找不到时返回 null */
    public static PaymentStatus fromString(String status) {
        if (status == null) return null;
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
