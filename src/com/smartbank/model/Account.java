package com.smartbank.model;

import com.smartbank.config.Config;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.BankException;
import com.smartbank.exception.InvalidAmountException;
import com.smartbank.exception.LimitExceededException;
import com.smartbank.util.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Account implements Transactable, Printable {
    private final String accountNumber;
    private BigDecimal balance;
    private String customerId;
    private final LocalDate openDate;
    private String status;
    protected final List<Transaction> history = new ArrayList<>();

    public Account(String accountNumber, String customerId, BigDecimal initialBalance, LocalDate openDate, String status) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.openDate = openDate != null ? openDate : LocalDate.now();
        this.status = status != null ? status : "ACTIVE";
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    protected void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    @Override
    public void deposit(BigDecimal amt) throws BankException {
        if (!isActive()) {
            throw new AccountInactiveException();
        }
        if (!Validator.isValidAmount(amt)) {
            throw new InvalidAmountException();
        }
        if (amt.compareTo(Config.SINGLE_DEPOSIT_LIMIT) > 0) {
            throw new LimitExceededException("Transaction limit exceeded");
        }
        credit(amt);
    }

    @Override
    public abstract void withdraw(BigDecimal amt) throws BankException;

    public abstract BigDecimal calculateInterest();

    public abstract String getAccountType();

    public void debit(BigDecimal amt) {
        this.balance = this.balance.subtract(amt);
    }

    public void credit(BigDecimal amt) {
        this.balance = this.balance.add(amt);
    }

    public void reverseDebit(BigDecimal amt) {
        this.balance = this.balance.add(amt);
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            this.history.add(transaction);
        }
    }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Transaction> getMiniStatement() {
        int size = history.size();
        int fromIndex = Math.max(0, size - 10);
        return Collections.unmodifiableList(history.subList(fromIndex, size));
    }

    @Override
    public String printReceipt(Transaction transaction) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("       SMART BANK ATM RECEIPT           \n");
        sb.append("========================================\n");
        if (transaction != null) {
            sb.append("Transaction ID: ").append(transaction.getTransactionId()).append("\n");
            sb.append("Account Number: ").append(accountNumber).append("\n");
            sb.append("Type:           ").append(transaction.getType()).append("\n");
            sb.append("Amount:         Rs. ").append(transaction.getAmount().toPlainString()).append("\n");
            sb.append("Date & Time:    ").append(transaction.getDateTime() != null ? transaction.getDateTime().toString() : "").append("\n");
            sb.append("Status:         ").append(transaction.getStatus()).append("\n");
            sb.append("Balance After:  Rs. ").append(transaction.getBalanceAfter().toPlainString()).append("\n");
        } else {
            sb.append("Account Number: ").append(accountNumber).append("\n");
            sb.append("Current Balance:Rs. ").append(balance.toPlainString()).append("\n");
        }
        sb.append("========================================\n");
        sb.append("       Thank you for banking with us!   \n");
        sb.append("========================================\n");
        return sb.toString();
    }

    @Override
    public String printStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("                             ACCOUNT STATEMENT (MINI)                                   \n");
        sb.append("Account Number: ").append(accountNumber).append(" | Type: ").append(getAccountType()).append("\n");
        sb.append("Current Balance: Rs. ").append(balance.toPlainString()).append(" | Status: ").append(status).append("\n");
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-18s | %-19s | %-13s | %-12s | %-12s%n", "Txn ID", "Date Time", "Type", "Amount (Rs)", "Balance (Rs)"));
        sb.append("----------------------------------------------------------------------------------------\n");
        List<Transaction> mini = getMiniStatement();
        for (Transaction t : mini) {
            sb.append(String.format("%-18s | %-19s | %-13s | %12.2f | %12.2f%n",
                    t.getTransactionId(),
                    t.getDateTime() != null ? t.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "",
                    t.getType(),
                    t.getAmount() != null ? t.getAmount().doubleValue() : 0.0,
                    t.getBalanceAfter() != null ? t.getBalanceAfter().doubleValue() : 0.0));
        }
        sb.append("========================================================================================\n");
        return sb.toString();
    }

    public String toCsvLine() {
        return String.join(",",
                accountNumber != null ? accountNumber : "",
                customerId != null ? customerId : "",
                getAccountType(),
                balance != null ? balance.toPlainString() : "0.00",
                openDate != null ? openDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                status != null ? status : "");
    }
}
