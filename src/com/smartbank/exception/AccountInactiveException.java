package com.smartbank.exception;

public class AccountInactiveException extends BankException {
    public AccountInactiveException() {
        super("Account is not active");
    }

    public AccountInactiveException(String message) {
        super(message);
    }
}
