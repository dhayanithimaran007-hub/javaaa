package com.smartcampus.ui.components;

import com.smartcampus.security.UserSession;
import com.smartcampus.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public class ChangePasswordDialog extends JDialog {
    private final JPasswordField txtCurrentPassword;
    private final JPasswordField txtNewPassword;
    private final JPasswordField txtConfirmPassword;
    private final JProgressBar progressBar;
    private final ModernButton btnUpdate;
    private final ModernButton btnCancel;

    private final UserService userService = new UserService();

    public ChangePasswordDialog(JFrame parent) {
        super(parent, "Change Account Password", true);
        setSize(440, 320);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("Update Password");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Ensure your password has at least 8 characters with letters and numbers");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCurrentPassword = new JPasswordField();
        txtNewPassword = new JPasswordField();
        txtConfirmPassword = new JPasswordField();

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        formPanel.add(new JLabel("Current Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        formPanel.add(txtCurrentPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtNewPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Confirm New Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtConfirmPassword, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnUpdate = new ModernButton("Update Password", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnUpdate);
        add(buttonPanel, BorderLayout.SOUTH);

        btnUpdate.addActionListener(e -> handleUpdate());
        btnCancel.addActionListener(e -> dispose());
    }

    private void handleUpdate() {
        char[] current = txtCurrentPassword.getPassword();
        char[] next = txtNewPassword.getPassword();
        char[] confirm = txtConfirmPassword.getPassword();

        if (current.length == 0 || next.length == 0) {
            JOptionPane.showMessageDialog(this, "Please fill in all password fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Arrays.equals(next, confirm)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String currentStr = new String(current);
        String nextStr = new String(next);
        int userId = UserSession.getCurrentUser().getUserId();

        setWorking(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                userService.changePassword(userId, currentStr, nextStr);
                return null;
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                            "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                            "Failed to change password: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnUpdate.setEnabled(!working);
        btnCancel.setEnabled(!working);
        txtCurrentPassword.setEnabled(!working);
        txtNewPassword.setEnabled(!working);
        txtConfirmPassword.setEnabled(!working);
    }
}
