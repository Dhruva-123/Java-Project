package com.smartbank.config;

import java.math.BigDecimal;

public final class Config {
    private Config() {}

    public static final BigDecimal MIN_OPENING_DEPOSIT_SAVINGS = new BigDecimal("1000.00");
    public static final BigDecimal MIN_OPENING_DEPOSIT_CURRENT = new BigDecimal("5000.00");
    public static final BigDecimal MIN_BALANCE = new BigDecimal("1000.00");
    public static final BigDecimal SAVINGS_RATE = new BigDecimal("4.00");
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("10000.00");
    public static final BigDecimal OVERDRAFT_CHARGE_RATE = new BigDecimal("0.015");
    public static final BigDecimal CURRENT_MONTHLY_CHARGE = new BigDecimal("100.00");
    public static final int MIN_CUSTOMER_AGE = 18;

    public static final BigDecimal ATM_PER_TRANSACTION_LIMIT = new BigDecimal("10000.00");
    public static final BigDecimal ATM_DAILY_WITHDRAWAL_LIMIT = new BigDecimal("20000.00");
    public static final BigDecimal WITHDRAWAL_DENOMINATION = new BigDecimal("100.00");
    public static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("50000.00");
    public static final BigDecimal SINGLE_DEPOSIT_LIMIT = new BigDecimal("100000.00");

    public static final int PIN_LENGTH = 4;
    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final int ATM_SESSION_TIMEOUT_SECONDS = 60;
    public static final int CARD_VALIDITY_YEARS = 5;

    public static final String BANK_CODE = "523456";
    public static final long INITIAL_ACCOUNT_NUMBER = 1000000001L;
    public static final int INITIAL_CUSTOMER_ID_SEQ = 1001;

    public static final String DATA_DIR = "data";
    public static final String CUSTOMERS_FILE = "customers.csv";
    public static final String ACCOUNTS_FILE = "accounts.csv";
    public static final String CARDS_FILE = "cards.csv";
    public static final String TRANSACTIONS_FILE = "transactions.csv";
    public static final String ADMINS_FILE = "admins.csv";
    public static final String AUDIT_LOG_FILE = "audit_log.csv";
}
