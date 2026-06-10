package org.example.payment.exception;

public class PaymentRateLimitException extends PaymentException {
    public PaymentRateLimitException(String message) {
        super("PAYMENT_RATE_LIMITED", message);
    }
}
