package com.smartbank.ui;

import com.smartbank.batch.InstructionProcessor;
import com.smartbank.config.Config;
import com.smartbank.exception.BankException;
import com.smartbank.model.Account;
import com.smartbank.model.Admin;
import com.smartbank.model.Card;
import com.smartbank.model.Customer;
import com.smartbank.model.Transaction;
import com.smartbank.service.Bank;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JPanel {
    private final MainFrame mainFrame;
    private Admin currentAdmin;
    private JLabel adminInfoLabel;

    private DefaultTableModel customersModel;
    private DefaultTableModel transactionsModel;
    private DefaultTableModel blockedCardsModel;

    public AdminPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(60, 40, 95));
        topBar.setPreferredSize(new Dimension(800, 70));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        adminInfoLabel = new JLabel("Admin Panel - Banking Management");
        adminInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        adminInfoLabel.setForeground(Color.WHITE);
        topBar.add(adminInfoLabel, BorderLayout.WEST);

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topButtons.setOpaque(false);

        JButton monthEndBtn = new JButton("Run Month-End");
        monthEndBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        monthEndBtn.setBackground(new Color(220, 130, 40));
        monthEndBtn.setForeground(Color.WHITE);
        monthEndBtn.addActionListener(e -> executeMonthEnd());

        JButton batchFileBtn = new JButton("Batch Script");
        batchFileBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        batchFileBtn.addActionListener(e -> uploadBatchScript());

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logoutBtn.addActionListener(e -> logout());

        topButtons.add(monthEndBtn);
        topButtons.add(batchFileBtn);
        topButtons.add(logoutBtn);
        topBar.add(topButtons, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));

        tabbedPane.addTab("Create Account", createOpenAccountTab());
        tabbedPane.addTab("Close Account", createCloseAccountTab());
        tabbedPane.addTab("Customers", createCustomersTab());
        tabbedPane.addTab("Transactions", createTransactionsTab());
        tabbedPane.addTab("Blocked Cards", createBlockedCardsTab());
        tabbedPane.addTab("Reports", createReportsTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void loadAdmin(Admin admin) {
        this.currentAdmin = admin;
        if (admin != null) {
            adminInfoLabel.setText("Admin Panel | Logged in as: " + admin.getName() + " (" + admin.getAdminId() + ")");
            refreshTables();
        }
    }

    private JPanel createOpenAccountTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        JTextField nameField = new JTextField(20);
        JTextField dobField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        JTextField depositField = new JTextField(20);
        JPasswordField pinField = new JPasswordField(20);
        JPasswordField pwdField = new JPasswordField(20);

        int row = 0;
        form.add(new JLabel("Full Name:"), setGbc(g, 0, row));
        form.add(nameField, setGbc(g, 1, row++));

        form.add(new JLabel("Date of Birth (YYYY-MM-DD):"), setGbc(g, 0, row));
        form.add(dobField, setGbc(g, 1, row++));

        form.add(new JLabel("Phone (10 digits):"), setGbc(g, 0, row));
        form.add(phoneField, setGbc(g, 1, row++));

        form.add(new JLabel("Address:"), setGbc(g, 0, row));
        form.add(addressField, setGbc(g, 1, row++));

        form.add(new JLabel("Account Type:"), setGbc(g, 0, row));
        form.add(typeCombo, setGbc(g, 1, row++));

        form.add(new JLabel("Opening Deposit (Rs.):"), setGbc(g, 0, row));
        form.add(depositField, setGbc(g, 1, row++));

        form.add(new JLabel("4-Digit Card PIN:"), setGbc(g, 0, row));
        form.add(pinField, setGbc(g, 1, row++));

        form.add(new JLabel("Customer Web Password:"), setGbc(g, 0, row));
        form.add(pwdField, setGbc(g, 1, row++));

        JButton createBtn = new JButton("Create Account");
        createBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        createBtn.setBackground(new Color(60, 40, 95));
        createBtn.setForeground(Color.WHITE);
        createBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                LocalDate dob = LocalDate.parse(dobField.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                String type = (String) typeCombo.getSelectedItem();
                BigDecimal deposit = new BigDecimal(depositField.getText().trim());
                String pin = new String(pinField.getPassword()).trim();
                String pwd = new String(pwdField.getPassword()).trim();
                if (pwd.isEmpty()) pwd = pin;

                String adminId = currentAdmin != null ? currentAdmin.getAdminId() : "A1001";
                Account created = mainFrame.getBank().openAccount(name, dob, phone, address, type, deposit, pin, pwd, adminId);
                Card card = mainFrame.getBank().findCardByAccount(created.getAccountNumber());

                String msg = String.format("Account created successfully!\n\nCustomer ID: %s\nAccount Number: %s\nAccount Type: %s\nCard Number: %s\nInitial Balance: Rs. %.2f",
                        created.getCustomerId(), created.getAccountNumber(), created.getAccountType(),
                        card != null ? card.getCardNumber() : "N/A", created.getBalance().doubleValue());
                JOptionPane.showMessageDialog(panel, msg, "Account Created", JOptionPane.INFORMATION_MESSAGE);

                nameField.setText("");
                dobField.setText("");
                phoneField.setText("");
                addressField.setText("");
                depositField.setText("");
                pinField.setText("");
                pwdField.setText("");
                refreshTables();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, ex.getMessage(), "Account Creation Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 2;
        form.add(createBtn, g);

        panel.add(form);
        return panel;
    }

    private GridBagConstraints setGbc(GridBagConstraints g, int x, int y) {
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = 1;
        return g;
    }

    private JPanel createCloseAccountTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        box.add(new JLabel("Account Number to Close:"), g);

        JTextField accField = new JTextField(15);
        g.gridx = 1;
        box.add(accField, g);

        JButton closeBtn = new JButton("Close Account");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeBtn.setBackground(new Color(180, 50, 50));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> {
            String accNum = accField.getText().trim();
            if (accNum.isEmpty()) return;

            try {
                Account acc = mainFrame.getBank().findAccount(accNum);
                int confirm = JOptionPane.showConfirmDialog(panel,
                        "Are you sure you want to close account " + accNum + " (Balance: Rs. " + acc.getBalance().toPlainString() + ")?",
                        "Confirm Closure", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;

                String adminId = currentAdmin != null ? currentAdmin.getAdminId() : "A1001";
                mainFrame.getBank().closeAccount(accNum, adminId);
                JOptionPane.showMessageDialog(panel, "Account " + accNum + " closed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                accField.setText("");
                refreshTables();
            } catch (BankException ex) {
                JOptionPane.showMessageDialog(panel, ex.getMessage(), "Closure Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 2;
        box.add(closeBtn, g);

        panel.add(box);
        return panel;
    }

    private JPanel createCustomersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Customer ID", "Name", "Date of Birth", "Phone", "Address", "Accounts Count"};
        customersModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(customersModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Customers");
        refreshBtn.addActionListener(e -> refreshCustomersTable());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshCustomersTable() {
        if (customersModel == null) return;
        customersModel.setRowCount(0);
        Bank bank = mainFrame.getBank();
        for (Customer c : bank.getCustomerList()) {
            customersModel.addRow(new Object[]{
                    c.getUserId(),
                    c.getName(),
                    c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "",
                    c.getPhone(),
                    c.getAddress(),
                    c.getAccounts().size()
            });
        }
    }

    private JPanel createTransactionsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Txn ID", "Account", "Type", "Amount (Rs.)", "Date & Time", "Status", "Balance After (Rs.)", "Reference"};
        transactionsModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(transactionsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Transactions");
        refreshBtn.addActionListener(e -> refreshTransactionsTable());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshTransactionsTable() {
        if (transactionsModel == null) return;
        transactionsModel.setRowCount(0);
        Bank bank = mainFrame.getBank();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Account a : bank.getAccounts().values()) {
            for (Transaction t : a.getHistory()) {
                transactionsModel.addRow(new Object[]{
                        t.getTransactionId(),
                        t.getAccountNumber(),
                        t.getType(),
                        t.getAmount() != null ? String.format("%.2f", t.getAmount().doubleValue()) : "0.00",
                        t.getDateTime() != null ? t.getDateTime().format(fmt) : "",
                        t.getStatus(),
                        t.getBalanceAfter() != null ? String.format("%.2f", t.getBalanceAfter().doubleValue()) : "0.00",
                        t.getReferenceId() != null ? t.getReferenceId() : ""
                });
            }
        }
    }

    private JPanel createBlockedCardsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Card Number", "Account Number", "Expiry Date", "Failed Attempts", "Blocked Status"};
        blockedCardsModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(blockedCardsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton unblockBtn = new JButton("Unblock Selected Card");
        unblockBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        unblockBtn.setBackground(new Color(40, 130, 60));
        unblockBtn.setForeground(Color.WHITE);
        unblockBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(panel, "Please select a card from the table.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String cardNum = (String) blockedCardsModel.getValueAt(row, 0);
            try {
                String adminId = currentAdmin != null ? currentAdmin.getAdminId() : "A1001";
                mainFrame.getBank().unblockCard(cardNum, adminId);
                JOptionPane.showMessageDialog(panel, "Card " + cardNum + " unblocked successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshBlockedCardsTable();
            } catch (BankException ex) {
                JOptionPane.showMessageDialog(panel, ex.getMessage(), "Unblock Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshBlockedCardsTable());

        bottom.add(unblockBtn);
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshBlockedCardsTable() {
        if (blockedCardsModel == null) return;
        blockedCardsModel.setRowCount(0);
        Bank bank = mainFrame.getBank();
        for (Card c : bank.getBlockedCards()) {
            blockedCardsModel.addRow(new Object[]{
                    c.getCardNumber(),
                    c.getAccountNumber(),
                    c.getExpiryDate() != null ? c.getExpiryDate().toString() : "",
                    c.getFailedAttempts(),
                    c.isBlocked() ? "BLOCKED" : "ACTIVE"
            });
        }
    }

    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        btnBar.setOpaque(false);

        JTextArea reportArea = new JTextArea();
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(reportArea);

        JButton summaryBtn = new JButton("Daily Transaction Summary");
        summaryBtn.addActionListener(e -> reportArea.setText(mainFrame.getBank().generateDailySummary()));

        JButton blockedBtn = new JButton("Blocked Cards Report");
        blockedBtn.addActionListener(e -> {
            List<Card> blocked = mainFrame.getBank().getBlockedCards();
            StringBuilder sb = new StringBuilder("BLOCKED CARDS REPORT\n========================================\n");
            for (Card c : blocked) {
                sb.append(String.format("Card: %s | Account: %s | Expiry: %s%n",
                        c.getCardNumber(), c.getAccountNumber(), c.getExpiryDate()));
            }
            if (blocked.isEmpty()) sb.append("No cards currently blocked.\n");
            reportArea.setText(sb.toString());
        });

        JButton lowBalBtn = new JButton("Accounts Below Minimum Balance");
        lowBalBtn.addActionListener(e -> {
            List<Account> low = mainFrame.getBank().getLowBalanceAccounts();
            StringBuilder sb = new StringBuilder("LOW BALANCE ACCOUNTS REPORT\n========================================\n");
            for (Account a : low) {
                sb.append(String.format("Account: %s | Type: %s | Balance: Rs. %.2f%n",
                        a.getAccountNumber(), a.getAccountType(), a.getBalance().doubleValue()));
            }
            if (low.isEmpty()) sb.append("No accounts below minimum balance / overdrawn.\n");
            reportArea.setText(sb.toString());
        });

        JButton custReportBtn = new JButton("Customer List");
        custReportBtn.addActionListener(e -> {
            List<Customer> custs = mainFrame.getBank().getCustomerList();
            StringBuilder sb = new StringBuilder("REGISTERED CUSTOMERS REPORT\n========================================\n");
            for (Customer c : custs) {
                sb.append(String.format("ID: %s | Name: %s | Phone: %s | Accounts: %d%n",
                        c.getUserId(), c.getName(), c.getPhone(), c.getAccounts().size()));
            }
            reportArea.setText(sb.toString());
        });

        btnBar.add(summaryBtn);
        btnBar.add(blockedBtn);
        btnBar.add(lowBalBtn);
        btnBar.add(custReportBtn);

        panel.add(btnBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void executeMonthEnd() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Execute month-end processing now?\nThis credits 4% interest to Savings accounts and applies charges to Current accounts.",
                "Confirm Month-End", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            List<Transaction> created = mainFrame.getBank().runMonthEnd();
            JOptionPane.showMessageDialog(this,
                    "Month-end processing complete!\nGenerated " + created.size() + " interest/charge transactions.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshTables();
        } catch (BankException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Month-End Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void uploadBatchScript() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Instructions Text File");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            try {
                InstructionProcessor proc = new InstructionProcessor(mainFrame.getBank());
                String result = proc.processFile(selected);
                JTextArea area = new JTextArea(result, 20, 50);
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane sp = new JScrollPane(area);
                JOptionPane.showMessageDialog(this, sp, "Batch Instruction Execution Report", JOptionPane.INFORMATION_MESSAGE);
                refreshTables();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error processing file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshTables() {
        refreshCustomersTable();
        refreshTransactionsTable();
        refreshBlockedCardsTable();
    }

    private void logout() {
        if (currentAdmin != null) {
            currentAdmin.logout();
        }
        currentAdmin = null;
        mainFrame.showLogin();
    }
}
