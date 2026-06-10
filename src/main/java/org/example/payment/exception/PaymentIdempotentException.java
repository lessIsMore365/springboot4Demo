package org.example.payment.exception;

public class PaymentIdempotentException extends PaymentException {
    public PaymentIdempotentException(String message) {
        super("PAYMENT_IDEMPOTENT", message);
    }
}
