package com.smartbank.exception;

public class TransferFailedException extends BankException {
    public TransferFailedException() {
        super("Transfer failed, no money deducted");
    }

    public TransferFailedException(String message) {
        super(message);
    }

    public TransferFailedException(Throwable cause) {
        super("Transfer failed, no money deducted", cause);
    }

    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
