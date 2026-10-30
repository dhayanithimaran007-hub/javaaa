package com.smartcampus.ui.admin;

import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbRoleFilter;
    private final JComboBox<String> cmbActiveFilter;
    private final JTable tableUsers;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnToggleActive;
    private final ModernButton btnRefresh;

    private final UserService userService = new UserService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<User> currentUsers = new ArrayList<>();

    public UserManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        topBar.add(new JLabel("Search Users:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 30));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Role:"));
        cmbRoleFilter = new JComboBox<>(new String[]{"ALL", "STUDENT", "STAFF", "ADMIN"});
        topBar.add(cmbRoleFilter);

        topBar.add(new JLabel("Status:"));
        cmbActiveFilter = new JComboBox<>(new String[]{"ALL", "Active Only", "Inactive Only"});
        topBar.add(cmbActiveFilter);

        btnRefresh = new ModernButton("Filter Users", ModernButton.ButtonType.SECONDARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"User ID", "Full Name", "Email Address", "Role", "Department", "Contact Phone", "Status", "Registered On"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableUsers = new JTable(tableModel);
        tableUsers.setRowHeight(32);
        tableUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableUsers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tableUsers.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableUsers.getColumnModel().getColumn(1).setPreferredWidth(160);
        tableUsers.getColumnModel().getColumn(2).setPreferredWidth(180);
        tableUsers.getColumnModel().getColumn(3).setPreferredWidth(90);
        tableUsers.getColumnModel().getColumn(4).setPreferredWidth(140);
        tableUsers.getColumnModel().getColumn(5).setPreferredWidth(110);
        tableUsers.getColumnModel().getColumn(6).setPreferredWidth(80);
        tableUsers.getColumnModel().getColumn(7).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(tableUsers);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JPanel centerPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnToggleActive = new ModernButton("Activate / Deactivate Account", ModernButton.ButtonType.SECONDARY);
        bottomBar.add(btnToggleActive);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadUsers());
        txtSearch.addActionListener(e -> loadUsers());
        cmbRoleFilter.addActionListener(e -> loadUsers());
        cmbActiveFilter.addActionListener(e -> loadUsers());
        btnToggleActive.addActionListener(e -> handleToggleActive());

        loadUsers();
    }

    public void loadUsers() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String role = (String) cmbRoleFilter.getSelectedItem();
        String activeStr = (String) cmbActiveFilter.getSelectedItem();

        Boolean activeFilter = null;
        if ("Active Only".equalsIgnoreCase(activeStr)) activeFilter = true;
        else if ("Inactive Only".equalsIgnoreCase(activeStr)) activeFilter = false;

        final Boolean fActive = activeFilter;

        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.listUsers(role, fActive, search);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<User> list = get();
                    currentUsers.clear();
                    currentUsers.addAll(list);
                    tableModel.setRowCount(0);

                    for (User u : list) {
                        tableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getRole().getDisplayName(),
                                u.getDepartment() != null ? u.getDepartment() : "—",
                                u.getPhone() != null ? u.getPhone() : "—",
                                u.isActive() ? "Active" : "Inactive",
                                u.getCreatedAt() != null ? dateFormat.format(u.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "Failed to load user accounts: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleToggleActive() {
        int row = tableUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an account to toggle active state.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        User u = currentUsers.get(row);
        if (u.getUserId() == UserSession.getCurrentUser().getUserId()) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own current administrator account.",
                    "Operation Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean newActive = !u.isActive();
        String actionName = newActive ? "Activate" : "Deactivate";
        int confirm = JOptionPane.showConfirmDialog(this,
                actionName + " account for " + u.getFullName() + " (" + u.getEmail() + ")?",
                "Confirm " + actionName, JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userService.toggleUserActive(u.getUserId(), newActive);
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
