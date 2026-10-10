package com.smartbank.storage;

import com.smartbank.config.Config;
import com.smartbank.exception.DataAccessException;
import com.smartbank.model.Account;
import com.smartbank.model.Admin;
import com.smartbank.model.AuditLog;
import com.smartbank.model.Card;
import com.smartbank.model.Customer;
import com.smartbank.model.CurrentAccount;
import com.smartbank.model.SavingsAccount;
import com.smartbank.model.Transaction;
import com.smartbank.service.Bank;
import com.smartbank.util.IdGenerator;
import com.smartbank.util.SecurityUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileManager {
    private final String dataDir;

    public FileManager() {
        this(Config.DATA_DIR);
    }

    public FileManager(String dataDir) {
        this.dataDir = dataDir;
        ensureDataDirExists();
    }

    private void ensureDataDirExists() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private File getFile(String filename) {
        return new File(dataDir, filename);
    }

    private void backupFile(File file) throws IOException {
        if (file.exists()) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized void loadAll(Bank bank) throws DataAccessException {
        ensureDataDirExists();
        loadAdmins(bank);
        loadCustomers(bank);
        loadAccounts(bank);
        loadCards(bank);
        loadTransactions(bank);
        loadAuditLogs(bank);
    }

    public synchronized void saveAll(Bank bank) throws DataAccessException {
        ensureDataDirExists();
        saveCustomers(bank.getCustomers());
        saveAccounts(bank.getAccounts());
        saveCards(bank.getCards());
        saveAdmins(bank.getAdmins());
        saveAuditLogs(bank.getAuditLogs());
    }

    private void loadAdmins(Bank bank) throws DataAccessException {
        File file = getFile(Config.ADMINS_FILE);
        if (!file.exists()) {
            Admin defaultAdmin = new Admin("A1001", "System Administrator", SecurityUtil.hash("admin123"), "ADMIN");
            bank.addAdmin(defaultAdmin);
            saveAdmins(bank.getAdmins());
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    String role = parts.length >= 4 ? parts[3].trim() : "ADMIN";
                    Admin admin = new Admin(parts[0].trim(), parts[1].trim(), parts[2].trim(), role);
                    bank.addAdmin(admin);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load admins", e);
        }
    }

    private void saveAdmins(Map<String, Admin> admins) throws DataAccessException {
        File file = getFile(Config.ADMINS_FILE);
        try {
            backupFile(file);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (Admin admin : admins.values()) {
                    String line = String.join(",",
                            admin.getAdminId(),
                            admin.getName(),
                            admin.getPasswordHash(),
                            admin.getRole());
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to save admins", e);
        }
    }

    private void loadCustomers(Bank bank) throws DataAccessException {
        File file = getFile(Config.CUSTOMERS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    String custId = parts[0].trim();
                    String name = parts[1].trim();
                    LocalDate dob = LocalDate.parse(parts[2].trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    String phone = parts[3].trim();
                    String address = parts[4].trim();
                    String pwdHash = parts[5].trim();
                    Customer customer = new Customer(custId, name, pwdHash, phone, address, dob);
                    bank.addCustomer(customer);
                    if (custId.startsWith("C")) {
                        try {
                            int seq = Integer.parseInt(custId.substring(1));
                            IdGenerator.updateCustomerSeqIfGreater(seq);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load customers", e);
        }
    }

    private void saveCustomers(Map<String, Customer> customers) throws DataAccessException {
        File file = getFile(Config.CUSTOMERS_FILE);
        try {
            backupFile(file);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (Customer c : customers.values()) {
                    String line = String.join(",",
                            c.getUserId(),
                            c.getName(),
                            c.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            c.getPhone(),
                            c.getAddress(),
                            c.getPasswordHash());
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to save customers", e);
        }
    }

    private void loadAccounts(Bank bank) throws DataAccessException {
        File file = getFile(Config.ACCOUNTS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    String accNum = parts[0].trim();
                    String custId = parts[1].trim();
                    String type = parts[2].trim();
                    BigDecimal balance = new BigDecimal(parts[3].trim());
                    LocalDate openDate = LocalDate.parse(parts[4].trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    String status = parts[5].trim();

                    Account acc;
                    if ("SAVINGS".equalsIgnoreCase(type)) {
                        acc = new SavingsAccount(accNum, custId, balance, openDate, status);
                    } else {
                        acc = new CurrentAccount(accNum, custId, balance, openDate, status);
                    }
                    bank.addAccount(acc);
                    Customer cust = bank.findCustomer(custId);
                    if (cust != null) {
                        cust.addAccount(acc);
                    }
                    try {
                        long num = Long.parseLong(accNum);
                        IdGenerator.updateAccountSeqIfGreater(num);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load accounts", e);
        }
    }

    private void saveAccounts(Map<String, Account> accounts) throws DataAccessException {
        File file = getFile(Config.ACCOUNTS_FILE);
        try {
            backupFile(file);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (Account acc : accounts.values()) {
                    bw.write(acc.toCsvLine());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to save accounts", e);
        }
    }

    private void loadCards(Bank bank) throws DataAccessException {
        File file = getFile(Config.CARDS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Card card = Card.fromCsvLine(line);
                if (card != null) {
                    bank.addCard(card);
                    if (card.getCardNumber().length() >= 15 && card.getCardNumber().startsWith(Config.BANK_CODE)) {
                        try {
                            String mid = card.getCardNumber().substring(Config.BANK_CODE.length(), 15);
                            long cardSeqVal = Long.parseLong(mid);
                            IdGenerator.updateCardSeqIfGreater(cardSeqVal);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load cards", e);
        }
    }

    private void saveCards(Map<String, Card> cards) throws DataAccessException {
        File file = getFile(Config.CARDS_FILE);
        try {
            backupFile(file);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (Card card : cards.values()) {
                    bw.write(card.toCsvLine());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to save cards", e);
        }
    }

    private void loadTransactions(Bank bank) throws DataAccessException {
        File file = getFile(Config.TRANSACTIONS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Transaction txn = Transaction.fromCsvLine(line);
                if (txn != null) {
                    bank.recordLoadedTransaction(txn);
                    String tid = txn.getTransactionId();
                    if (tid != null && tid.length() >= 16 && tid.startsWith("TXN")) {
                        try {
                            int seq = Integer.parseInt(tid.substring(tid.length() - 5));
                            IdGenerator.updateTransactionSeqIfGreater(seq);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load transactions", e);
        }
    }

    public synchronized void appendTransaction(Transaction txn) throws DataAccessException {
        File file = getFile(Config.TRANSACTIONS_FILE);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(txn.toCsvLine());
            bw.newLine();
        } catch (IOException e) {
            throw new DataAccessException("Failed to append transaction", e);
        }
    }

    private void loadAuditLogs(Bank bank) throws DataAccessException {
        File file = getFile(Config.AUDIT_LOG_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                AuditLog log = AuditLog.fromCsvLine(line);
                if (log != null) {
                    bank.recordLoadedAuditLog(log);
                    String lid = log.getLogId();
                    if (lid != null && lid.startsWith("LOG")) {
                        try {
                            int seq = Integer.parseInt(lid.substring(3));
                            IdGenerator.updateLogSeqIfGreater(seq);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to load audit logs", e);
        }
    }

    public synchronized void appendAuditLog(AuditLog log) throws DataAccessException {
        File file = getFile(Config.AUDIT_LOG_FILE);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(log.toCsvLine());
            bw.newLine();
        } catch (IOException e) {
            throw new DataAccessException("Failed to append audit log", e);
        }
    }

    private void saveAuditLogs(List<AuditLog> logs) throws DataAccessException {
        File file = getFile(Config.AUDIT_LOG_FILE);
        try {
            backupFile(file);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (AuditLog l : logs) {
                    bw.write(l.toCsvLine());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to save audit logs", e);
        }
    }
}
