package com.smartbank.exception;

public class InvalidAmountException extends BankException {
    public InvalidAmountException() {
        super("Please enter a valid amount");
    }

    public InvalidAmountException(String message) {
        super(message);
    }
}
