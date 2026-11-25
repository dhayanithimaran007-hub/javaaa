package com.smartcampus.ui.components;

import com.smartcampus.config.AppConfig;
import com.smartcampus.service.DatabaseHealthService;
import com.smartcampus.ui.theme.DesignSystem;

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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Map;

public class DatabaseConnectionStatusDialog extends JDialog {
    private final JTextField txtUrl;
    private final JTextField txtUser;
    private final JPasswordField txtPassword;
    private final JCheckBox chkPersist;
    private final JLabel lblStatus;
    private final JLabel lblLatency;
    private final JProgressBar progressBar;
    private final ModernButton btnTest;
    private final ModernButton btnSave;
    private final ModernButton btnClose;

    private final DefaultTableModel tableModel;
    private final JTable tableChecklist;

    private final DatabaseHealthService healthService = new DatabaseHealthService();

    public DatabaseConnectionStatusDialog(JFrame parent) {
        super(parent, "MySQL Database Health & Infrastructure Telemetry", true);
        setSize(680, 560);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DesignSystem.MIDNIGHT_NAVY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Database Infrastructure & Schema Health");
        titleLabel.setFont(DesignSystem.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Real-time MySQL connection latency, table status verification, and credentials");
        subtitleLabel.setFont(DesignSystem.FONT_SMALL);
        subtitleLabel.setForeground(DesignSystem.TEXT_MUTED);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(DesignSystem.BG_CANVAS);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        CardPanel telemetryCard = new CardPanel(new GridLayout(1, 3, 10, 0));
        telemetryCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        telemetryCard.setMaximumSize(new Dimension(640, 75));

        lblStatus = new JLabel("Checking connection...");
        lblStatus.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblStatus.setForeground(DesignSystem.TEXT_PRIMARY);

        lblLatency = new JLabel("Ping: -- ms");
        lblLatency.setFont(DesignSystem.FONT_SMALL);
        lblLatency.setForeground(DesignSystem.TEXT_SECONDARY);

        JLabel lblDbName = new JLabel("DB: smart_campus_db");
        lblDbName.setFont(DesignSystem.FONT_SMALL);
        lblDbName.setForeground(DesignSystem.TEXT_MUTED);

        telemetryCard.add(lblStatus);
        telemetryCard.add(lblLatency);
        telemetryCard.add(lblDbName);
        centerPanel.add(telemetryCard);
        centerPanel.add(Box.createVerticalStrut(12));

        CardPanel checklistCard = new CardPanel(new BorderLayout(6, 6));
        checklistCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        checklistCard.setMaximumSize(new Dimension(640, 160));

        JLabel lblChecklistTitle = new JLabel("SCHEMA TABLE VERIFICATION & RECORD COUNTS");
        lblChecklistTitle.setFont(DesignSystem.FONT_CARD_LABEL);
        lblChecklistTitle.setForeground(DesignSystem.TEXT_SECONDARY);
        checklistCard.add(lblChecklistTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Table Name", "Status", "Live Record Count", "Integrity Check"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableChecklist = new JTable(tableModel);
        tableChecklist.setRowHeight(24);
        tableChecklist.setFont(DesignSystem.FONT_SMALL);
        tableChecklist.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableChecklist.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean ok = "Validated".equals(value);
                lbl.setIcon(ok ? VectorIcons.check(DesignSystem.SUCCESS_TEXT, 12) : VectorIcons.cross(DesignSystem.CRITICAL_TEXT, 12));
                lbl.setIconTextGap(6);
                lbl.setForeground(ok ? DesignSystem.SUCCESS_TEXT : DesignSystem.CRITICAL_TEXT);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                return lbl;
            }
        });
        checklistCard.add(new JScrollPane(tableChecklist), BorderLayout.CENTER);
        centerPanel.add(checklistCard);
        centerPanel.add(Box.createVerticalStrut(12));

