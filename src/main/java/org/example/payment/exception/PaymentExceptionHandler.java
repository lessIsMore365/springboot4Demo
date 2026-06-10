package org.example.payment.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "org.example.controller")
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentIdempotentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> handleIdempotent(PaymentIdempotentException e) {
        return Map.of("success", true, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentRateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimit(PaymentRateLimitException e, HttpServletResponse response) {
        response.setHeader("Retry-After", "1");
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(PaymentNotFoundException e) {
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentStatusException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleStatus(PaymentStatusException e) {
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentConfigException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConfig(PaymentConfigException e) {
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentSignatureException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleSignature(PaymentSignatureException e) {
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handlePayment(PaymentException e) {
        log.error("支付异常 [{}]: {}", e.getErrorCode(), e.getMessage());
        return Map.of("success", false, "message", e.getMessage(),
                "errorCode", e.getErrorCode(), "timestamp", System.currentTimeMillis());
    }
}
