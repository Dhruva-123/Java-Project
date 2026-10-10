package com.smartbank.exception;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException() {
        super("Insufficient funds");
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}
