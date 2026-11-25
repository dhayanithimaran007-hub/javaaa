package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.EmergencyDashboardMetrics;
import com.smartcampus.model.EmergencyIncidentDTO;
import com.smartcampus.model.enums.SlaStatus;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.EmergencyEscalationService;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class EmergencyOperationsPanel extends JPanel {
    private final StatCard cardActiveCritical;
    private final StatCard cardApproaching;
    private final StatCard cardBreached;
    private final StatCard cardEscalated;
    private final StatCard cardAvgResponse;
    private final StatCard cardResolutionRate;

    private final JComboBox<String> cmbBuildingFilter;
    private final JComboBox<String> cmbEscalationFilter;
    private final JComboBox<String> cmbSlaFilter;
    private final JTextField txtSearch;
    private final JProgressBar progressBar;

    private final DefaultTableModel tableModel;
    private final JTable tableIncidents;

    private final EmergencyEscalationService escalationService = new EmergencyEscalationService();
    private final ComplaintService complaintService = new ComplaintService();

    private List<EmergencyIncidentDTO> allLoadedIncidents = new ArrayList<>();
    private List<EmergencyIncidentDTO> filteredIncidents = new ArrayList<>();

    public EmergencyOperationsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topSection = new JPanel();
        topSection.setOpaque(false);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel title = new JLabel("Emergency Operations & SLA Escalation Command",
                VectorIcons.warning(DesignSystem.CRITICAL, 18), JLabel.LEFT);
        title.setIconTextGap(8);
        title.setFont(DesignSystem.FONT_DISPLAY_MD);
        title.setForeground(DesignSystem.TEXT_PRIMARY);

        JLabel sub = new JLabel("Showing safety-critical/CRITICAL incidents, HIGH complaints at >=50% SLA, approaching-SLA (>=75%), and breached SLAs (routine MEDIUM/LOW on-track tickets remain in All Complaints)");
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_SECONDARY);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(sub);
        titleBar.add(titleStack, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);
        ModernButton btnRunScan = new ModernButton("Run SLA Scan Now", ModernButton.ButtonType.DANGER);
        btnRunScan.setIcon(VectorIcons.bolt(Color.WHITE, 14));
        btnRunScan.setIconTextGap(6);
        ModernButton btnRefresh = new ModernButton("Refresh", ModernButton.ButtonType.SECONDARY);
        btnRefresh.setIcon(VectorIcons.refresh(DesignSystem.TEXT_PRIMARY, 14));
        btnRefresh.setIconTextGap(6);

        topActions.add(btnRunScan);
        topActions.add(btnRefresh);
        titleBar.add(topActions, BorderLayout.EAST);

        topSection.add(titleBar);
        topSection.add(Box.createVerticalStrut(12));

        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 10, 10));
        kpiGrid.setOpaque(false);
        cardActiveCritical = new StatCard("Critical Incidents", "0", "Active safety/critical", DesignSystem.CRITICAL);
        cardApproaching = new StatCard("Approaching SLA", "0", ">=75% SLA elapsed", DesignSystem.WARNING);
        cardBreached = new StatCard("Breached SLA", "0", "Overdue deadlines", new Color(0xBE, 0x12, 0x3C));
        cardEscalated = new StatCard("Escalated (L2+)", "0", "Active escalations", DesignSystem.VIOLET);
        cardAvgResponse = new StatCard("Avg Response", "0.0h", "Time to dispatch", DesignSystem.ELECTRIC_BLUE);
        cardResolutionRate = new StatCard("Emergency Res %", "0%", "Critical resolved rate", DesignSystem.SUCCESS);

        kpiGrid.add(cardActiveCritical);
        kpiGrid.add(cardApproaching);
        kpiGrid.add(cardBreached);
        kpiGrid.add(cardEscalated);
        kpiGrid.add(cardAvgResponse);
        kpiGrid.add(cardResolutionRate);
        topSection.add(kpiGrid);
        topSection.add(Box.createVerticalStrut(12));

        CardPanel filterCard = new CardPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterCard.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        cmbBuildingFilter = new JComboBox<>(new String[]{"All Buildings"});
        cmbEscalationFilter = new JComboBox<>(new String[]{"All Levels", "Level 1+ (Warning)", "Level 2+ (Breach)", "Level 3 (Admin Esc)"});
        cmbSlaFilter = new JComboBox<>(new String[]{"All SLA States", "BREACHED", "APPROACHING_DEADLINE", "ON_TRACK"});
        txtSearch = new JTextField(16);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search ID, title, room...");

        ModernButton btnApply = new ModernButton("Filter", ModernButton.ButtonType.PRIMARY);

        filterCard.add(new JLabel("Building:"));
        filterCard.add(cmbBuildingFilter);
        filterCard.add(new JLabel("Escalation:"));
        filterCard.add(cmbEscalationFilter);
        filterCard.add(new JLabel("SLA:"));
        filterCard.add(cmbSlaFilter);
        filterCard.add(new JLabel("Search:"));
        filterCard.add(txtSearch);
        filterCard.add(btnApply);

        topSection.add(filterCard);
        add(topSection, BorderLayout.NORTH);

        CardPanel tableCard = new CardPanel(new BorderLayout(8, 8));
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        tableCard.add(progressBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Complaint Title", "Building", "Location", "Priority", "Safety Risk",
                "Assigned Staff", "SLA Countdown", "Escalation Level", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableIncidents = new JTable(tableModel);
        tableIncidents.setRowHeight(34);
        tableIncidents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableIncidents.setFont(DesignSystem.FONT_SMALL);
        tableIncidents.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        DefaultTableCellRenderer highlightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && row >= 0 && row < filteredIncidents.size()) {
                    EmergencyIncidentDTO dto = filteredIncidents.get(row);
                    if (dto.slaSnapshot().slaStatus() == SlaStatus.BREACHED || dto.currentEscalationLevel().getLevel() >= 2) {
                        c.setBackground(new Color(255, 241, 242));
                        c.setForeground(column == 7 ? DesignSystem.CRITICAL_TEXT : DesignSystem.TEXT_PRIMARY);
                    } else if (dto.slaSnapshot().slaStatus() == SlaStatus.APPROACHING_DEADLINE) {
                        c.setBackground(new Color(255, 251, 235));
                        c.setForeground(column == 7 ? DesignSystem.WARNING_TEXT : DesignSystem.TEXT_PRIMARY);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(DesignSystem.TEXT_PRIMARY);
                    }
                }
                return c;
            }
        };
        tableIncidents.setDefaultRenderer(Object.class, highlightRenderer);
        tableIncidents.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableIncidents.getColumnModel().getColumn(8).setCellRenderer(new BadgeCellRenderer());
        tableIncidents.getColumnModel().getColumn(9).setCellRenderer(new BadgeCellRenderer());

        tableIncidents.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableIncidents.getColumnModel().getColumn(1).setPreferredWidth(210);
        tableIncidents.getColumnModel().getColumn(2).setPreferredWidth(120);
        tableIncidents.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableIncidents.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableIncidents.getColumnModel().getColumn(5).setPreferredWidth(80);
        tableIncidents.getColumnModel().getColumn(6).setPreferredWidth(125);
        tableIncidents.getColumnModel().getColumn(7).setPreferredWidth(135);
        tableIncidents.getColumnModel().getColumn(8).setPreferredWidth(145);
        tableIncidents.getColumnModel().getColumn(9).setPreferredWidth(100);

        tableIncidents.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableIncidents.getSelectedRow() >= 0) {
                    inspectSelected(0);
                }
            }
        });

        tableCard.add(new JScrollPane(tableIncidents), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        bottomBar.setOpaque(false);

        ModernButton btnInspect = new ModernButton("Inspect Incident", ModernButton.ButtonType.PRIMARY);
        ModernButton btnReassign = new ModernButton("Reassign Staff", ModernButton.ButtonType.SECONDARY);
        ModernButton btnAck = new ModernButton("Acknowledge Alert", ModernButton.ButtonType.SUCCESS);
        ModernButton btnNote = new ModernButton("Add Intervention Note", ModernButton.ButtonType.SECONDARY);
        ModernButton btnEscHist = new ModernButton("View Escalation History", ModernButton.ButtonType.GHOST);

        bottomBar.add(btnEscHist);
        bottomBar.add(btnNote);
        bottomBar.add(btnAck);
        bottomBar.add(btnReassign);
        bottomBar.add(btnInspect);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadEmergencyData());
        btnRunScan.addActionListener(e -> runManualEscalationScan());
        btnApply.addActionListener(e -> applyFilters());
        cmbBuildingFilter.addActionListener(e -> applyFilters());
        cmbEscalationFilter.addActionListener(e -> applyFilters());
        cmbSlaFilter.addActionListener(e -> applyFilters());

        btnInspect.addActionListener(e -> inspectSelected(0));
        btnEscHist.addActionListener(e -> inspectSelected(1));
        btnReassign.addActionListener(e -> reassignSelected());
        btnAck.addActionListener(e -> acknowledgeSelected());
        btnNote.addActionListener(e -> addInterventionNoteSelected());
    }

    public void loadEmergencyData() {
        progressBar.setVisible(true);
        new SwingWorker<EmergencyPayload, Void>() {
            @Override
            protected EmergencyPayload doInBackground() throws Exception {
                EmergencyEscalationService.EmergencyCommandData data = escalationService.getEmergencyCommandData();
                List<String> buildings = complaintService.getDistinctBuildings();
                return new EmergencyPayload(data, buildings);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    EmergencyPayload payload = get();
                    EmergencyDashboardMetrics m = payload.data.metrics();
                    cardActiveCritical.setValue(m.activeCriticalIncidents());
                    cardApproaching.setValue(m.approachingDeadlines());
                    cardBreached.setValue(m.breachedSlaCount());
                    cardEscalated.setValue(m.escalatedComplaintsCount());
                    cardAvgResponse.setValue(String.format("%.1fh", m.avgEmergencyResponseHours()));
                    cardResolutionRate.setValue(String.format("%.0f%%", m.emergencyResolutionRate()));

                    Object selBldg = cmbBuildingFilter.getSelectedItem();
                    cmbBuildingFilter.removeAllItems();
                    cmbBuildingFilter.addItem("All Buildings");
                    for (String b : payload.buildings) {
                        cmbBuildingFilter.addItem(b);
                    }
                    if (selBldg != null) cmbBuildingFilter.setSelectedItem(selBldg);

                    allLoadedIncidents = payload.data.incidents();
                    applyFilters();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyOperationsPanel.this,
                            "Failed to load emergency operations data: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyFilters() {
        String bldgFilter = cmbBuildingFilter.getSelectedItem() != null ? cmbBuildingFilter.getSelectedItem().toString() : "All Buildings";
        int escIdx = cmbEscalationFilter.getSelectedIndex();
        String slaFilter = cmbSlaFilter.getSelectedItem() != null ? cmbSlaFilter.getSelectedItem().toString() : "All SLA States";
        String query = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";

        filteredIncidents = new ArrayList<>();
        tableModel.setRowCount(0);

        for (EmergencyIncidentDTO dto : allLoadedIncidents) {
            Complaint c = dto.complaint();
            if (!"All Buildings".equals(bldgFilter) && !bldgFilter.equalsIgnoreCase(c.getBuilding())) continue;
            if (escIdx == 1 && dto.currentEscalationLevel().getLevel() < 1) continue;
            if (escIdx == 2 && dto.currentEscalationLevel().getLevel() < 2) continue;
            if (escIdx == 3 && dto.currentEscalationLevel().getLevel() < 3) continue;
            if (!"All SLA States".equals(slaFilter) && !slaFilter.equals(dto.slaSnapshot().slaStatus().name())) continue;
            if (!query.isEmpty()) {
                String hay = (c.getComplaintId() + " " + c.getTitle() + " " + c.getBuilding() + " " + c.getRoomNumber()).toLowerCase();
                if (!hay.contains(query)) continue;
            }

            filteredIncidents.add(dto);
            tableModel.addRow(new Object[]{
                    c.getComplaintId(),
                    c.getTitle() + (c.isSafetyCritical() ? " [SAFETY]" : ""),
                    c.getBuilding(),
                    c.getRoomNumber(),
                    c.getPriority(),
                    c.getSafetyRisk() + " / 5",
                    c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Unassigned",
                    dto.slaSnapshot().formatCountdownDisplay(),
                    dto.currentEscalationLevel(),
                    c.getStatus()
            });
        }
    }

    private void runManualEscalationScan() {
        progressBar.setVisible(true);
        new SwingWorker<EmergencyEscalationService.EscalationCycleResult, Void>() {
            @Override
            protected EmergencyEscalationService.EscalationCycleResult doInBackground() throws Exception {
                return escalationService.evaluateAndEscalateActiveComplaints();
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    EmergencyEscalationService.EscalationCycleResult res = get();
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(EmergencyOperationsPanel.this);
                    if (res.totalNewEvents() > 0) {
                        ToastNotification.showWarning(parent,
                                String.format("SLA Scan: %d warning(s), %d breach(es), %d admin escalation(s) logged.",
                                        res.newWarnings(), res.newBreaches(), res.newAdminEscalations()));
                    } else {
                        ToastNotification.showInfo(parent,
                                "SLA Scan completed (" + res.scannedComplaints() + " active incidents checked; no new escalations).");
                    }
                    loadEmergencyData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyOperationsPanel.this,
                            "SLA scan failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private EmergencyIncidentDTO getSelectedIncident(String actionLabel) {
        int row = tableIncidents.getSelectedRow();
        if (row < 0 || row >= filteredIncidents.size()) {
            JOptionPane.showMessageDialog(this, "Please select an incident to " + actionLabel + ".",
                    "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return filteredIncidents.get(row);
    }

    private void inspectSelected(int tabIndex) {
        EmergencyIncidentDTO dto = getSelectedIncident("inspect");
        if (dto == null) return;
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        EmergencyIncidentDialog dlg = new EmergencyIncidentDialog(parent, dto.complaint().getComplaintId(), tabIndex);
        dlg.setVisible(true);
        if (dlg.isModified()) loadEmergencyData();
    }

    private void reassignSelected() {
        EmergencyIncidentDTO dto = getSelectedIncident("reassign");
        if (dto == null) return;
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        Complaint c = dto.complaint();
        AssignStaffDialog dlg = new AssignStaffDialog(parent, c.getComplaintId(), c.getTitle(), c.getCategory());
        dlg.setVisible(true);
        if (dlg.isAssignedSuccessfully()) {
            ToastNotification.showSuccess(parent, "Staff reassigned for incident #" + c.getComplaintId());
            loadEmergencyData();
        }
    }

    private void acknowledgeSelected() {
        EmergencyIncidentDTO dto = getSelectedIncident("acknowledge");
        if (dto == null) return;
        int id = dto.complaint().getComplaintId();
        String note = JOptionPane.showInputDialog(this,
                "Acknowledge emergency alert for Complaint #" + id + "\nOptional note:",
                "Acknowledge Alert", JOptionPane.QUESTION_MESSAGE);
        if (note == null) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                escalationService.acknowledgeAlert(id, note);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(EmergencyOperationsPanel.this);
                    ToastNotification.showSuccess(parent, "Alert acknowledged for incident #" + id);
                    loadEmergencyData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyOperationsPanel.this,
                            "Error: " + ex.getMessage(), "Acknowledgment Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addInterventionNoteSelected() {
        EmergencyIncidentDTO dto = getSelectedIncident("add an intervention note to");
        if (dto == null) return;
        int id = dto.complaint().getComplaintId();
        String note = JOptionPane.showInputDialog(this,
                "Enter administrative intervention note for Complaint #" + id + ":",
                "Intervention Note", JOptionPane.PLAIN_MESSAGE);
        if (note == null || note.isBlank()) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                escalationService.addInterventionNote(id, note);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(EmergencyOperationsPanel.this);
                    ToastNotification.showSuccess(parent, "Intervention note logged for #" + id);
                    loadEmergencyData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmergencyOperationsPanel.this,
                            "Error: " + ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private record EmergencyPayload(
            EmergencyEscalationService.EmergencyCommandData data,
            List<String> buildings
    ) {}
}
