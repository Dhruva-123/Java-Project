package com.smartbank.ui;

import com.smartbank.exception.BankException;
import com.smartbank.model.Account;
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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

public class CustomerDashboard extends JPanel {
    private final MainFrame mainFrame;
    private Customer currentCustomer;
    private JLabel welcomeLabel;
    private JComboBox<String> accountSelector;
    private JLabel balanceLabel;
    private JLabel accountTypeLabel;
    private JLabel statusLabel;

    public CustomerDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(24, 76, 120));
        topBar.setPreferredSize(new Dimension(800, 70));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        welcomeLabel = new JLabel("Welcome, Customer");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topBar.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logoutBtn.addActionListener(e -> logout());
        topBar.add(logoutBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel accountInfoCard = new JPanel(new GridBagLayout());
        accountInfoCard.setBackground(Color.WHITE);
        accountInfoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        accountInfoCard.add(new JLabel("Select Account:"), gbc);

        accountSelector = new JComboBox<>();
        accountSelector.setFont(new Font("SansSerif", Font.PLAIN, 14));
        accountSelector.addActionListener(e -> onAccountSelected());
        gbc.gridx = 1;
        accountInfoCard.add(accountSelector, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        accountInfoCard.add(new JLabel("Account Type:"), gbc);

        accountTypeLabel = new JLabel("-");
        accountTypeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridx = 1;
        accountInfoCard.add(accountTypeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        accountInfoCard.add(new JLabel("Current Balance:"), gbc);

        balanceLabel = new JLabel("Rs. 0.00");
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        balanceLabel.setForeground(new Color(34, 139, 34));
        gbc.gridx = 1;
        accountInfoCard.add(balanceLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        accountInfoCard.add(new JLabel("Status:"), gbc);

        statusLabel = new JLabel("-");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridx = 1;
        accountInfoCard.add(statusLabel, gbc);

        contentPanel.add(accountInfoCard, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        actionsPanel.setOpaque(false);

        JButton depositBtn = createActionButton("Deposit", new Color(46, 139, 87));
        depositBtn.addActionListener(e -> showDepositDialog());

        JButton withdrawBtn = createActionButton("Withdraw", new Color(178, 34, 34));
        withdrawBtn.addActionListener(e -> showWithdrawDialog());

        JButton transferBtn = createActionButton("Transfer Funds", new Color(70, 130, 180));
        transferBtn.addActionListener(e -> showTransferDialog());

        JButton miniStatementBtn = createActionButton("Mini Statement", new Color(112, 128, 144));
        miniStatementBtn.addActionListener(e -> showMiniStatementDialog());

        JButton changePinBtn = createActionButton("Change PIN", new Color(106, 90, 205));
        changePinBtn.addActionListener(e -> showChangePinDialog());

        JButton refreshBtn = createActionButton("Refresh Balance", new Color(47, 79, 79));
        refreshBtn.addActionListener(e -> refreshAccountView());

        actionsPanel.add(depositBtn);
        actionsPanel.add(withdrawBtn);
        actionsPanel.add(transferBtn);
        actionsPanel.add(miniStatementBtn);
        actionsPanel.add(changePinBtn);
        actionsPanel.add(refreshBtn);

        contentPanel.add(actionsPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    public void loadCustomer(Customer customer) {
        this.currentCustomer = customer;
        if (customer != null) {
            welcomeLabel.setText("Welcome, " + customer.getName() + " (" + customer.getUserId() + ")");
            accountSelector.removeAllItems();
            for (Account acc : customer.getAccounts()) {
                accountSelector.addItem(acc.getAccountNumber());
            }
            if (accountSelector.getItemCount() > 0) {
                accountSelector.setSelectedIndex(0);
                refreshAccountView();
            }
        }
    }

    private Account getSelectedAccount() {
        if (currentCustomer == null || accountSelector.getSelectedItem() == null) {
            return null;
        }
        String accNum = (String) accountSelector.getSelectedItem();
        try {
            return mainFrame.getBank().findAccount(accNum);
        } catch (Exception e) {
            return null;
        }
    }

    private void onAccountSelected() {
        refreshAccountView();
    }

    private void refreshAccountView() {
        Account acc = getSelectedAccount();
        if (acc != null) {
            accountTypeLabel.setText(acc.getAccountType());
            balanceLabel.setText(String.format("Rs. %.2f", acc.getBalance().doubleValue()));
            statusLabel.setText(acc.getStatus());
            if (acc.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                balanceLabel.setForeground(Color.RED);
            } else {
                balanceLabel.setForeground(new Color(34, 139, 34));
            }
        } else {
            accountTypeLabel.setText("-");
            balanceLabel.setText("Rs. 0.00");
            statusLabel.setText("-");
        }
    }

    private void showDepositDialog() {
        Account acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Please select an account first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Enter amount to deposit (Rs.):", "Deposit", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal amt = new BigDecimal(input.trim());
            mainFrame.getBank().deposit(acc.getAccountNumber(), amt);
            refreshAccountView();
            JOptionPane.showMessageDialog(this, "Successfully deposited Rs. " + amt.toPlainString(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Deposit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showWithdrawDialog() {
        Account acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Please select an account first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Enter amount to withdraw (Rs.):", "Withdraw", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal amt = new BigDecimal(input.trim());
            mainFrame.getBank().withdraw(acc.getAccountNumber(), amt);
            refreshAccountView();
            JOptionPane.showMessageDialog(this, "Successfully withdrew Rs. " + amt.toPlainString(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Withdrawal Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTransferDialog() {
        Account acc = getSelectedAccount();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Please select an account first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField dstField = new JTextField();
        JTextField amtField = new JTextField();
        panel.add(new JLabel("Destination Account Number:"));
        panel.add(dstField);
        panel.add(new JLabel("Amount (Rs.):"));
        panel.add(amtField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Fund Transfer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String dstNum = dstField.getText().trim();
        String amtStr = amtField.getText().trim();

        try {
            Account dstAcc = mainFrame.getBank().findAccount(dstNum);
            Customer dstOwner = mainFrame.getBank().findCustomer(dstAcc.getCustomerId());
            String ownerName = dstOwner != null ? dstOwner.getName() : "Account Holder";

            BigDecimal amt = new BigDecimal(amtStr);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Confirm transfer of Rs. " + amt.toPlainString() + " to " + ownerName + " (Account: " + dstNum + ")?",
                    "Confirm Transfer", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            mainFrame.getBank().transfer(acc.getAccountNumber(), dstNum, amt);
            refreshAccountView();
            JOptionPane.showMessageDialog(this, "Transfer successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Transfer Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMiniStatementDialog() {
        Account acc = getSelectedAccount();
        if (acc == null) return;

        JDialog dialog = new JDialog(mainFrame, "Mini Statement - " + acc.getAccountNumber(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Txn ID", "Date Time", "Type", "Amount (Rs.)", "Balance After (Rs.)", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        List<Transaction> mini = acc.getMiniStatement();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Transaction t : mini) {
            model.addRow(new Object[]{
                    t.getTransactionId(),
                    t.getDateTime() != null ? t.getDateTime().format(fmt) : "",
                    t.getType(),
                    t.getAmount() != null ? String.format("%.2f", t.getAmount().doubleValue()) : "0.00",
                    t.getBalanceAfter() != null ? String.format("%.2f", t.getBalanceAfter().doubleValue()) : "0.00",
                    t.getStatus()
            });
        }

        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton printBtn = new JButton("Print Statement");
        printBtn.addActionListener(e -> {
            String receipt = acc.printStatement();
            JTextArea area = new JTextArea(receipt, 15, 60);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setEditable(false);
            JOptionPane.showMessageDialog(dialog, new JScrollPane(area), "Printed Statement", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(printBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showChangePinDialog() {
        Account acc = getSelectedAccount();
        if (acc == null) return;

        Card card = mainFrame.getBank().findCardByAccount(acc.getAccountNumber());
        if (card == null) {
            JOptionPane.showMessageDialog(this, "No debit card linked to this account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        JPasswordField oldPinField = new JPasswordField();
        JPasswordField newPinField = new JPasswordField();
        panel.add(new JLabel("Old 4-digit PIN:"));
        panel.add(oldPinField);
        panel.add(new JLabel("New 4-digit PIN:"));
        panel.add(newPinField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change PIN for Card " + card.getCardNumber(), JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String oldPin = new String(oldPinField.getPassword()).trim();
        String newPin = new String(newPinField.getPassword()).trim();

        try {
            mainFrame.getBank().changePin(card.getCardNumber(), oldPin, newPin);
            JOptionPane.showMessageDialog(this, "PIN successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "PIN Change Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        if (currentCustomer != null) {
            currentCustomer.logout();
        }
        currentCustomer = null;
        mainFrame.showLogin();
    }
}
