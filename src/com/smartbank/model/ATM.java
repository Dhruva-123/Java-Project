package com.smartbank.model;

import com.smartbank.config.Config;
import com.smartbank.exception.ATMOutOfCashException;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.AccountLockedException;
import com.smartbank.exception.BankException;
import com.smartbank.exception.InsufficientFundsException;
import com.smartbank.exception.InvalidAmountException;
import com.smartbank.exception.LimitExceededException;
import com.smartbank.service.Bank;
import com.smartbank.util.IdGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ATM {
    private final String atmId;
    private final String location;
    private BigDecimal cashAvailable;
    private Card currentSessionCard;
    private Account currentSessionAccount;
    private Bank bank;

    public ATM(String atmId, String location, BigDecimal initialCash, Bank bank) {
        this.atmId = atmId;
        this.location = location;
        this.cashAvailable = initialCash != null ? initialCash : new BigDecimal("100000.00");
        this.bank = bank;
    }

    public String getAtmId() {
        return atmId;
    }

    public String getLocation() {
        return location;
    }

    public BigDecimal getCashAvailable() {
        return cashAvailable;
    }

    public void addCash(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.cashAvailable = this.cashAvailable.add(amount);
        }
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public Card getCurrentSessionCard() {
        return currentSessionCard;
    }

    public Account getCurrentSessionAccount() {
        return currentSessionAccount;
    }

    public boolean authenticate(String cardNumber, String pin) throws BankException {
        if (bank == null) {
            throw new BankException("ATM is not connected to bank system");
        }
        Card card = bank.findCard(cardNumber);
        card.validatePin(pin);
        Account account = bank.findAccount(card.getAccountNumber());
        if (!account.isActive()) {
            throw new AccountInactiveException();
        }
        this.currentSessionCard = card;
        this.currentSessionAccount = account;
        return true;
    }

    public Transaction dispenseCash(BigDecimal amount) throws BankException {
        if (currentSessionCard == null || currentSessionAccount == null) {
            throw new BankException("No active session");
        }
        if (currentSessionCard.isBlocked()) {
            throw new AccountLockedException();
        }
        if (!currentSessionAccount.isActive()) {
            throw new AccountInactiveException();
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.remainder(Config.WITHDRAWAL_DENOMINATION).compareTo(BigDecimal.ZERO) != 0) {
            logFailedTransaction("WITHDRAWAL", amount != null ? amount : BigDecimal.ZERO, "INVALID_AMOUNT");
            throw new InvalidAmountException();
        }

        if (amount.compareTo(Config.ATM_PER_TRANSACTION_LIMIT) > 0) {
            logFailedTransaction("WITHDRAWAL", amount, "PER_TXN_LIMIT_EXCEEDED");
            throw new LimitExceededException("Transaction limit exceeded");
        }

        BigDecimal todayWithdrawn = bank.getTodayWithdrawals(currentSessionAccount.getAccountNumber());
        if (todayWithdrawn.add(amount).compareTo(Config.ATM_DAILY_WITHDRAWAL_LIMIT) > 0) {
            logFailedTransaction("WITHDRAWAL", amount, "DAILY_LIMIT_EXCEEDED");
            throw new LimitExceededException("Transaction limit exceeded");
        }

        if (this.cashAvailable.compareTo(amount) < 0) {
            logFailedTransaction("WITHDRAWAL", amount, "ATM_OUT_OF_CASH");
            throw new ATMOutOfCashException();
        }

        try {
            currentSessionAccount.withdraw(amount);
        } catch (InsufficientFundsException e) {
            logFailedTransaction("WITHDRAWAL", amount, "INSUFFICIENT_FUNDS");
            throw e;
        }

        this.cashAvailable = this.cashAvailable.subtract(amount);

        Transaction successTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                currentSessionAccount.getAccountNumber(),
                "WITHDRAWAL",
                amount,
                LocalDateTime.now(),
                "SUCCESS",
                currentSessionAccount.getBalance(),
                "ATM_" + atmId
        );
        currentSessionAccount.addTransaction(successTxn);
        bank.recordSuccessfulWithdrawal(currentSessionAccount.getAccountNumber(), amount, successTxn);

        return successTxn;
    }

    private void logFailedTransaction(String type, BigDecimal amount, String reason) {
        Transaction failedTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                currentSessionAccount.getAccountNumber(),
                type,
                amount,
                LocalDateTime.now(),
                "FAILED",
                currentSessionAccount.getBalance(),
                reason
        );
        currentSessionAccount.addTransaction(failedTxn);
        bank.recordFailedTransaction(failedTxn);
    }

    public BigDecimal checkBalance() throws BankException {
        if (currentSessionAccount == null) {
            throw new BankException("No active session");
        }
        return currentSessionAccount.getBalance();
    }

    public String printReceipt(Transaction transaction) {
        if (currentSessionAccount == null) {
            return "No active session.";
        }
        return currentSessionAccount.printReceipt(transaction);
    }

    public void endSession() {
        this.currentSessionCard = null;
        this.currentSessionAccount = null;
    }
}
