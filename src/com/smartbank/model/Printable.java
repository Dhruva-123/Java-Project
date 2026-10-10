package com.smartbank.model;

public interface Printable {
    String printReceipt(Transaction transaction);
    String printStatement();
}
