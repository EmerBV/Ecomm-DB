package com.emerbv.ecommdb.exceptions;

public class StripeException extends RuntimeException {
    public StripeException(String message) {
        super(message);
    }

    public StripeException(String message, Throwable cause) {
        super(message, cause);
    }
}
