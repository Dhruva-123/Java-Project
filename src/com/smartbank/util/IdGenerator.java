package com.smartbank.util;

import com.smartbank.config.Config;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicInteger customerSeq = new AtomicInteger(Config.INITIAL_CUSTOMER_ID_SEQ);
    private static final AtomicLong accountSeq = new AtomicLong(Config.INITIAL_ACCOUNT_NUMBER);
    private static final AtomicLong cardSeq = new AtomicLong(1L);
    private static final AtomicInteger transactionSeq = new AtomicInteger(1);
    private static final AtomicInteger logSeq = new AtomicInteger(1);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdGenerator() {}

    public static synchronized void updateCustomerSeqIfGreater(int val) {
        if (val >= customerSeq.get()) {
            customerSeq.set(val + 1);
        }
    }

    public static synchronized void updateAccountSeqIfGreater(long val) {
        if (val >= accountSeq.get()) {
            accountSeq.set(val + 1);
        }
    }

    public static synchronized void updateCardSeqIfGreater(long val) {
        if (val >= cardSeq.get()) {
            cardSeq.set(val + 1);
        }
    }

    public static synchronized void updateTransactionSeqIfGreater(int val) {
        if (val >= transactionSeq.get()) {
            transactionSeq.set(val + 1);
        }
    }

    public static synchronized void updateLogSeqIfGreater(int val) {
        if (val >= logSeq.get()) {
            logSeq.set(val + 1);
        }
    }

    public static String nextCustomerId() {
        int id = customerSeq.getAndIncrement();
        return "C" + id;
    }

    public static String nextAccountNumber() {
        long acc = accountSeq.getAndIncrement();
        return String.valueOf(acc);
    }

    public static String nextCardNumber() {
        long seq = cardSeq.getAndIncrement();
        String prefix = Config.BANK_CODE;
        String middle = String.format("%09d", seq);
        String fifteen = prefix + middle;
        int checkDigit = calculateLuhnCheckDigit(fifteen);
        return fifteen + checkDigit;
    }

    public static String nextTransactionId() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        int seq = transactionSeq.getAndIncrement();
        return "TXN" + dateStr + String.format("%05d", seq);
    }

    public static String nextLogId() {
        int seq = logSeq.getAndIncrement();
        return "LOG" + String.format("%06d", seq);
    }

    public static int calculateLuhnCheckDigit(String fifteenDigits) {
        int sum = 0;
        for (int i = 14; i >= 0; i--) {
            int digit = Character.digit(fifteenDigits.charAt(i), 10);
            if ((14 - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
