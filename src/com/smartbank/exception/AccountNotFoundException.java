package com.smartbank.exception;

public class AccountNotFoundException extends BankException {
    public AccountNotFoundException() {
        super("Account not found");
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
