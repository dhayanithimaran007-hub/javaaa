package com.smartcampus.ui.student;

import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.ChangePasswordDialog;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class StudentProfilePanel extends JPanel {
    private final JTextField txtName;
    private final JTextField txtEmail;
    private final JTextField txtDepartment;
    private final JTextField txtPhone;
    private final JProgressBar progressBar;
    private final ModernButton btnSave;
    private final ModernButton btnChangePass;

    private final UserService userService = new UserService();

    public StudentProfilePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        // Header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        JLabel title = new JLabel("My Student Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(15, 23, 42));

        JLabel subtitle = new JLabel("View and manage your campus contact details and password");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 116, 139));

        cardHeader.add(title, BorderLayout.NORTH);
        cardHeader.add(subtitle, BorderLayout.SOUTH);
        card.add(cardHeader, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        User user = UserSession.getCurrentUser();

        txtName = new JTextField(user != null ? user.getFullName() : "");
        txtEmail = new JTextField(user != null ? user.getEmail() : "");
        txtEmail.setEditable(false);
        txtEmail.setBackground(new Color(241, 245, 249));

        txtDepartment = new JTextField(user != null && user.getDepartment() != null ? user.getDepartment() : "");
        txtPhone = new JTextField(user != null && user.getPhone() != null ? user.getPhone() : "");

        txtName.setPreferredSize(new Dimension(280, 32));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        form.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Email Address (ID):"), gbc);
        gbc.gridx = 1;
        form.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Department / Major:"), gbc);
        gbc.gridx = 1;
        form.add(txtDepartment, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        form.add(txtPhone, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(progressBar, gbc);

        card.add(form, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setOpaque(false);
        btnChangePass = new ModernButton("Change Password", ModernButton.ButtonType.SECONDARY);
        btnSave = new ModernButton("Save Profile Changes", ModernButton.ButtonType.PRIMARY);

        actions.add(btnChangePass);
        actions.add(btnSave);
        card.add(actions, BorderLayout.SOUTH);

        add(card, BorderLayout.NORTH);

        btnSave.addActionListener(e -> handleSave());
        btnChangePass.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ChangePasswordDialog(frame).setVisible(true);
        });
    }

    private void handleSave() {
        String name = txtName.getText().trim();
        String dept = txtDepartment.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = UserSession.getCurrentUser();
        progressBar.setVisible(true);
        btnSave.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                userService.updateProfile(user.getUserId(), name, dept, phone);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnSave.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(StudentProfilePanel.this,
                            "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentProfilePanel.this,
                            "Failed to update profile: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
