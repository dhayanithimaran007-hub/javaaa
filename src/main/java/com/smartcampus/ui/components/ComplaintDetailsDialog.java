package com.smartcampus.ui.components;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintDetailsDTO;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.SlaSnapshot;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.EmergencyEscalationService;
import com.smartcampus.service.PriorityService;
import com.smartcampus.ui.student.ComplaintFeedbackDialog;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
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
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;

public class ComplaintDetailsDialog extends JDialog {
    private final int complaintId;
    private final ComplaintService complaintService = new ComplaintService();
    private final PriorityService priorityService = new PriorityService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JPanel detailsContainer;
    private final JProgressBar progressBar;
    private final ModernButton btnFeedback;
    private final ModernButton btnClose;

    private ComplaintDetailsDTO currentData;

    public ComplaintDetailsDialog(JFrame parent, int complaintId) {
        super(parent, "Complaint Intelligence Center - #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(850, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DesignSystem.MIDNIGHT_NAVY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel titleLabel = new JLabel("Complaint Overview & Prioritization Audit");
        titleLabel.setFont(DesignSystem.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Complaint Reference ID: #" + complaintId + " | Multi-Factor Classification Engine");
        subLabel.setFont(DesignSystem.FONT_SMALL);
        subLabel.setForeground(DesignSystem.TEXT_MUTED);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        detailsContainer = new JPanel(new BorderLayout());
        detailsContainer.setBackground(DesignSystem.BG_CANVAS);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        detailsContainer.add(progressBar, BorderLayout.NORTH);
        add(detailsContainer, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        btnFeedback = new ModernButton("Submit Feedback", ModernButton.ButtonType.PRIMARY);
        btnFeedback.setVisible(false);
        btnClose = new ModernButton("Close Details", ModernButton.ButtonType.SECONDARY);

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

        JPanel stepperPanel = buildStatusStepper(c.getStatus());
        detailsContainer.add(stepperPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(DesignSystem.FONT_BODY_BOLD);

        JPanel pnlGeneral = new JPanel(new GridBagLayout());
        pnlGeneral.setBackground(Color.WHITE);
        pnlGeneral.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addDetailRow(pnlGeneral, gbc, 0, "Complaint ID:", "#" + c.getComplaintId());
        addDetailRow(pnlGeneral, gbc, 1, "Title:", c.getTitle());
        addDetailRow(pnlGeneral, gbc, 2, "Category:", c.getCategory());
        addDetailRow(pnlGeneral, gbc, 3, "Building & Room:", c.getBuilding() + " (Room " + c.getRoomNumber() + ")");
        addDetailRow(pnlGeneral, gbc, 4, "Current Status:", c.getStatus().getDisplayName());
        addDetailRow(pnlGeneral, gbc, 5, "Priority Classification:", c.getPriority().name() + " (Score: " + c.getPriorityScore() + " / 100)");
        addDetailRow(pnlGeneral, gbc, 6, "Safety Critical Override:", c.isSafetyCritical() ? "YES - Immediate Life Safety Hazard Flagged" : "No (Standard Facilities Flow)");

        String scoringFactors = String.format("Severity: %d/5 | Safety Risk: %d/5 | Affected: %d people | Operational Impact: %d/5",
                c.getSeverity(), c.getSafetyRisk(), c.getPeopleAffected(), c.getOperationalImpact());
        addDetailRow(pnlGeneral, gbc, 7, "Raw Input Factors:", scoringFactors);

        addDetailRow(pnlGeneral, gbc, 8, "Submitted By:", (c.getStudentName() != null ? c.getStudentName() : "Student ID #" + c.getStudentId())
                + (c.getStudentEmail() != null ? " (" + c.getStudentEmail() + ")" : ""));
        addDetailRow(pnlGeneral, gbc, 9, "Submitted On:", c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : "N/A");

        if (c.getAssignedStaffName() != null) {
            addDetailRow(pnlGeneral, gbc, 10, "Assigned Technician:", c.getAssignedStaffName());
        }
        if (c.getResolvedAt() != null) {
            addDetailRow(pnlGeneral, gbc, 11, "Resolved Timestamp:", dateFormat.format(c.getResolvedAt()));
        }

        gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 0.28;
        JLabel lblDesc = new JLabel("Full Description:");
        lblDesc.setFont(DesignSystem.FONT_SMALL_BOLD);
        pnlGeneral.add(lblDesc, gbc);
        gbc.gridx = 1; gbc.weightx = 0.72;
        JTextArea txtDesc = new JTextArea(c.getDescription(), 4, 30);
        txtDesc.setFont(DesignSystem.FONT_SMALL);
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBackground(new Color(248, 250, 252));
        pnlGeneral.add(new JScrollPane(txtDesc), gbc);

        tabs.addTab("Complaint Information", new JScrollPane(pnlGeneral));

        JPanel pnlExplain = buildPrioritizationExplainPanel(c);
        tabs.addTab("Prioritization Engine Breakdown", pnlExplain);

        JPanel pnlTimeline = new JPanel(new BorderLayout(10, 10));
        pnlTimeline.setBackground(Color.WHITE);
        pnlTimeline.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        String[] cols = {"Time", "Changed By", "Role", "Old Status", "New Status", "Audit Remarks / Dispatch Notes"};
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
        tableHistory.setRowHeight(30);
        tableHistory.setFont(DesignSystem.FONT_SMALL);
        tableHistory.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableHistory.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableHistory.getColumnModel().getColumn(0).setPreferredWidth(130);
        tableHistory.getColumnModel().getColumn(1).setPreferredWidth(130);
        tableHistory.getColumnModel().getColumn(5).setPreferredWidth(280);

        pnlTimeline.add(new JScrollPane(tableHistory), BorderLayout.CENTER);
        tabs.addTab("Lifecycle History (" + data.historyList().size() + ")", pnlTimeline);

        if (data.feedback().isPresent()) {
            var fb = data.feedback().get();
            JPanel pnlFeedback = new JPanel(new GridBagLayout());
            pnlFeedback.setBackground(Color.WHITE);
            pnlFeedback.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
            GridBagConstraints fgbc = new GridBagConstraints();
            fgbc.insets = new Insets(6, 6, 6, 6);
            fgbc.fill = GridBagConstraints.HORIZONTAL;

            addDetailRow(pnlFeedback, fgbc, 0, "Rating:", fb.getRating() + " / 5 Stars");
            addDetailRow(pnlFeedback, fgbc, 1, "Submitted By:", fb.getStudentName() != null ? fb.getStudentName() : "Student");
            addDetailRow(pnlFeedback, fgbc, 2, "Feedback Date:", fb.getCreatedAt() != null ? dateFormat.format(fb.getCreatedAt()) : "");

            fgbc.gridx = 0; fgbc.gridy = 3; fgbc.weightx = 0.3;
            JLabel lblComments = new JLabel("Student Remarks:");
            lblComments.setFont(DesignSystem.FONT_SMALL_BOLD);
            pnlFeedback.add(lblComments, fgbc);

            fgbc.gridx = 1; fgbc.weightx = 0.7;
            JTextArea fbComments = new JTextArea(fb.getComments() != null ? fb.getComments() : "No comments provided.", 4, 30);
            fbComments.setFont(DesignSystem.FONT_SMALL);
            fbComments.setEditable(false);
            fbComments.setBackground(new Color(248, 250, 252));
            pnlFeedback.add(new JScrollPane(fbComments), fgbc);

            tabs.addTab("Student Feedback", pnlFeedback);
        }

        detailsContainer.add(tabs, BorderLayout.CENTER);

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

    private JPanel buildStatusStepper(ComplaintStatus currentStatus) {
        JPanel stepper = new JPanel(new GridLayout(1, 4, 8, 0));
        stepper.setBackground(DesignSystem.BG_CANVAS);
        stepper.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        ComplaintStatus[] steps = {
                ComplaintStatus.SUBMITTED,
                ComplaintStatus.ACKNOWLEDGED,
                ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.RESOLVED
        };

        int activeStepIdx = 0;
        if (currentStatus == ComplaintStatus.ACKNOWLEDGED) activeStepIdx = 1;
        else if (currentStatus == ComplaintStatus.IN_PROGRESS) activeStepIdx = 2;
        else if (currentStatus == ComplaintStatus.RESOLVED) activeStepIdx = 3;
        else if (currentStatus == ComplaintStatus.REJECTED) activeStepIdx = -1;

        for (int i = 0; i < steps.length; i++) {
            ComplaintStatus s = steps[i];
            boolean isCompleted = (activeStepIdx >= i);
            boolean isCurrent = (activeStepIdx == i);

            CardPanel stepCard = new CardPanel();
            stepCard.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
            stepCard.setPreferredSize(new Dimension(170, 36));

            if (isCurrent) {
                stepCard.setBackgroundColor(DesignSystem.ELECTRIC_BLUE);
                stepCard.setBorderColor(DesignSystem.ELECTRIC_BLUE);
            } else if (isCompleted) {
                stepCard.setBackgroundColor(DesignSystem.SUCCESS_LIGHT);
                stepCard.setBorderColor(DesignSystem.SUCCESS);
            } else {
                stepCard.setBackgroundColor(Color.WHITE);
                stepCard.setBorderColor(DesignSystem.CARD_BORDER);
            }

            Icon stepIcon = isCompleted
                    ? (isCurrent ? VectorIcons.statusDot(Color.WHITE, 9) : VectorIcons.check(DesignSystem.SUCCESS_TEXT, 12))
                    : VectorIcons.circleOutline(DesignSystem.TEXT_MUTED, 9);
            JLabel lbl = new JLabel((i + 1) + ". " + s.getDisplayName(), stepIcon, JLabel.CENTER);
            lbl.setIconTextGap(6);
            lbl.setFont(DesignSystem.FONT_SMALL_BOLD);
            lbl.setForeground(isCurrent ? Color.WHITE : (isCompleted ? DesignSystem.SUCCESS_TEXT : DesignSystem.TEXT_MUTED));
            stepCard.add(lbl);

            stepper.add(stepCard);
        }

        return stepper;
    }

    private JPanel buildPrioritizationExplainPanel(Complaint c) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        PriorityService.FactorBreakdown fb = priorityService.getDetailedFactorBreakdown(
                c.getSeverity(), c.getSafetyRisk(), c.getPeopleAffected(), c.getOperationalImpact(), c.getCategory());
        SlaSnapshot sla = new EmergencyEscalationService().computeSlaSnapshot(c);

        CardPanel summaryCard = new CardPanel(new GridLayout(1, 4, 12, 0));
        summaryCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        StatCard cScore = new StatCard("Priority Score", String.format("%.1f", fb.totalScore()), "Out of 100", DesignSystem.ELECTRIC_BLUE);
        StatCard cClass = new StatCard("Classification", c.getPriority().name(), "Urgency tier",
                c.getPriority() == com.smartcampus.model.enums.PriorityLevel.CRITICAL ? DesignSystem.CRITICAL : DesignSystem.WARNING);
        StatCard cSla = new StatCard("SLA Countdown", sla.formatCountdownDisplay(),
                "Status: " + sla.slaStatus().getDisplayName() + " (" + fb.slaHours() + "h target)",
                sla.slaStatus() == com.smartcampus.model.enums.SlaStatus.BREACHED ? DesignSystem.CRITICAL : DesignSystem.CYAN);
        StatCard cDriver = new StatCard("Primary Driver", "Factor High", fb.primaryDriver(), DesignSystem.VIOLET);

        summaryCard.add(cScore);
        summaryCard.add(cClass);
        summaryCard.add(cSla);
        summaryCard.add(cDriver);
        panel.add(summaryCard);
        panel.add(Box.createVerticalStrut(16));

        CardPanel actionCard = new CardPanel(new BorderLayout(8, 6));
        actionCard.setBackgroundColor(new Color(248, 250, 252));
        actionCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblActTitle = new JLabel("AUTOMATED RESPONSE PROTOCOL & ACTION RECOMMENDATION");
        lblActTitle.setFont(DesignSystem.FONT_CARD_LABEL);
        lblActTitle.setForeground(DesignSystem.ELECTRIC_BLUE);
        actionCard.add(lblActTitle, BorderLayout.NORTH);

        JLabel lblAct = new JLabel("<html><body style='width: 700px; font-size: 11px;'>" + fb.recommendedAction() + "</body></html>");
        lblAct.setFont(DesignSystem.FONT_BODY_BOLD);
        lblAct.setForeground(DesignSystem.TEXT_PRIMARY);
        actionCard.add(lblAct, BorderLayout.CENTER);
        panel.add(actionCard);
        panel.add(Box.createVerticalStrut(16));

        CardPanel weightsCard = new CardPanel(new GridLayout(4, 2, 8, 8));
        weightsCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        addFactorRow(weightsCard, "1. Severity Component (Weight 35%):",
                String.format("Input: %d/5 | Points: %.1f / 35.0", c.getSeverity(), fb.severityPoints()));
        addFactorRow(weightsCard, "2. Safety Hazard Risk (Weight 30%):",
                String.format("Input: %d/5 | Points: %.1f / 30.0", c.getSafetyRisk(), fb.safetyPoints()));
        addFactorRow(weightsCard, "3. Population Affected (Weight 20%):",
                String.format("Input: %d people | Points: %.1f / 20.0", c.getPeopleAffected(), fb.affectedPoints()));
        addFactorRow(weightsCard, "4. Operational Disruption (Weight 15%):",
                String.format("Input: %d/5 | Points: %.1f / 15.0", c.getOperationalImpact(), fb.impactPoints()));

        panel.add(weightsCard);
        return panel;
    }

    private void addFactorRow(JPanel p, String title, String val) {
        JLabel t = new JLabel(title);
        t.setFont(DesignSystem.FONT_SMALL_BOLD);
        t.setForeground(DesignSystem.TEXT_PRIMARY);

        JLabel v = new JLabel(val);
        v.setFont(DesignSystem.FONT_SMALL);
        v.setForeground(DesignSystem.TEXT_SECONDARY);

        p.add(t);
        p.add(v);
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.28;
        JLabel lbl = new JLabel(label);
        lbl.setFont(DesignSystem.FONT_SMALL_BOLD);
        lbl.setForeground(DesignSystem.TEXT_SECONDARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.72;
        JLabel val = new JLabel(value != null ? value : "-");
        val.setFont(DesignSystem.FONT_BODY);
        val.setForeground(DesignSystem.TEXT_PRIMARY);
        panel.add(val, gbc);
    }
}
