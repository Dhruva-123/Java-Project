package com.smartbank.model;

import com.smartbank.config.Config;
import com.smartbank.exception.AccountLockedException;
import com.smartbank.exception.BankException;
import com.smartbank.exception.InvalidPinException;
import com.smartbank.util.SecurityUtil;
import com.smartbank.util.Validator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Card {
    private String cardNumber;
    private String accountNumber;
    private String pinHash;
    private LocalDate expiryDate;
    private int failedAttempts;
    private boolean blocked;

    public Card(String cardNumber, String accountNumber, String pinHash, LocalDate expiryDate, int failedAttempts, boolean blocked) {
        this.cardNumber = cardNumber;
        this.accountNumber = accountNumber;
        this.pinHash = pinHash;
        this.expiryDate = expiryDate;
        this.failedAttempts = failedAttempts;
        this.blocked = blocked;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean validatePin(String pin) throws BankException {
        if (blocked) {
            throw new AccountLockedException();
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            throw new BankException("Card is expired");
        }
        String hash = SecurityUtil.hash(pin);
        if (pinHash != null && pinHash.equalsIgnoreCase(hash)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= Config.MAX_FAILED_ATTEMPTS) {
            blocked = true;
            throw new AccountLockedException();
        }
        throw new InvalidPinException(Config.MAX_FAILED_ATTEMPTS - failedAttempts);
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
        this.failedAttempts = 0;
    }

    public void changePin(String oldPin, String newPin) throws BankException {
        validatePin(oldPin);
        if (oldPin != null && oldPin.equals(newPin)) {
            throw new BankException("New PIN must be different from old PIN");
        }
        if (!Validator.isValidPin(newPin)) {
            throw new BankException("PIN is too simple or invalid");
        }
        this.pinHash = SecurityUtil.hash(newPin);
    }

    public String toCsvLine() {
        return String.join(",",
                cardNumber != null ? cardNumber : "",
                accountNumber != null ? accountNumber : "",
                pinHash != null ? pinHash : "",
                expiryDate != null ? expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                String.valueOf(failedAttempts),
                String.valueOf(blocked));
    }

    public static Card fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            return null;
        }
        return new Card(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                LocalDate.parse(parts[3].trim(), DateTimeFormatter.ISO_LOCAL_DATE),
                Integer.parseInt(parts[4].trim()),
                Boolean.parseBoolean(parts[5].trim())
        );
    }
}
