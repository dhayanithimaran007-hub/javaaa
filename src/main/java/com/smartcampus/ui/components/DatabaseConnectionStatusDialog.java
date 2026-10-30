package com.smartcampus.ui.components;

import com.smartcampus.config.AppConfig;
import com.smartcampus.config.DBConnection;
import com.smartcampus.config.DatabaseInitializer;
import com.smartcampus.service.DatabaseHealthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class DatabaseConnectionStatusDialog extends JDialog {
    private final JTextField txtUrl;
    private final JTextField txtUser;
    private final JPasswordField txtPassword;
    private final JCheckBox chkPersist;
    private final JLabel lblStatus;
    private final JProgressBar progressBar;
    private final ModernButton btnTest;
    private final ModernButton btnSave;
    private final ModernButton btnClose;

    private final DatabaseHealthService healthService = new DatabaseHealthService();

    public DatabaseConnectionStatusDialog(JFrame parent) {
        super(parent, "MySQL Database Connection Status & Configuration", true);
        setSize(560, 420);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Database Configuration & Health Check");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Configure MySQL parameters and verify live database connectivity");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form fields
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUrl = new JTextField(AppConfig.getDbUrl());
        txtUser = new JTextField(AppConfig.getDbUser());
        txtPassword = new JPasswordField(AppConfig.getDbPassword() != null ? AppConfig.getDbPassword() : "");
        chkPersist = new JCheckBox("Save changes to config/application.properties", true);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        contentPanel.add(new JLabel("JDBC URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        contentPanel.add(txtUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25;
        contentPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        contentPanel.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25;
        contentPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        contentPanel.add(txtPassword, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        contentPanel.add(chkPersist, gbc);

        // Status area
        JPanel statusBox = new JPanel();
        statusBox.setLayout(new BoxLayout(statusBox, BoxLayout.Y_AXIS));
        statusBox.setBorder(BorderFactory.createTitledBorder("Live Health Status"));

        lblStatus = new JLabel("Status: Unknown (Click 'Test Connection' below)");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        statusBox.add(lblStatus);
        statusBox.add(Box.createVerticalStrut(6));
        statusBox.add(progressBar);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        contentPanel.add(statusBox, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnTest = new ModernButton("Test Connection", ModernButton.ButtonType.SECONDARY);
        btnSave = new ModernButton("Save & Initialize", ModernButton.ButtonType.PRIMARY);
        btnClose = new ModernButton("Close", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnTest);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnClose);
        add(buttonPanel, BorderLayout.SOUTH);

        // Handlers
        btnTest.addActionListener(e -> runHealthCheck(false));
        btnSave.addActionListener(e -> saveAndApply());
        btnClose.addActionListener(e -> dispose());

        // Initial check
        runHealthCheck(false);
    }

    private void runHealthCheck(boolean silentSuccess) {
        setWorking(true);
        lblStatus.setText("Testing connection to MySQL server...");
        lblStatus.setForeground(new Color(71, 85, 105));

        String candidateUrl = txtUrl.getText().trim();
        String candidateUser = txtUser.getText().trim();
        String candidatePassword = new String(txtPassword.getPassword());

        new SwingWorker<DatabaseHealthService.HealthReport, Void>() {
            @Override
            protected DatabaseHealthService.HealthReport doInBackground() {
                try {
                    boolean ok = DBConnection.testSpecificConnection(candidateUrl, candidateUser, candidatePassword);
                    if (ok) {
                        return new DatabaseHealthService.HealthReport(true, 15, "smart_campus_db", 6, true,
                                "Successfully connected to MySQL server!");
                    } else {
                        return new DatabaseHealthService.HealthReport(false, 0, "", 0, false, "Connection failed.");
                    }
                } catch (Exception ex) {
                    return new DatabaseHealthService.HealthReport(false, 0, "", 0, false, ex.getMessage());
                }
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    DatabaseHealthService.HealthReport report = get();
                    if (report.connected()) {
                        lblStatus.setText("Connected: " + report.message());
                        lblStatus.setForeground(new Color(22, 163, 74));
                        if (!silentSuccess) {
                            JOptionPane.showMessageDialog(DatabaseConnectionStatusDialog.this,
                                    "Database connection successful!\nMySQL is active and ready.",
                                    "Connection Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        lblStatus.setText("Failed: " + report.message());
                        lblStatus.setForeground(new Color(220, 38, 38));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Error: " + ex.getMessage());
                    lblStatus.setForeground(new Color(220, 38, 38));
                }
            }
        }.execute();
    }

    private void saveAndApply() {
        String url = txtUrl.getText().trim();
        String user = txtUser.getText().trim();
        String password = new String(txtPassword.getPassword());
        boolean persist = chkPersist.isSelected();

        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "JDBC URL, Username, and Password are all required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AppConfig.updateDbCredentials(url, user, password, persist);

        setWorking(true);
        lblStatus.setText("Applying credentials and initializing database schema...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    DatabaseInitializer.initializeDatabase();
                    return DBConnection.testConnection();
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setText("Connected and schema initialized successfully!");
                        lblStatus.setForeground(new Color(22, 163, 74));
                        JOptionPane.showMessageDialog(DatabaseConnectionStatusDialog.this,
                                "Database credentials saved and verified!\nSchema & default accounts initialized.",
                                "Setup Complete", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        lblStatus.setText("Failed to connect with new credentials.");
                        lblStatus.setForeground(new Color(220, 38, 38));
                        JOptionPane.showMessageDialog(DatabaseConnectionStatusDialog.this,
                                "Could not connect to MySQL with the entered credentials.\nPlease verify username and password.",
                                "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnTest.setEnabled(!working);
        btnSave.setEnabled(!working);
        txtUrl.setEnabled(!working);
        txtUser.setEnabled(!working);
        txtPassword.setEnabled(!working);
    }
}