        CardPanel formCard = new CardPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        formCard.setMaximumSize(new Dimension(640, 140));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUrl = new JTextField(AppConfig.getDbUrl());
        txtUrl.setFont(DesignSystem.FONT_SMALL);
        txtUser = new JTextField(AppConfig.getDbUser());
        txtUser.setFont(DesignSystem.FONT_SMALL);
        txtPassword = new JPasswordField(AppConfig.getDbPassword() != null ? AppConfig.getDbPassword() : "");
        chkPersist = new JCheckBox("Persist credentials to config/application.properties", true);
        chkPersist.setFont(DesignSystem.FONT_SMALL);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        formCard.add(new JLabel("JDBC URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        formCard.add(txtUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formCard.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        formCard.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formCard.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        formCard.add(txtPassword, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        formCard.add(chkPersist, gbc);

        centerPanel.add(formCard);
        add(centerPanel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setOpaque(false);

        btnTest = new ModernButton("Test Connection", ModernButton.ButtonType.SECONDARY);
        btnSave = new ModernButton("Save Configuration", ModernButton.ButtonType.PRIMARY);
        btnClose = new ModernButton("Close", ModernButton.ButtonType.GHOST);

        buttons.add(btnTest);
        buttons.add(btnSave);
        buttons.add(btnClose);

        bottomBar.add(progressBar, BorderLayout.NORTH);
        bottomBar.add(buttons, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        btnTest.addActionListener(e -> runHealthCheck());
        btnSave.addActionListener(e -> handleSave());
        btnClose.addActionListener(e -> dispose());

        runHealthCheck();
    }

    private void runHealthCheck() {
        btnTest.setEnabled(false);
        progressBar.setVisible(true);

        new SwingWorker<HealthPayload, Void>() {
            @Override
            protected HealthPayload doInBackground() {
                DatabaseHealthService.HealthReport report = healthService.checkHealth();
                Map<String, Integer> counts = healthService.getTableRecordCounts();
                return new HealthPayload(report, counts);
            }

            @Override
            protected void done() {
                btnTest.setEnabled(true);
                progressBar.setVisible(false);
                try {
                    HealthPayload data = get();
                    DatabaseHealthService.HealthReport report = data.report;

                    if (report.connected()) {
                        lblStatus.setIcon(VectorIcons.statusDot(DesignSystem.SUCCESS, 10));
                        lblStatus.setIconTextGap(6);
                        lblStatus.setText("Connected (Healthy)");
                        lblStatus.setForeground(DesignSystem.SUCCESS);
                        lblLatency.setText("Ping: " + report.latencyMs() + " ms");
                    } else {
                        lblStatus.setIcon(VectorIcons.cross(DesignSystem.CRITICAL, 12));
                        lblStatus.setIconTextGap(6);
                        lblStatus.setText("Disconnected");
                        lblStatus.setForeground(DesignSystem.CRITICAL);
                        lblLatency.setText("Error: " + report.message());
                    }

                    tableModel.setRowCount(0);
                    for (Map.Entry<String, Integer> entry : data.counts.entrySet()) {
                        int cnt = entry.getValue();
                        String statusStr = (cnt >= 0) ? "AVAILABLE" : "MISSING";
                        String cntStr = (cnt >= 0) ? String.valueOf(cnt) : "0";
                        String check = (cnt >= 0) ? "Validated" : "Error";
                        tableModel.addRow(new Object[]{entry.getKey(), statusStr, cntStr, check});
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Health check failed: " + ex.getMessage());
                    lblStatus.setForeground(DesignSystem.CRITICAL);
                }
            }
        }.execute();
    }

    private void handleSave() {
        String url = txtUrl.getText().trim();
        String user = txtUser.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        boolean persist = chkPersist.isSelected();

        if (url.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "URL and Username cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            AppConfig.updateDbCredentials(url, user, pass, persist);
            if (getParent() instanceof JFrame frame) {
                ToastNotification.showSuccess(frame, "Database credentials saved successfully.");
            }
            runHealthCheck();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record HealthPayload(DatabaseHealthService.HealthReport report, Map<String, Integer> counts) {}
}
