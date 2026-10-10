package com.smartbank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private String transactionId;
    private String accountNumber;
    private String type;
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private String status;
    private BigDecimal balanceAfter;
    private String referenceId;

    public Transaction(String transactionId, String accountNumber, String type, BigDecimal amount,
                       LocalDateTime dateTime, String status, BigDecimal balanceAfter, String referenceId) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.dateTime = dateTime;
        this.status = status;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDetails() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] %s | Type: %s | Amount: Rs. %.2f | Status: %s | Balance After: Rs. %.2f | Ref: %s",
                dateTime != null ? dateTime.format(fmt) : "",
                transactionId,
                type,
                amount != null ? amount.doubleValue() : 0.0,
                status,
                balanceAfter != null ? balanceAfter.doubleValue() : 0.0,
                referenceId != null ? referenceId : "N/A");
    }

    public String toCsvLine() {
        return String.join(",",
                transactionId != null ? transactionId : "",
                accountNumber != null ? accountNumber : "",
                type != null ? type : "",
                amount != null ? amount.toPlainString() : "0.00",
                dateTime != null ? dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
                status != null ? status : "",
                balanceAfter != null ? balanceAfter.toPlainString() : "0.00",
                referenceId != null ? referenceId : "");
    }

    public static Transaction fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 8) {
            return null;
        }
        return new Transaction(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                new BigDecimal(parts[3].trim()),
                LocalDateTime.parse(parts[4].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                parts[5].trim(),
                new BigDecimal(parts[6].trim()),
                parts[7].trim()
        );
    }
}
