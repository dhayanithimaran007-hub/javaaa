package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintDetailsDTO;
import com.smartcampus.model.ComplaintEscalation;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.SlaSnapshot;
import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.Role;
import com.smartcampus.model.enums.SlaStatus;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.EmergencyEscalationService;
import com.smartcampus.service.PriorityService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

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
import java.util.List;

public class EmergencyIncidentDialog extends JDialog {
    private final int complaintId;
    private final JFrame ownerFrame;
    private final ComplaintService complaintService = new ComplaintService();
    private final EmergencyEscalationService escalationService = new EmergencyEscalationService();
    private final PriorityService priorityService = new PriorityService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final JPanel bodyContainer;
    private final JProgressBar progressBar;
    private final JTabbedPane tabs = new JTabbedPane();
    private final ModernButton btnAck;
    private final ModernButton btnNote;
    private final ModernButton btnReassign;

    private Complaint loadedComplaint;
    private boolean modified = false;

    public EmergencyIncidentDialog(JFrame parent, int complaintId) {
        this(parent, complaintId, 0);
    }

    public EmergencyIncidentDialog(JFrame parent, int complaintId, int initialTabIndex) {
        super(parent, "Emergency Incident Inspection - #" + complaintId, true);
        this.ownerFrame = parent;
        this.complaintId = complaintId;

        setSize(920, 680);
        setMinimumSize(new Dimension(780, 580));
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DesignSystem.MIDNIGHT_NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Emergency Operations Incident Inspector (#" + complaintId + ")",
                VectorIcons.warning(DesignSystem.WARNING, 18), JLabel.LEFT);
        title.setIconTextGap(8);
        title.setFont(DesignSystem.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Real-time SLA computation, safety override diagnostics, and escalation audit trail");
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        bodyContainer = new JPanel(new BorderLayout(10, 10));
        bodyContainer.setBackground(DesignSystem.BG_CANVAS);
        add(bodyContainer, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        footer.add(progressBar, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);

        btnAck = new ModernButton("Acknowledge Alert", ModernButton.ButtonType.PRIMARY);
        btnNote = new ModernButton("Add Intervention Note", ModernButton.ButtonType.SECONDARY);
        btnReassign = new ModernButton("Reassign Staff", ModernButton.ButtonType.WARNING);
        ModernButton btnClose = new ModernButton("Close", ModernButton.ButtonType.GHOST);

        boolean isAdmin = UserSession.hasRole(Role.ADMIN);
        btnAck.setVisible(isAdmin);
        btnNote.setVisible(isAdmin);
        btnReassign.setVisible(isAdmin);

        btnPanel.add(btnAck);
        btnPanel.add(btnNote);
        btnPanel.add(btnReassign);
        btnPanel.add(btnClose);
        footer.add(btnPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        btnAck.addActionListener(e -> handleAcknowledge());
        btnNote.addActionListener(e -> handleAddNote());
        btnReassign.addActionListener(e -> handleReassign());
        btnClose.addActionListener(e -> dispose());

        loadIncidentData(initialTabIndex);
    }

    public boolean isModified() {
        return modified;
    }

    private void loadIncidentData(int selectTab) {
        progressBar.setVisible(true);
        new SwingWorker<IncidentPayload, Void>() {
            @Override
            protected IncidentPayload doInBackground() throws Exception {
                ComplaintDetailsDTO details = complaintService.getComplaintDetails(complaintId);
                List<ComplaintEscalation> escalations = escalationService.getEscalationHistory(complaintId);
                SlaSnapshot sla = escalationService.computeSlaSnapshot(details.complaint());
                return new IncidentPayload(details, escalations, sla);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    IncidentPayload data = get();
                    renderPayload(data, selectTab);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyIncidentDialog.this,
                            "Failed to load incident details: " + ex.getMessage(),
                            "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderPayload(IncidentPayload data, int selectTab) {
        bodyContainer.removeAll();
        tabs.removeAll();

        Complaint c = data.details().complaint();
        this.loadedComplaint = c;
        SlaSnapshot sla = data.sla();

        JPanel topStrip = new JPanel(new GridLayout(1, 4, 10, 0));
        topStrip.setOpaque(false);
        topStrip.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));

        int maxRecordedLvl = data.escalations().stream()
                .mapToInt(ComplaintEscalation::getEscalationLevel).max().orElse(0);
        EscalationLevel effLevel = EscalationLevel.fromLevel(Math.max(maxRecordedLvl, sla.targetEscalationLevel().getLevel()));

        topStrip.add(new StatCard("SLA Target", sla.slaTargetHours() + "h",
                "Started " + dateFormat.format(sla.slaStart()), DesignSystem.ELECTRIC_BLUE));
        topStrip.add(new StatCard("SLA Countdown", sla.formatCountdownDisplay(),
                "Deadline: " + dateFormat.format(sla.slaDeadline()),
                sla.slaStatus() == SlaStatus.BREACHED ? DesignSystem.CRITICAL : DesignSystem.WARNING));
        topStrip.add(new StatCard("Priority Score", String.format("%.1f", c.getPriorityScore()),
                c.getPriority().name() + (c.isSafetyCritical() ? " [SAFETY]" : ""),
                c.isSafetyCritical() ? DesignSystem.CRITICAL : DesignSystem.VIOLET));
        topStrip.add(new StatCard("Escalation Tier", "Level " + effLevel.getLevel(),
                effLevel.getDisplayName(),
                effLevel.getLevel() >= 2 ? DesignSystem.CRITICAL : DesignSystem.CYAN));

        bodyContainer.add(topStrip, BorderLayout.NORTH);

        JPanel diagPanel = new JPanel();
        diagPanel.setLayout(new BoxLayout(diagPanel, BoxLayout.Y_AXIS));
        diagPanel.setBackground(Color.WHITE);
        diagPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        CardPanel hazardCard = new CardPanel(new GridBagLayout());
        hazardCard.setBackgroundColor(c.isSafetyCritical() || sla.slaStatus() == SlaStatus.BREACHED
                ? new Color(254, 242, 242) : new Color(248, 250, 252));
        hazardCard.setBorderColor(c.isSafetyCritical() || sla.slaStatus() == SlaStatus.BREACHED
                ? DesignSystem.CRITICAL : DesignSystem.CARD_BORDER);
        hazardCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        GridBagConstraints hgbc = new GridBagConstraints();
        hgbc.insets = new Insets(4, 6, 4, 6);
        hgbc.fill = GridBagConstraints.HORIZONTAL;
        hgbc.anchor = GridBagConstraints.WEST;

        addRow(hazardCard, hgbc, 0, "Affected Location:",
                c.getBuilding() + "  |  " + c.getRoomNumber() + "  (Category: " + c.getCategory() + ")");
        addRow(hazardCard, hgbc, 1, "Safety Classification:", escalationService.buildSafetyClassificationReason(c));

        PriorityService.FactorBreakdown fb = priorityService.getDetailedFactorBreakdown(
                c.getSeverity(), c.getSafetyRisk(), c.getPeopleAffected(), c.getOperationalImpact(), c.getCategory());
        addRow(hazardCard, hgbc, 2, "Priority Score Math:",
                String.format("Severity %.1f + Safety %.1f + Population %.1f + Impact %.1f = %.2f/100",
                        fb.severityPoints(), fb.safetyPoints(), fb.affectedPoints(), fb.impactPoints(), fb.totalScore()));
        addRow(hazardCard, hgbc, 3, "SLA Elapsed / Breach:",
                String.format("%s elapsed (%.1f%% of %dh window) | Breach duration: %s",
                        sla.formatElapsedDisplay(), sla.elapsedPercent(), sla.slaTargetHours(), sla.formatBreachDisplay()));
        addRow(hazardCard, hgbc, 4, "Current Status & Staff:",
                c.getStatus().getDisplayName() + " | Assigned: " +
                        (c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Unassigned"));

        diagPanel.add(hazardCard);
        diagPanel.add(Box.createVerticalStrut(12));

        JLabel descLbl = new JLabel("Incident Description & Recommended Action:");
        descLbl.setFont(DesignSystem.FONT_SMALL_BOLD);
        descLbl.setForeground(DesignSystem.TEXT_PRIMARY);
        descLbl.setAlignmentX(LEFT_ALIGNMENT);
        diagPanel.add(descLbl);
        diagPanel.add(Box.createVerticalStrut(6));

        JTextArea txtDesc = new JTextArea(c.getDescription() + "\n\nRecommended Action: " + fb.recommendedAction(), 5, 50);
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setFont(DesignSystem.FONT_BODY);
        txtDesc.setBackground(DesignSystem.BG_CANVAS);
        txtDesc.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        diagPanel.add(new JScrollPane(txtDesc));

        tabs.addTab("Criticality & SLA Diagnostics", diagPanel);

        String[] escCols = {"Level", "Prev", "SLA Status", "Escalation Reason", "Breach At", "Triggered At", "Ack By", "Intervention Note"};
        DefaultTableModel escModel = new DefaultTableModel(escCols, 0) {
            @Override public boolean isCellEditable(int r, int col) { return false; }
        };
        for (ComplaintEscalation e : data.escalations()) {
            escModel.addRow(new Object[]{
                    e.getEscalationLevelEnum(),
                    "L" + e.getPreviousLevel(),
                    e.getSlaStatus(),
                    e.getEscalationReason(),
                    e.getBreachAt() != null ? dateFormat.format(e.getBreachAt()) : "-",
                    e.getCreatedAt() != null ? dateFormat.format(e.getCreatedAt()) : "-",
                    e.getAcknowledgedByName() != null ? e.getAcknowledgedByName() : "Pending Ack",
                    e.getInterventionNote() != null ? e.getInterventionNote() : "-"
            });
        }
        JTable escTable = new JTable(escModel);
        escTable.setRowHeight(30);
        escTable.setFont(DesignSystem.FONT_SMALL);
        escTable.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        escTable.getColumnModel().getColumn(0).setCellRenderer(new BadgeCellRenderer());
        escTable.getColumnModel().getColumn(2).setCellRenderer(new BadgeCellRenderer());
        escTable.getColumnModel().getColumn(3).setPreferredWidth(230);
        escTable.getColumnModel().getColumn(7).setPreferredWidth(180);

        JPanel escPanel = new JPanel(new BorderLayout());
        escPanel.setBackground(Color.WHITE);
        escPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        escPanel.add(new JScrollPane(escTable), BorderLayout.CENTER);
        tabs.addTab("Escalation Timeline (" + data.escalations().size() + ")", escPanel);

        String[] histCols = {"Timestamp", "Actor", "Role", "Old Status", "New Status", "Remarks"};
        DefaultTableModel histModel = new DefaultTableModel(histCols, 0) {
            @Override public boolean isCellEditable(int r, int col) { return false; }
        };
        for (ComplaintStatusHistory h : data.details().historyList()) {
            histModel.addRow(new Object[]{
                    h.getChangedAt() != null ? dateFormat.format(h.getChangedAt()) : "",
                    h.getChangedByName(),
                    h.getChangedByRole(),
                    h.getOldStatus() != null ? h.getOldStatus().getDisplayName() : "-",
                    h.getNewStatus().getDisplayName(),
                    h.getRemarks()
            });
        }
        JTable histTable = new JTable(histModel);
        histTable.setRowHeight(28);
        histTable.setFont(DesignSystem.FONT_SMALL);
        histTable.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        histTable.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        histTable.getColumnModel().getColumn(5).setPreferredWidth(260);

        JPanel histPanel = new JPanel(new BorderLayout());
        histPanel.setBackground(Color.WHITE);
        histPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        histPanel.add(new JScrollPane(histTable), BorderLayout.CENTER);
        tabs.addTab("Assignment & Status History (" + data.details().historyList().size() + ")", histPanel);

        if (selectTab >= 0 && selectTab < tabs.getTabCount()) {
            tabs.setSelectedIndex(selectTab);
        }

        JPanel tabWrapper = new JPanel(new BorderLayout());
        tabWrapper.setOpaque(false);
        tabWrapper.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
        tabWrapper.add(tabs, BorderLayout.CENTER);
        bodyContainer.add(tabWrapper, BorderLayout.CENTER);

        bodyContainer.revalidate();
        bodyContainer.repaint();
    }

    private void handleAcknowledge() {
        String note = JOptionPane.showInputDialog(this,
                "Enter optional acknowledgment remark for Complaint #" + complaintId + ":",
                "Acknowledge Emergency Alert", JOptionPane.QUESTION_MESSAGE);
        if (note == null) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                escalationService.acknowledgeAlert(complaintId, note);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    modified = true;
                    ToastNotification.showSuccess(ownerFrame, "Emergency alert acknowledged for #" + complaintId);
                    loadIncidentData(1);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyIncidentDialog.this,
                            "Failed to acknowledge alert: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleAddNote() {
        String note = JOptionPane.showInputDialog(this,
                "Enter mandatory administrative intervention note for Complaint #" + complaintId + ":",
                "Add Intervention Note", JOptionPane.PLAIN_MESSAGE);
        if (note == null || note.isBlank()) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                escalationService.addInterventionNote(complaintId, note);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    modified = true;
                    ToastNotification.showSuccess(ownerFrame, "Intervention note recorded for #" + complaintId);
                    loadIncidentData(1);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyIncidentDialog.this,
                            "Failed to record note: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleReassign() {
        if (loadedComplaint == null) return;
        AssignStaffDialog dlg = new AssignStaffDialog(ownerFrame, complaintId, loadedComplaint.getTitle(), loadedComplaint.getCategory());
        dlg.setVisible(true);
        if (dlg.isAssignedSuccessfully()) {
            modified = true;
            loadIncidentData(2);
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        JLabel k = new JLabel(label);
        k.setFont(DesignSystem.FONT_SMALL_BOLD);
        k.setForeground(DesignSystem.TEXT_SECONDARY);
        panel.add(k, gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        JLabel v = new JLabel("<html><body style='width: 560px;'>" + value + "</body></html>");
        v.setFont(DesignSystem.FONT_BODY);
        v.setForeground(DesignSystem.TEXT_PRIMARY);
        panel.add(v, gbc);
    }

    private record IncidentPayload(
            ComplaintDetailsDTO details,
            List<ComplaintEscalation> escalations,
            SlaSnapshot sla
    ) {}
}
