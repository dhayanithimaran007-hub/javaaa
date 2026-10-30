package com.smartcampus.ui.admin;

import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class StaffManagementPanel extends JPanel {
    private final JTable tableStaff;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnCreateStaff;
    private final ModernButton btnToggleActive;
    private final ModernButton btnRefresh;

    private final UserService userService = new UserService();
    private final List<User> currentStaffList = new ArrayList<>();

    public StaffManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Header
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel title = new JLabel("Maintenance Staff Administration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(15, 23, 42));

        btnCreateStaff = new ModernButton("+ Add Maintenance Staff", ModernButton.ButtonType.PRIMARY);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnCreateStaff, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"User ID", "Staff Name", "Email Address", "Department / Trade", "Contact Phone", "Account Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableStaff = new JTable(tableModel);
        tableStaff.setRowHeight(32);
        tableStaff.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableStaff.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tableStaff.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableStaff.getColumnModel().getColumn(1).setPreferredWidth(180);
        tableStaff.getColumnModel().getColumn(2).setPreferredWidth(180);
        tableStaff.getColumnModel().getColumn(3).setPreferredWidth(160);
        tableStaff.getColumnModel().getColumn(4).setPreferredWidth(120);
        tableStaff.getColumnModel().getColumn(5).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(tableStaff);
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

        btnToggleActive = new ModernButton("Activate / Deactivate Staff", ModernButton.ButtonType.SECONDARY);
        btnRefresh = new ModernButton("Refresh List", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnToggleActive);
        bottomBar.add(btnRefresh);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadStaffList());
        btnCreateStaff.addActionListener(e -> openCreateStaffDialog());
        btnToggleActive.addActionListener(e -> handleToggleActive());

        loadStaffList();
    }

    public void loadStaffList() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.listUsers("STAFF", null, null);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<User> list = get();
                    currentStaffList.clear();
                    currentStaffList.addAll(list);
                    tableModel.setRowCount(0);

                    for (User u : list) {
                        tableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getDepartment() != null ? u.getDepartment() : "General",
                                u.getPhone() != null ? u.getPhone() : "",
                                u.isActive() ? "Active" : "Deactivated"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffManagementPanel.this,
                            "Failed to load staff list: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openCreateStaffDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(parent, "Create Maintenance Staff Account", true);
        dlg.setSize(440, 420);
        dlg.setLocationRelativeTo(parent);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtDept = new JTextField("Electrical Maintenance");
        JTextField txtPhone = new JTextField();

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        form.add(new JLabel("Staff Full Name *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Staff Email *:"), gbc);
        gbc.gridx = 1;
        form.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Initial Password *:"), gbc);
        gbc.gridx = 1;
        form.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Trade / Department:"), gbc);
        gbc.gridx = 1;
        form.add(txtDept, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("Contact Phone:"), gbc);
        gbc.gridx = 1;
        form.add(txtPhone, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        ModernButton btnSave = new ModernButton("Create Staff", ModernButton.ButtonType.PRIMARY);
        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword());
            String dept = txtDept.getText().trim();
            String phone = txtPhone.getText().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Name, Email, and Password are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                userService.createStaffAccount(name, email, pass, dept, phone);
                JOptionPane.showMessageDialog(dlg, "Staff account created successfully for " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
                loadStaffList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Failed to create staff account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dlg.setVisible(true);
    }

    private void handleToggleActive() {
        int row = tableStaff.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a staff member from the table.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        User staff = currentStaffList.get(row);
        boolean newActive = !staff.isActive();
        String prompt = newActive
                ? "Activate account for " + staff.getFullName() + "?"
                : "Deactivate account for " + staff.getFullName() + "? They will not be able to log in.";

        int confirm = JOptionPane.showConfirmDialog(this, prompt, "Confirm Status Change", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userService.toggleUserActive(staff.getUserId(), newActive);
                loadStaffList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update staff status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
