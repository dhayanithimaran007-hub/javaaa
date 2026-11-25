package com.smartcampus.ui.admin;

import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topBar = new CardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        topBar.add(new JLabel("Search Users:"));
        txtSearch = new JTextField(14);
        txtSearch.setPreferredSize(new Dimension(160, 32));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Role:"));
        cmbRoleFilter = new JComboBox<>(new String[]{"ALL", "STUDENT", "STAFF", "ADMIN"});
        topBar.add(cmbRoleFilter);

        topBar.add(new JLabel("Status:"));
        cmbActiveFilter = new JComboBox<>(new String[]{"ALL", "Active Only", "Inactive Only"});
        topBar.add(cmbActiveFilter);

        btnRefresh = new ModernButton("Filter Directory", ModernButton.ButtonType.PRIMARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        String[] cols = {"User ID", "Full Name", "Campus Email", "Role", "Department", "Contact Phone", "Account Status", "Registered On"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableUsers = new JTable(tableModel);
        tableUsers.setRowHeight(34);
        tableUsers.setFont(DesignSystem.FONT_SMALL);
        tableUsers.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableUsers.getColumnModel().getColumn(0).setPreferredWidth(55);
        tableUsers.getColumnModel().getColumn(1).setPreferredWidth(170);
        tableUsers.getColumnModel().getColumn(2).setPreferredWidth(190);
        tableUsers.getColumnModel().getColumn(3).setPreferredWidth(90);
        tableUsers.getColumnModel().getColumn(4).setPreferredWidth(150);
        tableUsers.getColumnModel().getColumn(5).setPreferredWidth(110);
        tableUsers.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableUsers.getColumnModel().getColumn(7).setPreferredWidth(120);

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableUsers), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

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
                                u.getRole().name(),
                                u.getDepartment() != null ? u.getDepartment() : "General",
                                u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "-",
                                u.isActive() ? "ACTIVE" : "INACTIVE",
                                u.getCreatedAt() != null ? dateFormat.format(u.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "Failed to load user directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleToggleActive() {
        int row = tableUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an account to toggle.", "Select Account", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        User selected = currentUsers.get(row);
        boolean targetStatus = !selected.isActive();
        String actionName = targetStatus ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + actionName + " account: " + selected.getEmail() + " (" + selected.getFullName() + ")?",
                "Confirm Account Authorization Change", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            btnToggleActive.setEnabled(false);
            progressBar.setVisible(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    userService.toggleUserActive(selected.getUserId(), targetStatus);
                    return null;
                }

                @Override
                protected void done() {
                    btnToggleActive.setEnabled(true);
                    progressBar.setVisible(false);
                    try {
                        get();
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(UserManagementPanel.this);
                        ToastNotification.showSuccess(parent, "Account " + selected.getEmail() + " " + actionName + "d.");
                        loadUsers();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UserManagementPanel.this,
                                "Failed to toggle account status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
