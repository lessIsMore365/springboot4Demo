package org.example.payment.state;

import org.example.payment.exception.PaymentStatusException;

public class IllegalStateTransitionException extends PaymentStatusException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
