package com.smartbank.model;

import com.smartbank.config.Config;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.BankException;
import com.smartbank.exception.InsufficientFundsException;
import com.smartbank.exception.InvalidAmountException;
import com.smartbank.util.IdGenerator;
import com.smartbank.util.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SavingsAccount extends Account {
    private BigDecimal interestRate;
    private BigDecimal minimumBalance;

    public SavingsAccount(String accountNumber, String customerId, BigDecimal initialBalance, LocalDate openDate, String status) {
        super(accountNumber, customerId, initialBalance, openDate, status);
        this.interestRate = Config.SAVINGS_RATE;
        this.minimumBalance = Config.MIN_BALANCE;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(BigDecimal minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public void withdraw(BigDecimal amt) throws BankException {
        if (!isActive()) {
            throw new AccountInactiveException();
        }
        if (!Validator.isValidAmount(amt)) {
            throw new InvalidAmountException();
        }
        if (getBalance().subtract(amt).compareTo(Config.MIN_BALANCE) < 0) {
            throw new InsufficientFundsException("Minimum balance must be maintained");
        }
        debit(amt);
    }

    @Override
    public BigDecimal calculateInterest() {
        if (getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return getBalance().multiply(Config.SAVINGS_RATE)
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
    }

    public Transaction creditMonthlyInterest() {
        BigDecimal interest = calculateInterest();
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            credit(interest);
            Transaction t = new Transaction(
                    IdGenerator.nextTransactionId(),
                    getAccountNumber(),
                    "INTEREST",
                    interest,
                    LocalDateTime.now(),
                    "SUCCESS",
                    getBalance(),
                    null
            );
            addTransaction(t);
            return t;
        }
        return null;
    }
}
