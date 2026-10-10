package com.smartbank.exception;

public class LimitExceededException extends BankException {
    public LimitExceededException() {
        super("Transaction limit exceeded");
    }

    public LimitExceededException(String message) {
        super(message);
    }
}
