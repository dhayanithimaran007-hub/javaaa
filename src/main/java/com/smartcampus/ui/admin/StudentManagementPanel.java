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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class StudentManagementPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbDepartment;
    private final JComboBox<String> cmbStatus;

    private final JTable tableStudents;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnViewProfile;
    private final ModernButton btnToggleStatus;
    private final ModernButton btnRefresh;

    private final UserService userService = new UserService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<User> currentStudents = new ArrayList<>();

    public StudentManagementPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topCard = new CardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topCard.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        topCard.add(new JLabel("Search Students:"));
        txtSearch = new JTextField(14);
        txtSearch.setPreferredSize(new Dimension(160, 32));
        topCard.add(txtSearch);

        topCard.add(new JLabel("Department:"));
        cmbDepartment = new JComboBox<>(new String[]{
                "ALL", "Computer Science", "Mechanical Engineering", "Electrical Engineering",
                "Civil Engineering", "Biotechnology", "Business Administration", "Physics", "Chemistry"
        });
        topCard.add(cmbDepartment);

        topCard.add(new JLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"ALL", "Active Only", "Inactive Only"});
        topCard.add(cmbStatus);

        btnRefresh = new ModernButton("Filter Directory", ModernButton.ButtonType.SECONDARY);
        topCard.add(btnRefresh);

        add(topCard, BorderLayout.NORTH);

        String[] cols = {"Student ID", "Full Name", "Campus Email", "Department", "Contact Phone", "Account Status", "Registered On"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableStudents = new JTable(tableModel);
        tableStudents.setRowHeight(34);
        tableStudents.setFont(DesignSystem.FONT_SMALL);
        tableStudents.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableStudents.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableStudents.getColumnModel().getColumn(1).setPreferredWidth(170);
        tableStudents.getColumnModel().getColumn(2).setPreferredWidth(190);
        tableStudents.getColumnModel().getColumn(3).setPreferredWidth(160);
        tableStudents.getColumnModel().getColumn(4).setPreferredWidth(120);
        tableStudents.getColumnModel().getColumn(5).setPreferredWidth(90);
        tableStudents.getColumnModel().getColumn(6).setPreferredWidth(120);

        tableStudents.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableStudents.getSelectedRow() >= 0) {
                    openSelectedProfile();
                }
            }
        });

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableStudents), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnViewProfile = new ModernButton("View Student Profile & Tickets", ModernButton.ButtonType.PRIMARY);
        btnToggleStatus = new ModernButton("Activate / Deactivate Account", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnToggleStatus);
        bottomBar.add(btnViewProfile);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadStudents());
        txtSearch.addActionListener(e -> loadStudents());
        cmbDepartment.addActionListener(e -> loadStudents());
        cmbStatus.addActionListener(e -> loadStudents());

        btnViewProfile.addActionListener(e -> openSelectedProfile());
        btnToggleStatus.addActionListener(e -> handleToggleStatus());

        loadStudents();
    }

    public void loadStudents() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String statusStr = (String) cmbStatus.getSelectedItem();
        String dept = (String) cmbDepartment.getSelectedItem();

        Boolean activeFilter = null;
        if ("Active Only".equalsIgnoreCase(statusStr)) activeFilter = true;
        else if ("Inactive Only".equalsIgnoreCase(statusStr)) activeFilter = false;

        final Boolean fActive = activeFilter;
        final String fSearch = search;
        final String fDept = dept;

        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.listUsers("STUDENT", fActive, fSearch);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<User> list = get();
                    currentStudents.clear();
                    tableModel.setRowCount(0);

                    for (User u : list) {
                        String uDept = u.getDepartment() != null && !u.getDepartment().isBlank() ? u.getDepartment() : "General";
                        if (fDept != null && !"ALL".equalsIgnoreCase(fDept) && !uDept.toLowerCase().contains(fDept.toLowerCase())) {
                            continue;
                        }
                        currentStudents.add(u);
                        tableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                uDept,
                                u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "-",
                                u.isActive() ? "ACTIVE" : "INACTIVE",
                                u.getCreatedAt() != null ? dateFormat.format(u.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentManagementPanel.this,
                            "Failed to load students: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSelectedProfile() {
        int row = tableStudents.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student from the directory.", "Select Student", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int studentId = (Integer) tableModel.getValueAt(row, 0);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new StudentProfileDialog(parent, studentId).setVisible(true);
    }

    private void handleToggleStatus() {
        int row = tableStudents.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student account to toggle.", "Select Student", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        User selected = currentStudents.get(row);
        boolean targetStatus = !selected.isActive();
        String actionName = targetStatus ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + actionName + " student account: " + selected.getFullName() + "?",
                "Confirm Account Status Change", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            btnToggleStatus.setEnabled(false);
            progressBar.setVisible(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    userService.toggleUserActive(selected.getUserId(), targetStatus);
                    return null;
                }

                @Override
                protected void done() {
                    btnToggleStatus.setEnabled(true);
                    progressBar.setVisible(false);
                    try {
                        get();
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(StudentManagementPanel.this);
                        ToastNotification.showSuccess(parent, "Student account " + actionName + "d successfully.");
                        loadStudents();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StudentManagementPanel.this,
                                "Failed to update student account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
