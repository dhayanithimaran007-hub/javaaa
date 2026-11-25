package com.smartcampus.ui.admin;

import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topBar = new CardPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new javax.swing.BoxLayout(titlePanel, javax.swing.BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Maintenance Staff & Dispatch Administration");
        title.setFont(DesignSystem.FONT_TITLE);
        title.setForeground(DesignSystem.TEXT_PRIMARY);

        JLabel sub = new JLabel("Monitor active technician workloads, trade specializations, and account authorizations");
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(javax.swing.Box.createVerticalStrut(2));
        titlePanel.add(sub);

        btnCreateStaff = new ModernButton("+ Register Maintenance Staff", ModernButton.ButtonType.PRIMARY);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(btnCreateStaff, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        String[] cols = {"User ID", "Staff Name", "Campus Email", "Trade Specialization", "Contact Phone", "Active Tickets", "Workload Status", "Account Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableStaff = new JTable(tableModel);
        tableStaff.setRowHeight(34);
        tableStaff.setFont(DesignSystem.FONT_SMALL);
        tableStaff.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableStaff.getColumnModel().getColumn(0).setPreferredWidth(55);
        tableStaff.getColumnModel().getColumn(1).setPreferredWidth(170);
        tableStaff.getColumnModel().getColumn(2).setPreferredWidth(180);
        tableStaff.getColumnModel().getColumn(3).setPreferredWidth(170);
        tableStaff.getColumnModel().getColumn(4).setPreferredWidth(110);
        tableStaff.getColumnModel().getColumn(5).setPreferredWidth(95);
        tableStaff.getColumnModel().getColumn(6).setPreferredWidth(120);
        tableStaff.getColumnModel().getColumn(7).setPreferredWidth(100);

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableStaff), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

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

        new SwingWorker<StaffData, Void>() {
            @Override
            protected StaffData doInBackground() throws Exception {
                List<User> list = userService.listUsers("STAFF", null, null);
                Map<Integer, Integer> workloads = userService.getStaffWorkloadMap();
                return new StaffData(list, workloads);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    StaffData data = get();
                    currentStaffList.clear();
                    currentStaffList.addAll(data.staff);
                    tableModel.setRowCount(0);

                    for (User u : data.staff) {
                        int activeTasks = data.workloadMap.getOrDefault(u.getUserId(), 0);
                        String statusTag;
                        if (activeTasks == 0) statusTag = "Available (0)";
                        else if (activeTasks <= 2) statusTag = "Optimal (" + activeTasks + ")";
                        else statusTag = "Heavy (" + activeTasks + ")";

                        tableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getDepartment() != null ? u.getDepartment() : "General Facilities",
                                u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "-",
                                activeTasks + " active",
                                statusTag,
                                u.isActive() ? "ACTIVE" : "INACTIVE"
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
        JDialog dlg = new JDialog(parent, "Register New Maintenance Staff Account", true);
        dlg.setSize(480, 460);
        dlg.setLocationRelativeTo(parent);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DesignSystem.BG_CANVAS);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JComboBox<String> cmbDept = new JComboBox<>(new String[]{
                "Electrical Maintenance",
                "Plumbing & Sanitation",
                "HVAC & Climate Control",
                "Network & IT Support",
                "Classroom Equipment & AV",
                "Civil & Structural Maintenance",
                "General Facilities"
        });
        JTextField txtPhone = new JTextField();

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        form.add(new JLabel("Full Name *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Campus Email *:"), gbc);
        gbc.gridx = 1;
        form.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Password *:"), gbc);
        gbc.gridx = 1;
        form.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Trade Specialization *:"), gbc);
        gbc.gridx = 1;
        form.add(cmbDept, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("Contact Phone:"), gbc);
        gbc.gridx = 1;
        form.add(txtPhone, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnBar.setBackground(Color.WHITE);
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        ModernButton btnSave = new ModernButton("Create Staff Account", ModernButton.ButtonType.PRIMARY);
        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dlg.add(btnBar, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            String dept = (String) cmbDept.getSelectedItem();
            String phone = txtPhone.getText().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Name, email, and password are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnSave.setEnabled(false);
            btnCancel.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    userService.createStaffAccount(name, email, pass, dept, phone);
                    return null;
                }

                @Override
                protected void done() {
                    btnSave.setEnabled(true);
                    btnCancel.setEnabled(true);
                    try {
                        get();
                        ToastNotification.showSuccess(parent, "Staff account for " + name + " created successfully.");
                        dlg.dispose();
                        loadStaffList();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dlg, "Failed to create staff account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        dlg.setVisible(true);
    }

    private void handleToggleActive() {
        int row = tableStaff.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a staff member from the directory.", "Select Staff", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        User selected = currentStaffList.get(row);
        boolean targetStatus = !selected.isActive();
        String actionName = targetStatus ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + actionName + " staff account: " + selected.getFullName() + "?",
                "Confirm Account Status Change", JOptionPane.YES_NO_OPTION);

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
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(StaffManagementPanel.this);
                        ToastNotification.showSuccess(parent, "Staff account " + actionName + "d successfully.");
                        loadStaffList();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StaffManagementPanel.this,
                                "Failed to update staff account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private record StaffData(List<User> staff, Map<Integer, Integer> workloadMap) {}
}
