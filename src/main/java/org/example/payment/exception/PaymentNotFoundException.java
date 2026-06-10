package org.example.payment.exception;

public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String message) {
        super("PAYMENT_ORDER_NOT_FOUND", message);
    }
}
