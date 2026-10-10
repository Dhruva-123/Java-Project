package com.smartbank;

import com.smartbank.batch.InstructionProcessor;
import com.smartbank.exception.BankException;
import com.smartbank.model.Account;
import com.smartbank.model.Admin;
import com.smartbank.model.Card;
import com.smartbank.model.Customer;
import com.smartbank.model.Transaction;
import com.smartbank.service.Bank;
import com.smartbank.storage.FileManager;
import com.smartbank.ui.MainFrame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        FileManager fileManager = new FileManager();
        Bank bank = new Bank("Smart Bank of India", fileManager);

        try {
            fileManager.loadAll(bank);
        } catch (Exception e) {
            System.err.println("Notice during data initialization: " + e.getMessage());
        }

        if (args.length > 0) {
            String firstArg = args[0];
            if (firstArg.equalsIgnoreCase("--cli")) {
                runCli(bank);
                return;
            }

            String filePath = firstArg.equalsIgnoreCase("-f") || firstArg.equalsIgnoreCase("--file") ?
                    (args.length > 1 ? args[1] : null) : firstArg;

            if (filePath != null) {
                File scriptFile = new File(filePath);
                if (scriptFile.exists()) {
                    try {
                        InstructionProcessor processor = new InstructionProcessor(bank);
                        String result = processor.processFile(scriptFile);
                        System.out.println(result);
                    } catch (Exception e) {
                        System.err.println("Error executing instruction file: " + e.getMessage());
                    }
                    return;
                }
            }
        }

        if (GraphicsEnvironment.isHeadless()) {
            runCli(bank);
        } else {
            SwingUtilities.invokeLater(() -> {
                MainFrame.setupLookAndFeel();
                MainFrame frame = new MainFrame(bank);
                frame.setVisible(true);
            });
        }
    }

    public static void runCli(Bank bank) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("   SMART BANK & ATM SIMULATION SYSTEM     ");
            System.out.println("==========================================");
            System.out.println("1. Customer Login");
            System.out.println("2. Admin Login");
            System.out.println("3. ATM Mode (Card & PIN)");
            System.out.println("4. Execute Instructions Script File");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleCliCustomerLogin(bank, scanner);
                    break;
                case "2":
                    handleCliAdminLogin(bank, scanner);
                    break;
                case "3":
                    handleCliAtmMode(bank, scanner);
                    break;
                case "4":
                    handleCliInstructionFile(bank, scanner);
                    break;
                case "5":
                    System.out.println("Exiting system. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void handleCliCustomerLogin(Bank bank, Scanner scanner) {
        System.out.print("Enter Customer ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String pwd = scanner.nextLine().trim();

        Customer customer = bank.findCustomer(id);
        if (customer == null || !customer.login(id, pwd)) {
            System.out.println("Invalid ID or password");
            return;
        }

        System.out.println("\nWelcome, " + customer.getName() + "!");
        while (true) {
            System.out.println("\n--- Customer Dashboard ---");
            List<Account> accounts = customer.getAccounts();
            if (accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }
            for (int i = 0; i < accounts.size(); i++) {
                Account a = accounts.get(i);
                System.out.printf("[%d] Account: %s (%s) | Balance: Rs. %.2f | Status: %s%n",
                        i + 1, a.getAccountNumber(), a.getAccountType(), a.getBalance().doubleValue(), a.getStatus());
            }
            System.out.println("Actions: (D)eposit, (W)ithdraw, (T)ransfer, (M)ini Statement, (P)IN Change, (L)ogout");
            System.out.print("Select action: ");
            String act = scanner.nextLine().trim().toUpperCase();
            if (act.equals("L")) {
                break;
            }

            System.out.print("Select account number index [1-" + accounts.size() + "]: ");
            int idx;
            try {
                idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx < 0 || idx >= accounts.size()) {
                    System.out.println("Invalid selection.");
                    continue;
                }
            } catch (Exception e) {
                System.out.println("Invalid index.");
                continue;
            }
            Account selected = accounts.get(idx);

            try {
                if (act.equals("D")) {
                    System.out.print("Enter amount to deposit: ");
                    BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                    bank.deposit(selected.getAccountNumber(), amt);
                    System.out.println("Deposit successful! New balance: Rs. " + selected.getBalance().toPlainString());
                } else if (act.equals("W")) {
                    System.out.print("Enter amount to withdraw: ");
                    BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                    bank.withdraw(selected.getAccountNumber(), amt);
                    System.out.println("Withdrawal successful! New balance: Rs. " + selected.getBalance().toPlainString());
                } else if (act.equals("T")) {
                    System.out.print("Enter destination account number: ");
                    String dst = scanner.nextLine().trim();
                    System.out.print("Enter amount: ");
                    BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                    bank.transfer(selected.getAccountNumber(), dst, amt);
                    System.out.println("Transfer successful!");
                } else if (act.equals("M")) {
                    System.out.println(selected.printStatement());
                } else if (act.equals("P")) {
                    Card c = bank.findCardByAccount(selected.getAccountNumber());
                    if (c == null) {
                        System.out.println("No card associated.");
                        continue;
                    }
                    System.out.print("Enter old PIN: ");
                    String oldPin = scanner.nextLine().trim();
                    System.out.print("Enter new 4-digit PIN: ");
                    String newPin = scanner.nextLine().trim();
                    bank.changePin(c.getCardNumber(), oldPin, newPin);
                    System.out.println("PIN updated successfully.");
                }
            } catch (BankException e) {
                System.out.println("Operation Failed: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleCliAdminLogin(Bank bank, Scanner scanner) {
        System.out.print("Enter Admin ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String pwd = scanner.nextLine().trim();

        Admin admin = bank.findAdmin(id);
        if (admin == null || !admin.login(id, pwd)) {
            System.out.println("Invalid ID or password");
            return;
        }

        System.out.println("\nWelcome Admin, " + admin.getName() + "!");
        while (true) {
            System.out.println("\n--- Admin Management Panel ---");
            System.out.println("1. Create New Account");
            System.out.println("2. Close Account");
            System.out.println("3. Unblock Card");
            System.out.println("4. Run Month-End Processing");
            System.out.println("5. View Daily Transaction Summary");
            System.out.println("6. View Blocked Cards");
            System.out.println("7. View Low Balance Accounts");
            System.out.println("8. View Customer List");
            System.out.println("9. Logout");
            System.out.print("Choose action: ");

            String choice = scanner.nextLine().trim();
            if (choice.equals("9")) break;

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Customer Full Name: ");
                        String name = scanner.nextLine().trim();
                        System.out.print("Date of Birth (YYYY-MM-DD): ");
                        LocalDate dob = LocalDate.parse(scanner.nextLine().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                        System.out.print("Phone (10 digits): ");
                        String phone = scanner.nextLine().trim();
                        System.out.print("Address: ");
                        String address = scanner.nextLine().trim();
                        System.out.print("Account Type (SAVINGS/CURRENT): ");
                        String type = scanner.nextLine().trim();
                        System.out.print("Opening Deposit: ");
                        BigDecimal deposit = new BigDecimal(scanner.nextLine().trim());
                        System.out.print("4-digit PIN: ");
                        String pin = scanner.nextLine().trim();
                        System.out.print("Customer Password: ");
                        String password = scanner.nextLine().trim();

                        Account acc = bank.openAccount(name, dob, phone, address, type, deposit, pin, password, admin.getAdminId());
                        Card card = bank.findCardByAccount(acc.getAccountNumber());
                        System.out.printf("Created! Account: %s, Customer: %s, Card: %s%n",
                                acc.getAccountNumber(), acc.getCustomerId(), card != null ? card.getCardNumber() : "N/A");
                        break;
                    case "2":
                        System.out.print("Enter Account Number to close: ");
                        String accNum = scanner.nextLine().trim();
                        bank.closeAccount(accNum, admin.getAdminId());
                        System.out.println("Account closed.");
                        break;
                    case "3":
                        System.out.print("Enter Card Number to unblock: ");
                        String cardNum = scanner.nextLine().trim();
                        bank.unblockCard(cardNum, admin.getAdminId());
                        System.out.println("Card unblocked.");
                        break;
                    case "4":
                        List<Transaction> txns = bank.runMonthEnd();
                        System.out.println("Month-end processed. Created " + txns.size() + " transactions.");
                        break;
                    case "5":
                        System.out.println(bank.generateDailySummary());
                        break;
                    case "6":
                        List<Card> blocked = bank.getBlockedCards();
                        System.out.println("Blocked Cards: " + blocked.size());
                        for (Card c : blocked) {
                            System.out.println("Card: " + c.getCardNumber() + " | Account: " + c.getAccountNumber());
                        }
                        break;
                    case "7":
                        List<Account> low = bank.getLowBalanceAccounts();
                        System.out.println("Low Balance Accounts: " + low.size());
                        for (Account a : low) {
                            System.out.printf("Account: %s (%s) | Balance: Rs. %.2f%n",
                                    a.getAccountNumber(), a.getAccountType(), a.getBalance().doubleValue());
                        }
                        break;
                    case "8":
                        List<Customer> customers = bank.getCustomerList();
                        System.out.println("Total Customers: " + customers.size());
                        for (Customer c : customers) {
                            System.out.printf("ID: %s | Name: %s | Phone: %s | Accounts: %d%n",
                                    c.getUserId(), c.getName(), c.getPhone(), c.getAccounts().size());
                        }
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleCliAtmMode(Bank bank, Scanner scanner) {
        System.out.print("Insert Card (Enter 16-digit Card Number): ");
        String cardNum = scanner.nextLine().trim();
        System.out.print("Enter 4-digit PIN: ");
        String pin = scanner.nextLine().trim();

        try {
            bank.getAtm().authenticate(cardNum, pin);
            System.out.println("Authentication successful!");

            while (true) {
                System.out.println("\n--- ATM Menu ---");
                System.out.println("1. Withdraw Cash");
                System.out.println("2. Balance Enquiry");
                System.out.println("3. Mini Statement");
                System.out.println("4. Change PIN");
                System.out.println("5. Exit ATM Session");
                System.out.print("Choose option: ");

                String choice = scanner.nextLine().trim();
                if (choice.equals("5")) {
                    bank.getAtm().endSession();
                    System.out.println("ATM session ended. Card ejected.");
                    break;
                }

                switch (choice) {
                    case "1":
                        System.out.print("Enter amount (Multiples of Rs. 100, Max Rs. 10,000): ");
                        BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                        Transaction txn = bank.getAtm().dispenseCash(amt);
                        System.out.println("Cash dispensed! Current Balance: Rs. " + txn.getBalanceAfter().toPlainString());
                        System.out.print("Print receipt? (y/n): ");
                        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                            System.out.println(bank.getAtm().printReceipt(txn));
                        }
                        break;
                    case "2":
                        BigDecimal bal = bank.getAtm().checkBalance();
                        System.out.println("Available Balance: Rs. " + bal.toPlainString());
                        break;
                    case "3":
                        System.out.println(bank.getAtm().getCurrentSessionAccount().printStatement());
                        break;
                    case "4":
                        System.out.print("Enter Old PIN: ");
                        String oldPin = scanner.nextLine().trim();
                        System.out.print("Enter New PIN: ");
                        String newPin = scanner.nextLine().trim();
                        bank.changePin(cardNum, oldPin, newPin);
                        System.out.println("PIN updated successfully.");
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (BankException e) {
            System.out.println("ATM Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void handleCliInstructionFile(Bank bank, Scanner scanner) {
        System.out.print("Enter instructions script file path: ");
        String path = scanner.nextLine().trim();
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File not found: " + path);
            return;
        }

        try {
            InstructionProcessor processor = new InstructionProcessor(bank);
            String report = processor.processFile(file);
            System.out.println(report);
        } catch (Exception e) {
            System.out.println("Execution failed: " + e.getMessage());
        }
    }
}
