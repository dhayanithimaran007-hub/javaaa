package com.smartcampus.ui.components;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintDetailsDTO;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.student.ComplaintFeedbackDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
import java.text.SimpleDateFormat;
import java.util.List;

public class ComplaintDetailsDialog extends JDialog {
    private final int complaintId;
    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JPanel detailsContainer;
    private final JProgressBar progressBar;
    private final ModernButton btnFeedback;
    private final ModernButton btnClose;

    private ComplaintDetailsDTO currentData;

    public ComplaintDetailsDialog(JFrame parent, int complaintId) {
        super(parent, "Complaint Details & Lifecycle Tracking - #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(780, 640);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Complaint Overview & Audit History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Complaint Tracking ID: #" + complaintId);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Content Area
        detailsContainer = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        detailsContainer.add(progressBar, BorderLayout.NORTH);
        add(detailsContainer, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnFeedback = new ModernButton("Submit Feedback", ModernButton.ButtonType.PRIMARY);
        btnFeedback.setVisible(false);
        btnClose = new ModernButton("Close", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnFeedback);
        buttonPanel.add(btnClose);
        add(buttonPanel, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> dispose());
        btnFeedback.addActionListener(e -> {
            if (currentData != null) {
                ComplaintFeedbackDialog feedbackDlg = new ComplaintFeedbackDialog(
                        (JFrame) getParent(), complaintId, currentData.complaint().getTitle());
                feedbackDlg.setVisible(true);
                if (feedbackDlg.isFeedbackSubmitted()) {
                    loadData();
                }
            }
        });

        loadData();
    }

    private void loadData() {
        progressBar.setVisible(true);

        new SwingWorker<ComplaintDetailsDTO, Void>() {
            @Override
            protected ComplaintDetailsDTO doInBackground() throws Exception {
                return complaintService.getComplaintDetails(complaintId);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    currentData = get();
                    renderUI(currentData);
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(ComplaintDetailsDialog.this,
                            "Failed to load complaint details: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderUI(ComplaintDetailsDTO data) {
        detailsContainer.removeAll();
        Complaint c = data.complaint();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // TAB 1: General Details & Location
        JPanel pnlGeneral = new JPanel(new GridBagLayout());
        pnlGeneral.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Attributes grid
        addDetailRow(pnlGeneral, gbc, 0, "Complaint ID:", "#" + c.getComplaintId());
        addDetailRow(pnlGeneral, gbc, 1, "Title:", c.getTitle());
        addDetailRow(pnlGeneral, gbc, 2, "Category:", c.getCategory());
        addDetailRow(pnlGeneral, gbc, 3, "Location:", c.getBuilding() + " (Room/Area: " + c.getRoomNumber() + ")");
        addDetailRow(pnlGeneral, gbc, 4, "Current Status:", c.getStatus().getDisplayName());
        addDetailRow(pnlGeneral, gbc, 5, "Priority Level:", c.getPriority().name() + " (Deterministic Score: " + c.getPriorityScore() + ")");
        addDetailRow(pnlGeneral, gbc, 6, "Safety Critical Override:", c.isSafetyCritical() ? "YES - Immediate Human Review Flagged" : "No");

        String scoringFactors = String.format("Severity: %d/5 | Safety Risk: %d/5 | Affected: %d people | Operational Impact: %d/5",
                c.getSeverity(), c.getSafetyRisk(), c.getPeopleAffected(), c.getOperationalImpact());
        addDetailRow(pnlGeneral, gbc, 7, "Scoring Factors:", scoringFactors);

        addDetailRow(pnlGeneral, gbc, 8, "Submitted By:", (c.getStudentName() != null ? c.getStudentName() : "Student ID #" + c.getStudentId())
                + (c.getStudentEmail() != null ? " (" + c.getStudentEmail() + ")" : ""));
        addDetailRow(pnlGeneral, gbc, 9, "Submitted At:", c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : "N/A");

        if (c.getAssignedStaffName() != null) {
            addDetailRow(pnlGeneral, gbc, 10, "Assigned Staff:", c.getAssignedStaffName());
        }
        if (c.getResolvedAt() != null) {
            addDetailRow(pnlGeneral, gbc, 11, "Resolved At:", dateFormat.format(c.getResolvedAt()));
        }

        // Description box
        gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 0.25;
        pnlGeneral.add(new JLabel("Full Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        JTextArea txtDesc = new JTextArea(c.getDescription(), 4, 30);
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBackground(new Color(248, 250, 252));
        pnlGeneral.add(new JScrollPane(txtDesc), gbc);

        tabs.addTab("Complaint Details", new JScrollPane(pnlGeneral));

        // TAB 2: Chronological Tracking Timeline
        JPanel pnlTimeline = new JPanel(new BorderLayout(10, 10));
        pnlTimeline.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"Time", "Changed By", "Role", "Old Status", "New Status", "Remarks / Work Note"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        for (ComplaintStatusHistory h : data.historyList()) {
            model.addRow(new Object[]{
                    h.getChangedAt() != null ? dateFormat.format(h.getChangedAt()) : "",
                    h.getChangedByName() != null ? h.getChangedByName() : "User #" + h.getChangedBy(),
                    h.getChangedByRole() != null ? h.getChangedByRole() : "",
                    h.getOldStatus() != null ? h.getOldStatus().getDisplayName() : "-",
                    h.getNewStatus() != null ? h.getNewStatus().getDisplayName() : "",
                    h.getRemarks() != null ? h.getRemarks() : ""
            });
        }

        JTable tableHistory = new JTable(model);
        tableHistory.setRowHeight(26);
        tableHistory.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableHistory.getColumnModel().getColumn(0).setPreferredWidth(130);
        tableHistory.getColumnModel().getColumn(1).setPreferredWidth(120);
        tableHistory.getColumnModel().getColumn(5).setPreferredWidth(260);

        pnlTimeline.add(new JScrollPane(tableHistory), BorderLayout.CENTER);
        tabs.addTab("Status History & Audit Trail (" + data.historyList().size() + ")", pnlTimeline);

        // TAB 3: Student Feedback (if available)
        if (data.feedback().isPresent()) {
            var fb = data.feedback().get();
            JPanel pnlFeedback = new JPanel(new GridBagLayout());
            pnlFeedback.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
            GridBagConstraints fgbc = new GridBagConstraints();
            fgbc.insets = new Insets(6, 6, 6, 6);
            fgbc.fill = GridBagConstraints.HORIZONTAL;

            addDetailRow(pnlFeedback, fgbc, 0, "Rating:", fb.getRating() + " / 5 Stars");
            addDetailRow(pnlFeedback, fgbc, 1, "Submitted By:", fb.getStudentName() != null ? fb.getStudentName() : "Student");
            addDetailRow(pnlFeedback, fgbc, 2, "Feedback Date:", fb.getCreatedAt() != null ? dateFormat.format(fb.getCreatedAt()) : "");

            fgbc.gridx = 0; fgbc.gridy = 3; fgbc.weightx = 0.3;
            pnlFeedback.add(new JLabel("Comments:"), fgbc);
            fgbc.gridx = 1; fgbc.weightx = 0.7;
            JTextArea fbComments = new JTextArea(fb.getComments() != null ? fb.getComments() : "No comments provided.", 4, 30);
            fbComments.setEditable(false);
            fbComments.setBackground(new Color(248, 250, 252));
            pnlFeedback.add(new JScrollPane(fbComments), fgbc);

            tabs.addTab("Resolution Feedback", pnlFeedback);
        }

        detailsContainer.add(tabs, BorderLayout.CENTER);

        // Show Feedback button if user is Student, complaint is RESOLVED, and feedback not yet submitted
        if (UserSession.hasRole(Role.STUDENT) &&
                c.getStatus() == ComplaintStatus.RESOLVED &&
                data.feedback().isEmpty()) {
            btnFeedback.setVisible(true);
        } else {
            btnFeedback.setVisible(false);
        }

        detailsContainer.revalidate();
        detailsContainer.repaint();
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(new Color(15, 23, 42));
        panel.add(val, gbc);
    }
}
