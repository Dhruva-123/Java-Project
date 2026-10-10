package com.smartbank.model;

import com.smartbank.exception.BankException;
import java.math.BigDecimal;

public interface Transactable {
    void deposit(BigDecimal amt) throws BankException;
    void withdraw(BigDecimal amt) throws BankException;
}
