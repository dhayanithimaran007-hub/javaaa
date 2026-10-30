package com.smartcampus.ui.admin;

import com.smartcampus.model.User;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class AssignStaffDialog extends JDialog {
    private final int complaintId;
    private final JComboBox<UserItem> cmbStaff;
    private final JTextArea txtNotes;
    private final JProgressBar progressBar;
    private final ModernButton btnAssign;
    private final ModernButton btnCancel;

    private final ComplaintService complaintService = new ComplaintService();
    private final UserService userService = new UserService();
    private boolean assignedSuccessfully = false;

    public record UserItem(int id, String name, String dept) {
        @Override
        public String toString() {
            return name + " (" + dept + ")";
        }
    }

    public AssignStaffDialog(JFrame parent, int complaintId, String complaintTitle) {
        super(parent, "Assign Staff - Complaint #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(480, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("Assign Maintenance Staff");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Complaint: " + (complaintTitle.length() > 40 ? complaintTitle.substring(0, 40) + "..." : complaintTitle));
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbStaff = new JComboBox<>();
        txtNotes = new JTextArea(4, 25);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtNotes);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Select Staff *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(cmbStaff, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Dispatch Notes:"), gbc);
        gbc.gridx = 1;
        formPanel.add(scroll, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnAssign = new ModernButton("Confirm Assignment", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnAssign);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAssign.addActionListener(e -> handleAssign());
        btnCancel.addActionListener(e -> dispose());

        loadActiveStaff();
    }

    private void loadActiveStaff() {
        progressBar.setVisible(true);
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.getActiveStaffList();
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    List<User> list = get();
                    DefaultComboBoxModel<UserItem> model = new DefaultComboBoxModel<>();
                    for (User u : list) {
                        model.addElement(new UserItem(u.getUserId(), u.getFullName(), u.getDepartment()));
                    }
                    cmbStaff.setModel(model);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignStaffDialog.this,
                            "Failed to load staff list: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleAssign() {
        UserItem selected = (UserItem) cmbStaff.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a staff member to assign.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String notes = txtNotes.getText().trim();
        progressBar.setVisible(true);
        btnAssign.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.assignStaff(complaintId, selected.id(), notes);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnAssign.setEnabled(true);
                try {
                    get();
                    assignedSuccessfully = true;
                    JOptionPane.showMessageDialog(AssignStaffDialog.this,
                            "Complaint #" + complaintId + " successfully assigned to " + selected.name() + "!",
                            "Assignment Completed", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(AssignStaffDialog.this,
                            "Assignment failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isAssignedSuccessfully() {
        return assignedSuccessfully;
    }
}
