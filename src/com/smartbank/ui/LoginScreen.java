package com.smartbank.ui;

import com.smartbank.batch.InstructionProcessor;
import com.smartbank.model.Admin;
import com.smartbank.model.Customer;
import com.smartbank.service.Bank;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginScreen extends JPanel {
    private final MainFrame mainFrame;
    private JComboBox<String> roleCombo;
    private JTextField idField;
    private JPasswordField passwordField;
    private JLabel idLabel;
    private JLabel passwordLabel;

    public LoginScreen(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(24, 76, 120));
        headerPanel.setPreferredSize(new Dimension(800, 80));
        JLabel titleLabel = new JLabel("Smart Bank and ATM Simulation System", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel formBox = new JPanel(new GridBagLayout());
        formBox.setBackground(Color.WHITE);
        formBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)));

        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.insets = new Insets(8, 8, 8, 8);
        fgbc.anchor = GridBagConstraints.WEST;
        fgbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel formTitle = new JLabel("System Login");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        formTitle.setForeground(new Color(24, 76, 120));
        fgbc.gridx = 0;
        fgbc.gridy = 0;
        fgbc.gridwidth = 2;
        formBox.add(formTitle, fgbc);

        fgbc.gridwidth = 1;
        fgbc.gridy = 1;
        fgbc.gridx = 0;
        formBox.add(new JLabel("Select Role:"), fgbc);

        roleCombo = new JComboBox<>(new String[]{"Customer", "Admin", "ATM Mode"});
        roleCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        roleCombo.addActionListener(e -> onRoleChanged());
        fgbc.gridx = 1;
        formBox.add(roleCombo, fgbc);

        fgbc.gridy = 2;
        fgbc.gridx = 0;
        idLabel = new JLabel("Customer ID:");
        formBox.add(idLabel, fgbc);

        idField = new JTextField(15);
        idField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fgbc.gridx = 1;
        formBox.add(idField, fgbc);

        fgbc.gridy = 3;
        fgbc.gridx = 0;
        passwordLabel = new JLabel("Password:");
        formBox.add(passwordLabel, fgbc);

        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fgbc.gridx = 1;
        formBox.add(passwordField, fgbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setBackground(new Color(24, 76, 120));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.addActionListener(e -> performLogin());

        JButton exitBtn = new JButton("Exit");
        exitBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        exitBtn.addActionListener(e -> System.exit(0));

        btnPanel.add(exitBtn);
        btnPanel.add(loginBtn);

        fgbc.gridy = 4;
        fgbc.gridx = 0;
        fgbc.gridwidth = 2;
        formBox.add(btnPanel, fgbc);

        JPanel batchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        batchPanel.setOpaque(false);
        JButton batchBtn = new JButton("Upload Instructions File");
        batchBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        batchBtn.addActionListener(e -> uploadInstructionsFile());
        batchPanel.add(batchBtn);

        fgbc.gridy = 5;
        formBox.add(batchPanel, fgbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(formBox, gbc);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void onRoleChanged() {
        String role = (String) roleCombo.getSelectedItem();
        if ("Customer".equals(role)) {
            idLabel.setText("Customer ID:");
            passwordLabel.setText("Password:");
            idField.setEnabled(true);
            passwordField.setEnabled(true);
        } else if ("Admin".equals(role)) {
            idLabel.setText("Admin ID:");
            passwordLabel.setText("Password:");
            idField.setEnabled(true);
            passwordField.setEnabled(true);
        } else {
            mainFrame.showAtmScreen();
        }
    }

    private void performLogin() {
        String role = (String) roleCombo.getSelectedItem();
        String id = idField.getText().trim();
        String pwd = new String(passwordField.getPassword()).trim();

        if ("ATM Mode".equals(role)) {
            mainFrame.showAtmScreen();
            return;
        }

        Bank bank = mainFrame.getBank();

        if ("Customer".equals(role)) {
            Customer cust = bank.findCustomer(id);
            if (cust != null && cust.login(id, pwd)) {
                mainFrame.showCustomerDashboard(cust);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid ID or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } else if ("Admin".equals(role)) {
            Admin admin = bank.findAdmin(id);
            if (admin != null && admin.login(id, pwd)) {
                mainFrame.showAdminPanel(admin);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid ID or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void uploadInstructionsFile() {
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
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error processing file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void resetFields() {
        idField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        onRoleChanged();
    }
}
