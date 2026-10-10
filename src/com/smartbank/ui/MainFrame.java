package com.smartbank.ui;

import com.smartbank.model.Admin;
import com.smartbank.model.Customer;
import com.smartbank.service.Bank;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class MainFrame extends JFrame {
    public static final String SCREEN_LOGIN = "LOGIN";
    public static final String SCREEN_CUSTOMER = "CUSTOMER";
    public static final String SCREEN_ADMIN = "ADMIN";
    public static final String SCREEN_ATM = "ATM";

    private final Bank bank;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    private LoginScreen loginScreen;
    private CustomerDashboard customerDashboard;
    private AdminPanel adminPanel;
    private ATMScreen atmScreen;

    public MainFrame(Bank bank) {
        this.bank = bank;
        setTitle("Smart Bank and ATM Simulation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 650));
        setSize(950, 700);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        initScreens();

        add(cardsPanel);
        showScreen(SCREEN_LOGIN);
    }

    private void initScreens() {
        loginScreen = new LoginScreen(this);
        customerDashboard = new CustomerDashboard(this);
        adminPanel = new AdminPanel(this);
        atmScreen = new ATMScreen(this);

        cardsPanel.add(loginScreen, SCREEN_LOGIN);
        cardsPanel.add(customerDashboard, SCREEN_CUSTOMER);
        cardsPanel.add(adminPanel, SCREEN_ADMIN);
        cardsPanel.add(atmScreen, SCREEN_ATM);
    }

    public Bank getBank() {
        return bank;
    }

    public void showScreen(String name) {
        cardLayout.show(cardsPanel, name);
    }

    public void showLogin() {
        loginScreen.resetFields();
        showScreen(SCREEN_LOGIN);
    }

    public void showCustomerDashboard(Customer customer) {
        customerDashboard.loadCustomer(customer);
        showScreen(SCREEN_CUSTOMER);
    }

    public void showAdminPanel(Admin admin) {
        adminPanel.loadAdmin(admin);
        showScreen(SCREEN_ADMIN);
    }

    public void showAtmScreen() {
        atmScreen.resetAtm();
        showScreen(SCREEN_ATM);
    }

    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }
}
