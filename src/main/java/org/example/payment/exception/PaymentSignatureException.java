package org.example.payment.exception;

public class PaymentSignatureException extends PaymentException {
    public PaymentSignatureException(String message) {
        super("PAYMENT_SIGNATURE_INVALID", message);
    }
}
