package com.smartcampus.ui.staff;

import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
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

public class WorkProgressDialog extends JDialog {
    private final int complaintId;
    private final JComboBox<String> cmbTargetStatus;
    private final JTextArea txtWorkNote;
    private final JProgressBar progressBar;
    private final ModernButton btnUpdate;
    private final ModernButton btnCancel;

    private final ComplaintService complaintService = new ComplaintService();
    private boolean progressUpdated = false;

    public WorkProgressDialog(JFrame parent, int complaintId, String complaintTitle, ComplaintStatus currentStatus) {
        super(parent, "Update Work Progress - Complaint #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(480, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("Log Maintenance Progress");
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

        cmbTargetStatus = new JComboBox<>(new String[]{"IN_PROGRESS", "ACKNOWLEDGED"});
        if (currentStatus == ComplaintStatus.ACKNOWLEDGED) {
            cmbTargetStatus.setSelectedItem("IN_PROGRESS");
        }

        txtWorkNote = new JTextArea(4, 25);
        txtWorkNote.setLineWrap(true);
        txtWorkNote.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtWorkNote);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        formPanel.add(new JLabel("Update Status To:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        formPanel.add(cmbTargetStatus, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Work Notes / Progress *:"), gbc);
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
        btnUpdate = new ModernButton("Save Progress", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnUpdate);
        add(buttonPanel, BorderLayout.SOUTH);

        btnUpdate.addActionListener(e -> handleUpdate());
        btnCancel.addActionListener(e -> dispose());
    }

    private void handleUpdate() {
        String note = txtWorkNote.getText().trim();
        if (note.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please describe the work progress.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String targetStr = (String) cmbTargetStatus.getSelectedItem();
        ComplaintStatus status = ComplaintStatus.fromString(targetStr);

        progressBar.setVisible(true);
        btnUpdate.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.updateProgress(complaintId, status, note);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnUpdate.setEnabled(true);
                try {
                    get();
                    progressUpdated = true;
                    JOptionPane.showMessageDialog(WorkProgressDialog.this,
                            "Work progress recorded successfully for Complaint #" + complaintId,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(WorkProgressDialog.this,
                            "Failed to update progress: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isProgressUpdated() {
        return progressUpdated;
    }
}
