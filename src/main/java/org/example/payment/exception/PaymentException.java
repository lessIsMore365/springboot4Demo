package org.example.payment.exception;

/** 支付模块基础异常 */
public abstract class PaymentException extends RuntimeException {
    private final String errorCode;

    protected PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
