package com.smartcampus.ui;

import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public class RegistrationDialog extends JDialog {
    private final JTextField txtFullName;
    private final JTextField txtEmail;
    private final JPasswordField txtPassword;
    private final JPasswordField txtConfirmPassword;
    private final JTextField txtDepartment;
    private final JTextField txtPhone;
    private final JProgressBar progressBar;
    private final ModernButton btnRegister;
    private final ModernButton btnCancel;

    private final UserService userService = new UserService();

    public RegistrationDialog(JFrame parent) {
        super(parent, "Student Self-Registration", true);
        setSize(480, 460);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Create Student Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Role: Student (Staff/Admin accounts are assigned by university IT)");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtFullName = new JTextField();
        txtEmail = new JTextField();
        txtPassword = new JPasswordField();
        txtConfirmPassword = new JPasswordField();
        txtDepartment = new JTextField();
        txtPhone = new JTextField();

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Full Name *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Email Address *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Department / Major:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtDepartment, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Contact Phone:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Password *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Confirm Password *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtConfirmPassword, gbc);

        // Security hint
        JLabel lblHint = new JLabel("<html><font color='#64748b'>Min 8 characters, must include both letters and numbers.</font></html>");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridx = 1; gbc.gridy = 6;
        formPanel.add(lblHint, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnRegister = new ModernButton("Create Account", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnRegister);
        add(buttonPanel, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> handleRegistration());
        btnCancel.addActionListener(e -> dispose());
    }

    private void handleRegistration() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        char[] pass = txtPassword.getPassword();
        char[] confirmPass = txtConfirmPassword.getPassword();
        String department = txtDepartment.getText().trim();
        String phone = txtPhone.getText().trim();

        if (fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full Name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Arrays.equals(pass, confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordStr = new String(pass);
        if (!ValidationUtil.isStrongPassword(passwordStr)) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters long and contain both letters and digits.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setWorking(true);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return userService.registerStudent(fullName, email, passwordStr, department, phone);
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    User created = get();
                    JOptionPane.showMessageDialog(RegistrationDialog.this,
                            "Registration successful! Welcome, " + created.getFullName() + ".\n" +
                            "You can now log in using your student email.",
                            "Account Created", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(RegistrationDialog.this,
                            "Registration failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnRegister.setEnabled(!working);
        btnCancel.setEnabled(!working);
        txtFullName.setEnabled(!working);
        txtEmail.setEnabled(!working);
        txtPassword.setEnabled(!working);
        txtConfirmPassword.setEnabled(!working);
        txtDepartment.setEnabled(!working);
        txtPhone.setEnabled(!working);
    }
}
