package org.example.payment.exception;

public class PaymentStatusException extends PaymentException {
    public PaymentStatusException(String message) {
        super("PAYMENT_STATUS_INVALID", message);
    }
}
