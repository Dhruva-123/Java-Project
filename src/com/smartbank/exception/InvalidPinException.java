package com.smartbank.exception;

public class InvalidPinException extends BankException {
    private final int attemptsLeft;

    public InvalidPinException(int attemptsLeft) {
        super("Incorrect PIN, " + attemptsLeft + " attempts left");
        this.attemptsLeft = attemptsLeft;
    }

    public InvalidPinException(String message, int attemptsLeft) {
        super(message);
        this.attemptsLeft = attemptsLeft;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}
