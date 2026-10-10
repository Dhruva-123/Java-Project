package com.smartbank.exception;

public class DataAccessException extends BankException {
    public DataAccessException() {
        super("System error, please try later");
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Throwable cause) {
        super("System error, please try later", cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
