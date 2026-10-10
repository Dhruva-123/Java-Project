package com.smartbank.ui;

import com.smartbank.config.Config;
import com.smartbank.exception.BankException;
import com.smartbank.model.ATM;
import com.smartbank.model.Card;
import com.smartbank.model.Transaction;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class ATMScreen extends JPanel {
    private static final String PANEL_AUTH = "AUTH";
    private static final String PANEL_MENU = "MENU";

    private final MainFrame mainFrame;
    private final CardLayout cardLayout;
    private final JPanel containerPanel;

    private JTextField cardNumberField;
    private JPasswordField pinField;
    private JLabel statusMessageLabel;

    private JLabel sessionInfoLabel;
    private Timer sessionTimer;
    private int remainingSeconds = Config.ATM_SESSION_TIMEOUT_SECONDS;
    private JLabel timerLabel;

    public ATMScreen(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(230, 235, 240));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 50, 90));
        header.setPreferredSize(new Dimension(800, 70));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("Smart Bank ATM Simulation", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JButton exitToBankBtn = new JButton("Exit to Main Menu");
        exitToBankBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        exitToBankBtn.addActionListener(e -> {
            endAtmSession();
            mainFrame.showLogin();
        });
        header.add(exitToBankBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        containerPanel.setOpaque(false);

        JPanel authPanel = createAuthPanel();
        JPanel menuPanel = createMenuPanel();

        containerPanel.add(authPanel, PANEL_AUTH);
        containerPanel.add(menuPanel, PANEL_MENU);

        add(containerPanel, BorderLayout.CENTER);

        initSessionTimer();
    }

    private void initSessionTimer() {
        sessionTimer = new Timer(1000, e -> {
            remainingSeconds--;
            if (timerLabel != null) {
                timerLabel.setText("Session Timeout: " + remainingSeconds + "s");
            }
            if (remainingSeconds <= 0) {
                sessionTimer.stop();
                JOptionPane.showMessageDialog(this, "Session timed out due to inactivity.", "ATM Timeout", JOptionPane.WARNING_MESSAGE);
                endAtmSession();
            }
        });
    }

    private void resetSessionTimer() {
        remainingSeconds = Config.ATM_SESSION_TIMEOUT_SECONDS;
        if (timerLabel != null) {
            timerLabel.setText("Session Timeout: " + remainingSeconds + "s");
        }
        if (!sessionTimer.isRunning()) {
            sessionTimer.start();
        }
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 205), 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcome = new JLabel("Insert Card & Enter PIN", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcome.setForeground(new Color(20, 50, 90));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        box.add(welcome, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        box.add(new JLabel("Card Number (16-digit):"), gbc);

        cardNumberField = new JTextField(16);
        cardNumberField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridx = 1;
        box.add(cardNumberField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        box.add(new JLabel("4-Digit PIN:"), gbc);

        pinField = new JPasswordField(6);
        pinField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        gbc.gridx = 1;
        box.add(pinField, gbc);

        JPanel keypadPanel = new JPanel(new GridLayout(4, 3, 6, 6));
        keypadPanel.setOpaque(false);
        for (int i = 1; i <= 9; i++) {
            final String digit = String.valueOf(i);
            JButton b = new JButton(digit);
            b.setFont(new Font("SansSerif", Font.BOLD, 14));
            b.addActionListener(e -> appendPinDigit(digit));
            keypadPanel.add(b);
        }
        JButton clearBtn = new JButton("Clear");
        clearBtn.setBackground(new Color(210, 80, 80));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.addActionListener(e -> pinField.setText(""));
        keypadPanel.add(clearBtn);

        JButton zeroBtn = new JButton("0");
        zeroBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        zeroBtn.addActionListener(e -> appendPinDigit("0"));
        keypadPanel.add(zeroBtn);

        JButton enterBtn = new JButton("Enter");
        enterBtn.setBackground(new Color(40, 140, 60));
        enterBtn.setForeground(Color.WHITE);
        enterBtn.addActionListener(e -> authenticateCard());
        keypadPanel.add(enterBtn);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        box.add(keypadPanel, gbc);

        statusMessageLabel = new JLabel(" ", SwingConstants.CENTER);
        statusMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusMessageLabel.setForeground(Color.RED);
        gbc.gridy = 4;
        box.add(statusMessageLabel, gbc);

        panel.add(box);
        return panel;
    }

    private void appendPinDigit(String digit) {
        char[] existing = pinField.getPassword();
        if (existing.length < 4) {
            pinField.setText(new String(existing) + digit);
        }
    }

    private void authenticateCard() {
        String cardNum = cardNumberField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();

        if (cardNum.isEmpty() || pin.isEmpty()) {
            statusMessageLabel.setText("Please enter both card number and PIN.");
            return;
        }

        ATM atm = mainFrame.getBank().getAtm();
        try {
            atm.authenticate(cardNum, pin);
            statusMessageLabel.setText(" ");
            sessionInfoLabel.setText(String.format("Card: %s | Account: %s",
                    cardNum, atm.getCurrentSessionAccount().getAccountNumber()));
            resetSessionTimer();
            cardLayout.show(containerPanel, PANEL_MENU);
        } catch (BankException e) {
            statusMessageLabel.setText(e.getMessage());
            pinField.setText("");
        }
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setOpaque(false);
        sessionInfoLabel = new JLabel("ATM Session Active");
        sessionInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sessionInfoLabel.setForeground(new Color(20, 50, 90));
        topInfo.add(sessionInfoLabel, BorderLayout.WEST);

        timerLabel = new JLabel("Session Timeout: 60s");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        timerLabel.setForeground(new Color(180, 50, 50));
        topInfo.add(timerLabel, BorderLayout.EAST);
        panel.add(topInfo, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 2, 20, 20));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 10));

        JButton quickCashBtn = createMenuButton("Quick Cash (Rs. 1,000)", e -> atmWithdraw(new BigDecimal("1000.00")));
        JButton withdrawBtn = createMenuButton("Withdraw Cash", e -> showWithdrawOptions());
        JButton balanceBtn = createMenuButton("Balance Enquiry", e -> showAtmBalance());
        JButton miniStatementBtn = createMenuButton("Mini Statement", e -> showAtmMiniStatement());
        JButton changePinBtn = createMenuButton("Change PIN", e -> showAtmChangePin());
        JButton exitBtn = createMenuButton("Exit Session", e -> endAtmSession());
        exitBtn.setBackground(new Color(180, 50, 50));

        grid.add(quickCashBtn);
        grid.add(withdrawBtn);
        grid.add(balanceBtn);
        grid.add(miniStatementBtn);
        grid.add(changePinBtn);
        grid.add(exitBtn);

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JButton createMenuButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(new Color(30, 75, 130));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            resetSessionTimer();
            action.actionPerformed(e);
        });
        return btn;
    }

    private void showWithdrawOptions() {
        resetSessionTimer();
        String[] options = {"500", "1000", "2000", "5000", "Other Amount", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select withdrawal amount (Multiples of Rs. 100, Max Rs. 10,000):",
                "Withdraw Cash", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice >= 0 && choice <= 3) {
            BigDecimal amt = new BigDecimal(options[choice] + ".00");
            atmWithdraw(amt);
        } else if (choice == 4) {
            String input = JOptionPane.showInputDialog(this, "Enter amount (Multiples of Rs. 100):");
            if (input != null && !input.trim().isEmpty()) {
                try {
                    BigDecimal amt = new BigDecimal(input.trim());
                    atmWithdraw(amt);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void atmWithdraw(BigDecimal amount) {
        resetSessionTimer();
        ATM atm = mainFrame.getBank().getAtm();
        try {
            Transaction txn = atm.dispenseCash(amount);
            String receipt = atm.printReceipt(txn);

            int printChoice = JOptionPane.showConfirmDialog(this,
                    "Cash dispensed: Rs. " + amount.toPlainString() + "\nWould you like a printed receipt?",
                    "Dispense Cash Success", JOptionPane.YES_NO_OPTION);

            if (printChoice == JOptionPane.YES_OPTION) {
                JTextArea area = new JTextArea(receipt, 12, 35);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                area.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(area), "ATM Receipt", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Withdrawal Denied", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAtmBalance() {
        resetSessionTimer();
        ATM atm = mainFrame.getBank().getAtm();
        try {
            BigDecimal bal = atm.checkBalance();
            int print = JOptionPane.showConfirmDialog(this,
                    "Current Balance: Rs. " + bal.toPlainString() + "\nWould you like a balance receipt?",
                    "Balance Enquiry", JOptionPane.YES_NO_OPTION);
            if (print == JOptionPane.YES_OPTION) {
                String receipt = atm.printReceipt(null);
                JTextArea area = new JTextArea(receipt, 10, 35);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                area.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(area), "ATM Receipt", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAtmMiniStatement() {
        resetSessionTimer();
        ATM atm = mainFrame.getBank().getAtm();
        if (atm.getCurrentSessionAccount() != null) {
            String stmt = atm.getCurrentSessionAccount().printStatement();
            JTextArea area = new JTextArea(stmt, 14, 65);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Mini Statement", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAtmChangePin() {
        resetSessionTimer();
        ATM atm = mainFrame.getBank().getAtm();
        Card card = atm.getCurrentSessionCard();
        if (card == null) return;

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        JPasswordField oldPinField = new JPasswordField();
        JPasswordField newPinField = new JPasswordField();
        panel.add(new JLabel("Old 4-digit PIN:"));
        panel.add(oldPinField);
        panel.add(new JLabel("New 4-digit PIN:"));
        panel.add(newPinField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change ATM PIN", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String oldPin = new String(oldPinField.getPassword()).trim();
        String newPin = new String(newPinField.getPassword()).trim();

        try {
            mainFrame.getBank().changePin(card.getCardNumber(), oldPin, newPin);
            JOptionPane.showMessageDialog(this, "PIN successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (BankException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void endAtmSession() {
        if (sessionTimer != null) {
            sessionTimer.stop();
        }
        mainFrame.getBank().getAtm().endSession();
        resetAtm();
    }

    public void resetAtm() {
        if (sessionTimer != null) {
            sessionTimer.stop();
        }
        cardNumberField.setText("");
        pinField.setText("");
        statusMessageLabel.setText(" ");
        cardLayout.show(containerPanel, PANEL_AUTH);
    }
}
