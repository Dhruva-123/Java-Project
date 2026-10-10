package com.smartbank.exception;

public class ATMOutOfCashException extends BankException {
    public ATMOutOfCashException() {
        super("ATM cannot dispense this amount");
    }

    public ATMOutOfCashException(String message) {
        super(message);
    }
}
