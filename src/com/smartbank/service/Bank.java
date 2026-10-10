package com.smartbank.service;

import com.smartbank.config.Config;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.AccountNotFoundException;
import com.smartbank.exception.BankException;
import com.smartbank.exception.InvalidAmountException;
import com.smartbank.exception.LimitExceededException;
import com.smartbank.exception.TransferFailedException;
import com.smartbank.model.Account;
import com.smartbank.model.Admin;
import com.smartbank.model.AuditLog;
import com.smartbank.model.Card;
import com.smartbank.model.Customer;
import com.smartbank.model.CurrentAccount;
import com.smartbank.model.SavingsAccount;
import com.smartbank.model.Transaction;
import com.smartbank.model.ATM;
import com.smartbank.storage.FileManager;
import com.smartbank.util.IdGenerator;
import com.smartbank.util.SecurityUtil;
import com.smartbank.util.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bank {
    private final String bankName;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Card> cards = new ConcurrentHashMap<>();
    private final Map<String, Admin> admins = new ConcurrentHashMap<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
    private final Map<String, BigDecimal> dailyWithdrawals = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> dailyTransfers = new ConcurrentHashMap<>();
    private FileManager fileManager;
    private ATM atm;

    public Bank(String bankName) {
        this.bankName = bankName;
        this.fileManager = new FileManager();
        this.atm = new ATM("ATM001", "Main Branch", new BigDecimal("500000.00"), this);
    }

    public Bank(String bankName, FileManager fileManager) {
        this.bankName = bankName;
        this.fileManager = fileManager;
        this.atm = new ATM("ATM001", "Main Branch", new BigDecimal("500000.00"), this);
    }

    public String getBankName() {
        return bankName;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public ATM getAtm() {
        return atm;
    }

    public void setAtm(ATM atm) {
        this.atm = atm;
        if (this.atm != null) {
            this.atm.setBank(this);
        }
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public Map<String, Customer> getCustomers() {
        return customers;
    }

    public Map<String, Card> getCards() {
        return cards;
    }

    public Map<String, Admin> getAdmins() {
        return admins;
    }

    public List<AuditLog> getAuditLogs() {
        return Collections.unmodifiableList(auditLogs);
    }

    public void addAdmin(Admin admin) {
        if (admin != null) {
            admins.put(admin.getAdminId(), admin);
        }
    }

    public void addCustomer(Customer customer) {
        if (customer != null) {
            customers.put(customer.getUserId(), customer);
        }
    }

    public void addAccount(Account account) {
        if (account != null) {
            accounts.put(account.getAccountNumber(), account);
        }
    }

    public void addCard(Card card) {
        if (card != null) {
            cards.put(card.getCardNumber(), card);
        }
    }

    public void recordLoadedTransaction(Transaction transaction) {
        if (transaction != null) {
            Account acc = accounts.get(transaction.getAccountNumber());
            if (acc != null) {
                acc.addTransaction(transaction);
            }
        }
    }

    public void recordLoadedAuditLog(AuditLog log) {
        if (log != null) {
            auditLogs.add(log);
        }
    }

    public Account findAccount(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null) {
            throw new AccountNotFoundException();
        }
        Account acc = accounts.get(accountNumber.trim());
        if (acc == null) {
            throw new AccountNotFoundException();
        }
        return acc;
    }

    public Card findCard(String cardNumber) throws BankException {
        if (cardNumber == null) {
            throw new AccountNotFoundException("Card not found");
        }
        String cleanNumber = cardNumber.replaceAll("\\s+", "");
        Card c = cards.get(cleanNumber);
        if (c == null) {
            throw new AccountNotFoundException("Card not found");
        }
        return c;
    }

    public Customer findCustomer(String customerId) {
        if (customerId == null) {
            return null;
        }
        return customers.get(customerId.trim());
    }

    public Admin findAdmin(String adminId) {
        if (adminId == null) {
            return null;
        }
        return admins.get(adminId.trim());
    }

    public synchronized Account openAccount(String name, LocalDate dob, String phone, String address,
                                            String accountType, BigDecimal initialDeposit, String pin,
                                            String password, String adminId) throws BankException {
        if (!Validator.isValidName(name)) {
            throw new BankException("Invalid customer name");
        }
        if (!Validator.isAdult(dob)) {
            throw new BankException("Customer must be at least 18 years old");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new BankException("Invalid or already registered phone number");
        }
        for (Customer c : customers.values()) {
            if (c.getPhone().equals(phone)) {
                throw new BankException("Invalid or already registered phone number");
            }
        }
        if (!Validator.isValidPin(pin)) {
            throw new BankException("PIN is too simple or invalid");
        }

        boolean isSavings = "SAVINGS".equalsIgnoreCase(accountType);
        boolean isCurrent = "CURRENT".equalsIgnoreCase(accountType);
        if (!isSavings && !isCurrent) {
            throw new BankException("Invalid account type: " + accountType);
        }

        BigDecimal minRequired = isSavings ? Config.MIN_OPENING_DEPOSIT_SAVINGS : Config.MIN_OPENING_DEPOSIT_CURRENT;
        if (initialDeposit == null || initialDeposit.compareTo(minRequired) < 0) {
            throw new InvalidAmountException("Opening deposit must be at least Rs. " + minRequired.toPlainString());
        }

        String customerId = IdGenerator.nextCustomerId();
        String pwdHash = SecurityUtil.hash(password != null && !password.trim().isEmpty() ? password : pin);
        Customer customer = new Customer(customerId, name.trim(), pwdHash, phone.trim(), address != null ? address.trim() : "", dob);
        customers.put(customerId, customer);

        String accNumber = IdGenerator.nextAccountNumber();
        Account account;
        if (isSavings) {
            account = new SavingsAccount(accNumber, customerId, initialDeposit, LocalDate.now(), "ACTIVE");
        } else {
            account = new CurrentAccount(accNumber, customerId, initialDeposit, LocalDate.now(), "ACTIVE");
        }
        accounts.put(accNumber, account);
        customer.addAccount(account);

        String cardNumber = IdGenerator.nextCardNumber();
        Card card = new Card(cardNumber, accNumber, SecurityUtil.hash(pin),
                LocalDate.now().plusYears(Config.CARD_VALIDITY_YEARS), 0, false);
        cards.put(cardNumber, card);

        Transaction initialTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                accNumber,
                "OPENING_DEPOSIT",
                initialDeposit,
                LocalDateTime.now(),
                "SUCCESS",
                initialDeposit,
                "INITIAL_OPENING"
        );
        account.addTransaction(initialTxn);

        AuditLog log = new AuditLog(IdGenerator.nextLogId(), adminId != null ? adminId : "SYSTEM",
                "CREATE_ACCOUNT", accNumber, LocalDateTime.now());
        auditLogs.add(log);

        if (fileManager != null) {
            fileManager.appendTransaction(initialTxn);
            fileManager.appendAuditLog(log);
            fileManager.saveAll(this);
        }

        return account;
    }

    public synchronized void closeAccount(String accountNumber, String adminId) throws BankException {
        Account acc = findAccount(accountNumber);
        if (acc.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BankException("Account balance must be zero before closing");
        }
        acc.setStatus("CLOSED");

        for (Card card : cards.values()) {
            if (card.getAccountNumber().equals(accountNumber)) {
                card.block();
            }
        }

        AuditLog log = new AuditLog(IdGenerator.nextLogId(), adminId != null ? adminId : "SYSTEM",
                "CLOSE_ACCOUNT", accountNumber, LocalDateTime.now());
        auditLogs.add(log);

        if (fileManager != null) {
            fileManager.appendAuditLog(log);
            fileManager.saveAll(this);
        }
    }

    public synchronized Transaction payoutRemainingBalance(String accountNumber, String adminId) throws BankException {
        Account acc = findAccount(accountNumber);
        BigDecimal bal = acc.getBalance();
        if (bal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("No balance to pay out");
        }
        acc.debit(bal);
        Transaction t = new Transaction(
                IdGenerator.nextTransactionId(),
                accountNumber,
                "WITHDRAWAL",
                bal,
                LocalDateTime.now(),
                "SUCCESS",
                BigDecimal.ZERO.setScale(2),
                "ADMIN_PAYOUT"
        );
        acc.addTransaction(t);
        AuditLog log = new AuditLog(IdGenerator.nextLogId(), adminId != null ? adminId : "SYSTEM",
                "PAYOUT_BALANCE", accountNumber, LocalDateTime.now());
        auditLogs.add(log);
        if (fileManager != null) {
            fileManager.appendTransaction(t);
            fileManager.appendAuditLog(log);
            fileManager.saveAll(this);
        }
        return t;
    }

    public synchronized void unblockCard(String cardNumber, String adminId) throws BankException {
        Card card = findCard(cardNumber);
        card.unblock();

        AuditLog log = new AuditLog(IdGenerator.nextLogId(), adminId != null ? adminId : "SYSTEM",
                "UNBLOCK_CARD", card.getCardNumber(), LocalDateTime.now());
        auditLogs.add(log);

        if (fileManager != null) {
            fileManager.appendAuditLog(log);
            fileManager.saveAll(this);
        }
    }

    public synchronized void changePin(String cardNumber, String oldPin, String newPin) throws BankException {
        Card card = findCard(cardNumber);
        card.changePin(oldPin, newPin);
        if (fileManager != null) {
            fileManager.saveAll(this);
        }
    }

    public synchronized Transaction deposit(String accountNumber, BigDecimal amount) throws BankException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);

        Transaction txn = new Transaction(
                IdGenerator.nextTransactionId(),
                accountNumber,
                "DEPOSIT",
                amount,
                LocalDateTime.now(),
                "SUCCESS",
                account.getBalance(),
                "DIRECT_DEPOSIT"
        );
        account.addTransaction(txn);

        if (fileManager != null) {
            fileManager.appendTransaction(txn);
            fileManager.saveAll(this);
        }

        return txn;
    }

    public synchronized Transaction withdraw(String accountNumber, BigDecimal amount) throws BankException {
        Account account = findAccount(accountNumber);
        account.withdraw(amount);

        Transaction txn = new Transaction(
                IdGenerator.nextTransactionId(),
                accountNumber,
                "WITHDRAWAL",
                amount,
                LocalDateTime.now(),
                "SUCCESS",
                account.getBalance(),
                "DIRECT_WITHDRAWAL"
        );
        account.addTransaction(txn);

        if (fileManager != null) {
            fileManager.appendTransaction(txn);
            fileManager.saveAll(this);
        }

        return txn;
    }

    public synchronized void transfer(String fromAccount, String toAccount, BigDecimal amount) throws BankException {
        if (fromAccount == null || toAccount == null || fromAccount.trim().equalsIgnoreCase(toAccount.trim())) {
            throw new BankException("Invalid destination account");
        }
        Account src = findAccount(fromAccount);
        Account dst = findAccount(toAccount);

        if (!src.isActive() || !dst.isActive()) {
            throw new AccountInactiveException();
        }

        if (!Validator.isValidAmount(amount)) {
            throw new InvalidAmountException();
        }

        BigDecimal todayTransferred = getTodayTransfers(src.getAccountNumber());
        if (todayTransferred.add(amount).compareTo(Config.DAILY_TRANSFER_LIMIT) > 0) {
            throw new LimitExceededException("Transaction limit exceeded");
        }

        src.withdraw(amount);

        String refId = IdGenerator.nextTransactionId();
        try {
            dst.deposit(amount);
        } catch (Exception e) {
            src.reverseDebit(amount);
            Transaction failedSrc = new Transaction(
                    IdGenerator.nextTransactionId(),
                    src.getAccountNumber(),
                    "TRANSFER_OUT",
                    amount,
                    LocalDateTime.now(),
                    "FAILED",
                    src.getBalance(),
                    refId
            );
            src.addTransaction(failedSrc);
            if (fileManager != null) {
                fileManager.appendTransaction(failedSrc);
            }
            throw new TransferFailedException(e);
        }

        Transaction outTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                src.getAccountNumber(),
                "TRANSFER_OUT",
                amount,
                LocalDateTime.now(),
                "SUCCESS",
                src.getBalance(),
                refId
        );
        Transaction inTxn = new Transaction(
                IdGenerator.nextTransactionId(),
                dst.getAccountNumber(),
                "TRANSFER_IN",
                amount,
                LocalDateTime.now(),
                "SUCCESS",
                dst.getBalance(),
                refId
        );

        src.addTransaction(outTxn);
        dst.addTransaction(inTxn);
        recordSuccessfulTransfer(src.getAccountNumber(), amount);

        if (fileManager != null) {
            fileManager.appendTransaction(outTxn);
            fileManager.appendTransaction(inTxn);
            fileManager.saveAll(this);
        }
    }

    public synchronized List<Transaction> runMonthEnd() throws BankException {
        List<Transaction> createdTransactions = new ArrayList<>();
        for (Account acc : accounts.values()) {
            if (!acc.isActive()) continue;
            if (acc instanceof SavingsAccount) {
                Transaction t = ((SavingsAccount) acc).creditMonthlyInterest();
                if (t != null) {
                    createdTransactions.add(t);
                    if (fileManager != null) {
                        fileManager.appendTransaction(t);
                    }
                }
            } else if (acc instanceof CurrentAccount) {
                List<Transaction> charges = ((CurrentAccount) acc).applyMonthlyCharges();
                for (Transaction ct : charges) {
                    createdTransactions.add(ct);
                    if (fileManager != null) {
                        fileManager.appendTransaction(ct);
                    }
                }
            }
        }

        AuditLog log = new AuditLog(IdGenerator.nextLogId(), "SYSTEM", "RUN_MONTH_END", "ALL_ACCOUNTS", LocalDateTime.now());
        auditLogs.add(log);

        if (fileManager != null) {
            fileManager.appendAuditLog(log);
            fileManager.saveAll(this);
        }

        return createdTransactions;
    }

    public BigDecimal getTodayWithdrawals(String accountNumber) {
        String key = accountNumber + "_" + LocalDate.now().toString();
        return dailyWithdrawals.getOrDefault(key, BigDecimal.ZERO);
    }

    public void recordSuccessfulWithdrawal(String accountNumber, BigDecimal amount, Transaction txn) {
        String key = accountNumber + "_" + LocalDate.now().toString();
        dailyWithdrawals.merge(key, amount, BigDecimal::add);
        if (fileManager != null) {
            try {
                fileManager.appendTransaction(txn);
                fileManager.saveAll(this);
            } catch (Exception ignored) {}
        }
    }

    public void recordFailedTransaction(Transaction txn) {
        if (fileManager != null) {
            try {
                fileManager.appendTransaction(txn);
            } catch (Exception ignored) {}
        }
    }

    public BigDecimal getTodayTransfers(String accountNumber) {
        String key = accountNumber + "_" + LocalDate.now().toString();
        return dailyTransfers.getOrDefault(key, BigDecimal.ZERO);
    }

    public void recordSuccessfulTransfer(String accountNumber, BigDecimal amount) {
        String key = accountNumber + "_" + LocalDate.now().toString();
        dailyTransfers.merge(key, amount, BigDecimal::add);
    }

    public Card findCardByAccount(String accountNumber) {
        for (Card c : cards.values()) {
            if (c.getAccountNumber().equals(accountNumber)) {
                return c;
            }
        }
        return null;
    }

    public String generateDailySummary() {
        LocalDate today = LocalDate.now();
        int totalTxns = 0;
        int successful = 0;
        int failed = 0;
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfers = BigDecimal.ZERO;

        for (Account a : accounts.values()) {
            for (Transaction t : a.getHistory()) {
                if (t.getDateTime() != null && t.getDateTime().toLocalDate().equals(today)) {
                    totalTxns++;
                    if ("SUCCESS".equalsIgnoreCase(t.getStatus())) {
                        successful++;
                        switch (t.getType()) {
                            case "DEPOSIT":
                            case "OPENING_DEPOSIT":
                                totalDeposits = totalDeposits.add(t.getAmount());
                                break;
                            case "WITHDRAWAL":
                                totalWithdrawals = totalWithdrawals.add(t.getAmount());
                                break;
                            case "TRANSFER_OUT":
                                totalTransfers = totalTransfers.add(t.getAmount());
                                break;
                            default:
                                break;
                        }
                    } else {
                        failed++;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("               DAILY TRANSACTION SUMMARY            \n");
        sb.append("Date: ").append(today.toString()).append("\n");
        sb.append("====================================================\n");
        sb.append(String.format("Total Transactions:    %d%n", totalTxns));
        sb.append(String.format("Successful:            %d%n", successful));
        sb.append(String.format("Failed:                %d%n", failed));
        sb.append(String.format("Total Deposits:        Rs. %.2f%n", totalDeposits.doubleValue()));
        sb.append(String.format("Total Withdrawals:     Rs. %.2f%n", totalWithdrawals.doubleValue()));
        sb.append(String.format("Total Transfers:       Rs. %.2f%n", totalTransfers.doubleValue()));
        sb.append("====================================================\n");
        return sb.toString();
    }

    public List<Card> getBlockedCards() {
        List<Card> list = new ArrayList<>();
        for (Card c : cards.values()) {
            if (c.isBlocked()) {
                list.add(c);
            }
        }
        return list;
    }

    public List<Account> getLowBalanceAccounts() {
        List<Account> list = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a instanceof SavingsAccount && a.getBalance().compareTo(Config.MIN_BALANCE) < 0) {
                list.add(a);
            } else if (a instanceof CurrentAccount && a.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                list.add(a);
            }
        }
        return list;
    }

    public List<Customer> getCustomerList() {
        return new ArrayList<>(customers.values());
    }
}
