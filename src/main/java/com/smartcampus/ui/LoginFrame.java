package com.smartcampus.ui;

import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.admin.AdminDashboard;
import com.smartcampus.ui.components.DatabaseConnectionStatusDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.staff.StaffDashboard;
import com.smartcampus.ui.student.StudentDashboard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginFrame extends JFrame {
    private final JTextField txtEmail;
    private final JPasswordField txtPassword;
    private final JProgressBar progressBar;
    private final ModernButton btnLogin;
    private final ModernButton btnRegister;
    private final ModernButton btnDbConfig;
    private final JComboBox<String> cmbQuickFill;

    private final UserService userService = new UserService();

    public LoginFrame() {
        super("Smart Campus Complaint Prioritization and Tracking System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // Left Branding Banner
        JPanel leftBanner = new JPanel();
        leftBanner.setPreferredSize(new Dimension(320, 520));
        leftBanner.setBackground(new Color(24, 32, 54));
        leftBanner.setLayout(new BoxLayout(leftBanner, BoxLayout.Y_AXIS));
        leftBanner.setBorder(BorderFactory.createEmptyBorder(40, 28, 40, 28));

        JLabel appBadge = new JLabel("CAMPUS OPS");
        appBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        appBadge.setForeground(new Color(59, 130, 246));

        JLabel title1 = new JLabel("Smart Campus");
        title1.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title1.setForeground(Color.WHITE);

        JLabel title2 = new JLabel("Complaint Tracking");
        title2.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        title2.setForeground(new Color(226, 232, 240));

        JLabel desc = new JLabel("<html><body style='width: 200px; color: #94a3b8; font-size: 11px; margin-top: 15px; line-height: 1.5;'>" +
                "Automated priority scoring engine, multi-role dispatch, real-time status tracking, and audit-logged operations." +
                "</body></html>");

        JLabel version = new JLabel("v1.0.0 Enterprise Desktop");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(100, 116, 139));

        leftBanner.add(appBadge);
        leftBanner.add(Box.createVerticalStrut(10));
        leftBanner.add(title1);
        leftBanner.add(title2);
        leftBanner.add(Box.createVerticalStrut(20));
        leftBanner.add(desc);
        leftBanner.add(Box.createVerticalGlue());
        leftBanner.add(version);

        add(leftBanner, BorderLayout.WEST);

        // Right Login Form
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(248, 250, 252));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblWelcome = new JLabel("Welcome Back");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Please enter your campus credentials to log in");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblWelcome, gbc);
        gbc.gridy = 1;
        formCard.add(lblSub, gbc);

        // Quick demo fill selector for academic evaluation
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoPanel.setOpaque(false);
        demoPanel.add(new JLabel("Quick Demo Account:"));
        cmbQuickFill = new JComboBox<>(new String[]{
                "-- Select Demo User --",
                "Administrator (admin@campus.edu)",
                "Student: Alice (alice@student.campus.edu)",
                "Staff: Electrician (electrician@campus.edu)",
                "Staff: Plumber (plumber@campus.edu)"
        });
        cmbQuickFill.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        demoPanel.add(cmbQuickFill);

        gbc.gridy = 2;
        formCard.add(demoPanel, gbc);

        // Inputs
        JLabel lblEmail = new JLabel("Email Address");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtEmail = new JTextField(24);
        txtEmail.setPreferredSize(new Dimension(300, 34));

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtPassword = new JPasswordField(24);
        txtPassword.setPreferredSize(new Dimension(300, 34));

        gbc.gridy = 3; formCard.add(lblEmail, gbc);
        gbc.gridy = 4; formCard.add(txtEmail, gbc);
        gbc.gridy = 5; formCard.add(lblPass, gbc);
        gbc.gridy = 6; formCard.add(txtPassword, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridy = 7;
        formCard.add(progressBar, gbc);

        btnLogin = new ModernButton("Sign In", ModernButton.ButtonType.PRIMARY);
        btnLogin.setPreferredSize(new Dimension(300, 38));
        gbc.gridy = 8;
        formCard.add(btnLogin, gbc);

        // Register student button
        btnRegister = new ModernButton("New Student? Register Here", ModernButton.ButtonType.SECONDARY);
        gbc.gridy = 9;
        formCard.add(btnRegister, gbc);

        rightPanel.add(formCard, BorderLayout.CENTER);

        // Bottom toolbar: DB settings
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setOpaque(false);
        btnDbConfig = new ModernButton("Database Settings", ModernButton.ButtonType.SECONDARY);
        btnDbConfig.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottomBar.add(btnDbConfig);
        rightPanel.add(bottomBar, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);

        // Listeners
        btnLogin.addActionListener(e -> handleLogin());
        btnRegister.addActionListener(e -> new RegistrationDialog(this).setVisible(true));
        btnDbConfig.addActionListener(e -> new DatabaseConnectionStatusDialog(this).setVisible(true));

        // Enter key to submit
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        txtEmail.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        // Demo account auto-fill helper
        cmbQuickFill.addActionListener(e -> {
            int idx = cmbQuickFill.getSelectedIndex();
            switch (idx) {
                case 1 -> { txtEmail.setText("admin@campus.edu"); txtPassword.setText("Admin@123"); }
                case 2 -> { txtEmail.setText("alice@student.campus.edu"); txtPassword.setText("Student@123"); }
                case 3 -> { txtEmail.setText("electrician@campus.edu"); txtPassword.setText("Staff@123"); }
                case 4 -> { txtEmail.setText("plumber@campus.edu"); txtPassword.setText("Staff@123"); }
            }
        });
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        char[] pass = txtPassword.getPassword();

        if (email.isEmpty() || pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter both email and password.", "Login Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordStr = new String(pass);
        setWorking(true);

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return userService.authenticate(email, passwordStr);
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    User user = get();
                    UserSession.login(user);
                    openRoleDashboard(user);
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Login failed: " + msg, "Authentication Failure", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openRoleDashboard(User user) {
        dispose(); // Close login frame
        switch (user.getRole()) {
            case STUDENT -> new StudentDashboard().setVisible(true);
            case ADMIN -> new AdminDashboard().setVisible(true);
            case STAFF -> new StaffDashboard().setVisible(true);
        }
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnLogin.setEnabled(!working);
        btnRegister.setEnabled(!working);
        btnDbConfig.setEnabled(!working);
        txtEmail.setEnabled(!working);
        txtPassword.setEnabled(!working);
        cmbQuickFill.setEnabled(!working);
    }
}
