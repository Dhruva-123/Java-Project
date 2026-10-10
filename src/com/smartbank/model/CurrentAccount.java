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
import java.util.ArrayList;
import java.util.List;

public class CurrentAccount extends Account {
    private BigDecimal overdraftLimit;
    private BigDecimal monthlyCharge;

    public CurrentAccount(String accountNumber, String customerId, BigDecimal initialBalance, LocalDate openDate, String status) {
        super(accountNumber, customerId, initialBalance, openDate, status);
        this.overdraftLimit = Config.OVERDRAFT_LIMIT;
        this.monthlyCharge = Config.CURRENT_MONTHLY_CHARGE;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public BigDecimal getMonthlyCharge() {
        return monthlyCharge;
    }

    public void setMonthlyCharge(BigDecimal monthlyCharge) {
        this.monthlyCharge = monthlyCharge;
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }

    @Override
    public void withdraw(BigDecimal amt) throws BankException {
        if (!isActive()) {
            throw new AccountInactiveException();
        }
        if (!Validator.isValidAmount(amt)) {
            throw new InvalidAmountException();
        }
        BigDecimal allowedFloor = Config.OVERDRAFT_LIMIT.negate();
        if (getBalance().subtract(amt).compareTo(allowedFloor) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        debit(amt);
    }

    @Override
    public BigDecimal calculateInterest() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public List<Transaction> applyMonthlyCharges() {
        List<Transaction> created = new ArrayList<>();
        debit(Config.CURRENT_MONTHLY_CHARGE);
        Transaction feeTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                getAccountNumber(),
                "CHARGE",
                Config.CURRENT_MONTHLY_CHARGE,
                LocalDateTime.now(),
                "SUCCESS",
                getBalance(),
                "MONTHLY_MAINTENANCE"
        );
        addTransaction(feeTxn);
        created.add(feeTxn);

        if (getBalance().compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal overdrawn = getBalance().abs();
            BigDecimal odCharge = overdrawn.multiply(Config.OVERDRAFT_CHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
            if (odCharge.compareTo(BigDecimal.ZERO) > 0) {
                debit(odCharge);
                Transaction odTxn = new Transaction(
                        IdGenerator.nextTransactionId(),
                        getAccountNumber(),
                        "CHARGE",
                        odCharge,
                        LocalDateTime.now(),
                        "SUCCESS",
                        getBalance(),
                        "OVERDRAFT_FEE"
                );
                addTransaction(odTxn);
                created.add(odTxn);
            }
        }
        return created;
    }
}
