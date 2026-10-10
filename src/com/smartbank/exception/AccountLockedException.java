package com.smartbank.exception;

public class AccountLockedException extends BankException {
    public AccountLockedException() {
        super("Card blocked, contact bank");
    }

    public AccountLockedException(String message) {
        super(message);
    }
}
