package com.smartbank.batch;

import com.smartbank.model.Account;
import com.smartbank.model.Card;
import com.smartbank.model.Customer;
import com.smartbank.model.Transaction;
import com.smartbank.service.Bank;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstructionProcessor {
    private final Bank bank;

    public InstructionProcessor(Bank bank) {
        this.bank = bank;
    }

    public String processFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return processLines(lines);
    }

    public String processInstructions(String scriptText) {
        String[] rawLines = scriptText.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String l : rawLines) {
            lines.add(l);
        }
        return processLines(lines);
    }

    public String processLines(List<String> lines) {
        StringBuilder output = new StringBuilder();
        int lineNum = 0;
        int successCount = 0;
        int failCount = 0;

        for (String raw : lines) {
            lineNum++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            try {
                String result = executeInstruction(line);
                successCount++;
                output.append(String.format("[LINE %d] SUCCESS: %s%n", lineNum, result));
            } catch (Exception e) {
                failCount++;
                output.append(String.format("[LINE %d] ERROR: %s%n", lineNum, e.getMessage()));
            }
        }

        output.append(String.format("%n--- Summary: %d succeeded, %d failed ---%n", successCount, failCount));
        return output.toString();
    }

    public String executeInstruction(String instruction) throws Exception {
        String trimmed = instruction.trim();
        int firstSpace = trimmed.indexOf(' ');
        String command = firstSpace == -1 ? trimmed.toUpperCase() : trimmed.substring(0, firstSpace).toUpperCase();
        String paramsPart = firstSpace == -1 ? "" : trimmed.substring(firstSpace + 1).trim();

        Map<String, String> params = parseParameters(paramsPart);

        switch (command) {
            case "OPEN_ACCOUNT":
                return handleOpenAccount(params);
            case "DEPOSIT":
                return handleDeposit(params);
            case "WITHDRAW":
                return handleWithdraw(params);
            case "ATM_WITHDRAW":
                return handleAtmWithdraw(params);
            case "TRANSFER":
                return handleTransfer(params);
            case "BALANCE":
            case "CHECK_BALANCE":
                return handleCheckBalance(params);
            case "MINI_STATEMENT":
                return handleMiniStatement(params);
            case "CHANGE_PIN":
                return handleChangePin(params);
            case "UNBLOCK_CARD":
                return handleUnblockCard(params);
            case "CLOSE_ACCOUNT":
                return handleCloseAccount(params);
            case "PAYOUT":
            case "PAYOUT_BALANCE":
                return handlePayoutBalance(params);
            case "MONTH_END":
            case "RUN_MONTH_END":
                return handleMonthEnd();
            case "REPORT":
                return handleReport(params);
            case "ATM_LOGIN":
                return handleAtmLogin(params);
            case "ATM_CHECK_BALANCE":
                return handleAtmCheckBalance();
            case "ATM_END_SESSION":
                return handleAtmEndSession();
            default:
                throw new IllegalArgumentException("Unknown instruction command: " + command);
        }
    }

    private Map<String, String> parseParameters(String input) {
        Map<String, String> map = new HashMap<>();
        if (input == null || input.isEmpty()) {
            return map;
        }

        Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s,]+))");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value;
            if (matcher.group(3) != null) {
                value = matcher.group(3);
            } else if (matcher.group(4) != null) {
                value = matcher.group(4);
            } else {
                value = matcher.group(5);
            }
            map.put(key, value.trim());
        }
        return map;
    }

    private String handleOpenAccount(Map<String, String> p) throws Exception {
        String name = getRequired(p, "name");
        String dobStr = getRequired(p, "dob");
        String phone = getRequired(p, "phone");
        String address = p.getOrDefault("address", "Standard Address");
        String type = p.getOrDefault("type", "SAVINGS");
        String depositStr = getRequired(p, "deposit");
        String pin = getRequired(p, "pin");
        String password = p.getOrDefault("password", pin);
        String adminId = p.getOrDefault("adminid", "A1001");

        LocalDate dob = LocalDate.parse(dobStr, DateTimeFormatter.ISO_LOCAL_DATE);
        BigDecimal deposit = new BigDecimal(depositStr);

        Account acc = bank.openAccount(name, dob, phone, address, type, deposit, pin, password, adminId);
        Card card = bank.findCardByAccount(acc.getAccountNumber());
        return String.format("Account created: %s (%s) for Customer: %s, Card: %s, Balance: Rs. %.2f",
                acc.getAccountNumber(), acc.getAccountType(), acc.getCustomerId(),
                card != null ? card.getCardNumber() : "N/A", acc.getBalance().doubleValue());
    }

    private String handleDeposit(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        BigDecimal amt = new BigDecimal(getRequired(p, "amount"));
        Transaction t = bank.deposit(accNum, amt);
        return String.format("Deposited Rs. %.2f into %s. New Balance: Rs. %.2f (Txn: %s)",
                amt.doubleValue(), accNum, t.getBalanceAfter().doubleValue(), t.getTransactionId());
    }

    private String handleWithdraw(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        BigDecimal amt = new BigDecimal(getRequired(p, "amount"));
        Transaction t = bank.withdraw(accNum, amt);
        return String.format("Withdrew Rs. %.2f from %s. New Balance: Rs. %.2f (Txn: %s)",
                amt.doubleValue(), accNum, t.getBalanceAfter().doubleValue(), t.getTransactionId());
    }

    private String handleAtmWithdraw(Map<String, String> p) throws Exception {
        String cardNum = getRequired(p, "card");
        String pin = getRequired(p, "pin");
        BigDecimal amt = new BigDecimal(getRequired(p, "amount"));

        bank.getAtm().authenticate(cardNum, pin);
        Transaction t = bank.getAtm().dispenseCash(amt);
        bank.getAtm().endSession();

        return String.format("ATM dispensed Rs. %.2f. Balance: Rs. %.2f (Txn: %s)",
                amt.doubleValue(), t.getBalanceAfter().doubleValue(), t.getTransactionId());
    }

    private String handleTransfer(Map<String, String> p) throws Exception {
        String from = getRequired(p, "from");
        String to = getRequired(p, "to");
        BigDecimal amt = new BigDecimal(getRequired(p, "amount"));
        bank.transfer(from, to, amt);
        Account src = bank.findAccount(from);
        Account dst = bank.findAccount(to);
        return String.format("Transferred Rs. %.2f from %s to %s. Source Bal: Rs. %.2f, Dest Bal: Rs. %.2f",
                amt.doubleValue(), from, to, src.getBalance().doubleValue(), dst.getBalance().doubleValue());
    }

    private String handleCheckBalance(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        Account acc = bank.findAccount(accNum);
        return String.format("Account %s Balance: Rs. %.2f (Status: %s)",
                accNum, acc.getBalance().doubleValue(), acc.getStatus());
    }

    private String handleMiniStatement(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        Account acc = bank.findAccount(accNum);
        return "\n" + acc.printStatement();
    }

    private String handleChangePin(Map<String, String> p) throws Exception {
        String cardNum = getRequired(p, "card");
        String oldPin = getRequired(p, "oldpin");
        String newPin = getRequired(p, "newpin");
        bank.changePin(cardNum, oldPin, newPin);
        return "PIN successfully updated for Card " + cardNum;
    }

    private String handleUnblockCard(Map<String, String> p) throws Exception {
        String cardNum = getRequired(p, "card");
        String adminId = p.getOrDefault("adminid", "A1001");
        bank.unblockCard(cardNum, adminId);
        return "Card " + cardNum + " successfully unblocked by Admin " + adminId;
    }

    private String handleCloseAccount(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        String adminId = p.getOrDefault("adminid", "A1001");
        bank.closeAccount(accNum, adminId);
        return "Account " + accNum + " closed by Admin " + adminId;
    }

    private String handlePayoutBalance(Map<String, String> p) throws Exception {
        String accNum = getRequired(p, "account");
        String adminId = p.getOrDefault("adminid", "A1001");
        Transaction t = bank.payoutRemainingBalance(accNum, adminId);
        return String.format("Paid out remaining balance Rs. %.2f from account %s (Txn: %s)",
                t.getAmount().doubleValue(), accNum, t.getTransactionId());
    }

    private String handleMonthEnd() throws Exception {
        List<Transaction> txns = bank.runMonthEnd();
        return "Month-end processed successfully. Generated " + txns.size() + " interest/charge transactions.";
    }

    private String handleReport(Map<String, String> p) {
        String reportType = p.getOrDefault("type", "DAILY_SUMMARY").toUpperCase();
        switch (reportType) {
            case "DAILY_SUMMARY":
                return "\n" + bank.generateDailySummary();
            case "BLOCKED_CARDS":
                List<Card> blocked = bank.getBlockedCards();
                StringBuilder sb1 = new StringBuilder("\nBlocked Cards (" + blocked.size() + "):\n");
                for (Card c : blocked) {
                    sb1.append("Card: ").append(c.getCardNumber()).append(" | Account: ").append(c.getAccountNumber()).append("\n");
                }
                return sb1.toString();
            case "LOW_BALANCE":
                List<Account> low = bank.getLowBalanceAccounts();
                StringBuilder sb2 = new StringBuilder("\nLow Balance Accounts (" + low.size() + "):\n");
                for (Account a : low) {
                    sb2.append("Account: ").append(a.getAccountNumber()).append(" | Type: ").append(a.getAccountType())
                            .append(" | Balance: Rs. ").append(a.getBalance().toPlainString()).append("\n");
                }
                return sb2.toString();
            case "CUSTOMERS":
                List<Customer> custs = bank.getCustomerList();
                StringBuilder sb3 = new StringBuilder("\nCustomers (" + custs.size() + "):\n");
                for (Customer c : custs) {
                    sb3.append("ID: ").append(c.getUserId()).append(" | Name: ").append(c.getName())
                            .append(" | Phone: ").append(c.getPhone()).append(" | Accounts: ").append(c.getAccounts().size()).append("\n");
                }
                return sb3.toString();
            default:
                return "Unknown report type: " + reportType;
        }
    }

    private String handleAtmLogin(Map<String, String> p) throws Exception {
        String cardNum = getRequired(p, "card");
        String pin = getRequired(p, "pin");
        bank.getAtm().authenticate(cardNum, pin);
        return "ATM session started for Card " + cardNum;
    }

    private String handleAtmCheckBalance() throws Exception {
        BigDecimal bal = bank.getAtm().checkBalance();
        return "ATM Current Balance: Rs. " + bal.toPlainString();
    }

    private String handleAtmEndSession() {
        bank.getAtm().endSession();
        return "ATM session ended.";
    }

    private String getRequired(Map<String, String> map, String key) {
        String val = map.get(key.toLowerCase());
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return val.trim();
    }
}
