package org.example.payment.exception;

public class PaymentConfigException extends PaymentException {
    public PaymentConfigException(String message) {
        super("PAYMENT_CONFIG_INVALID", message);
    }
}
