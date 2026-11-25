package com.smartcampus.ui.admin;

import com.smartcampus.model.ActivityFeedItem;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.EmergencyEscalationService;
import com.smartcampus.service.EmergencyMonitoringService;
import com.smartcampus.ui.LoginFrame;
import com.smartcampus.ui.charts.DonutChartPanel;
import com.smartcampus.ui.charts.TrendLineChartPanel;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.DatabaseConnectionStatusDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ModernSidebar;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.student.SubmitComplaintDialog;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AdminDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    private final ModernSidebar sidebar;

    private final JPanel overviewPanel;
    private final AllComplaintsPanel allComplaintsPanel = new AllComplaintsPanel();
    private final StaffManagementPanel staffPanel = new StaffManagementPanel();
    private final StudentManagementPanel studentPanel = new StudentManagementPanel();
    private final UserManagementPanel userPanel = new UserManagementPanel();
    private final ReportsPanel reportsPanel = new ReportsPanel();
    private final AuditLogsPanel auditPanel = new AuditLogsPanel();
    private final EmergencyOperationsPanel emergencyPanel = new EmergencyOperationsPanel();
    private final PredictiveMaintenancePanel predictivePanel = new PredictiveMaintenancePanel();

    private final JLabel lblClock;
    private final JLabel lblCampusStatus;
    private final JLabel lblDbStatus;

    private final CardPanel safetyAlertBanner;
    private final JLabel lblAlertMessage;
    private final ModernButton btnAlertDispatch;

    private final StatCard cardTotal;
    private final StatCard cardSubmitted;
    private final StatCard cardAcknowledged;
    private final StatCard cardInProgress;
    private final StatCard cardResolved;
    private final StatCard cardCritical;
    private final StatCard cardOverdue;
    private final StatCard cardAvgTime;

    private final TrendLineChartPanel trendChart;
    private final DonutChartPanel categoryChart;

    private final JTable tableUrgent;
    private final DefaultTableModel urgentModel;
    private final ActivityFeedPanel activityFeedPanel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final EmergencyEscalationService escalationService = new EmergencyEscalationService();
    private final EmergencyMonitoringService monitoringService = new EmergencyMonitoringService(escalationService);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("EEE, dd MMM yyyy  HH:mm:ss");
    private Timer clockTimer;

    private int unassignedSafetyCount = 0;

    public AdminDashboard() {
        super("Smart Campus - Elite Operations Command Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        sidebar = new ModernSidebar("Smart Campus", "Ops Command Center");
        sidebar.addNavItem("OVERVIEW", VectorIcons.of(VectorIcons.Kind.DASHBOARD, 16), "Command Center");
        sidebar.addNavItem("COMPLAINTS", VectorIcons.of(VectorIcons.Kind.CLIPBOARD, 16), "All Complaints");
        sidebar.addNavItem("STAFF", VectorIcons.of(VectorIcons.Kind.WRENCH, 16), "Staff Directory");
        sidebar.addNavItem("STUDENTS", VectorIcons.of(VectorIcons.Kind.GRADUATION, 16), "Student Directory");
        sidebar.addNavItem("USERS", VectorIcons.of(VectorIcons.Kind.USERS, 16), "User Accounts");
        sidebar.addNavItem("REPORTS", VectorIcons.of(VectorIcons.Kind.TREND, 16), "Analytics & Reports");
        sidebar.addNavItem("AUDIT", VectorIcons.of(VectorIcons.Kind.SHIELD, 16), "Security Audit");
        sidebar.addNavItem("DB", VectorIcons.of(VectorIcons.Kind.DATABASE, 16), "Database Health");
        sidebar.addNavItem("EMERGENCY", VectorIcons.of(VectorIcons.Kind.ALERT, 16), "Emergency Operations");
        sidebar.addNavItem("PREDICTIVE", VectorIcons.of(VectorIcons.Kind.TREND, 16), "Predictive Maintenance");

        sidebar.setNavSelectedListener(this::handleNavigation);
        sidebar.setLogoutListener(this::handleLogout);
        add(sidebar, BorderLayout.WEST);

        overviewPanel = new JPanel(new BorderLayout(12, 12));
        overviewPanel.setBackground(DesignSystem.BG_CANVAS);
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        CardPanel topCommandBar = new CardPanel(new BorderLayout(12, 0));
        topCommandBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftStatus.setOpaque(false);

        lblCampusStatus = new JLabel("CAMPUS OPERATIONS: OPTIMAL",
                VectorIcons.statusDot(DesignSystem.SUCCESS, 10), JLabel.LEFT);
        lblCampusStatus.setIconTextGap(6);
        lblCampusStatus.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblCampusStatus.setForeground(DesignSystem.SUCCESS);
        leftStatus.add(lblCampusStatus);

        lblDbStatus = new JLabel("MySQL 8.0: Connected (1ms)",
                VectorIcons.statusDot(DesignSystem.SUCCESS, 8), JLabel.LEFT);
        lblDbStatus.setIconTextGap(6);
        lblDbStatus.setFont(DesignSystem.FONT_SMALL);
        lblDbStatus.setForeground(DesignSystem.TEXT_MUTED);
        leftStatus.add(lblDbStatus);
        topCommandBar.add(leftStatus, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        lblClock = new JLabel(clockFormat.format(new Date()));
        lblClock.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblClock.setForeground(DesignSystem.TEXT_PRIMARY);
        rightControls.add(lblClock);

        ModernButton btnTopRefresh = new ModernButton("Refresh", ModernButton.ButtonType.SECONDARY);
        btnTopRefresh.setIcon(VectorIcons.refresh(DesignSystem.TEXT_PRIMARY, 14));
        btnTopRefresh.setIconTextGap(6);
        btnTopRefresh.setPreferredSize(new Dimension(108, 32));
        btnTopRefresh.addActionListener(e -> loadDashboardData());
        rightControls.add(btnTopRefresh);

        topCommandBar.add(rightControls, BorderLayout.EAST);

        clockTimer = new Timer(1000, e -> lblClock.setText(clockFormat.format(new Date())));
        clockTimer.start();

        safetyAlertBanner = new CardPanel(new BorderLayout(12, 0));
        safetyAlertBanner.setBackgroundColor(new Color(254, 242, 242));
        safetyAlertBanner.setBorderColor(DesignSystem.CRITICAL);
        safetyAlertBanner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        safetyAlertBanner.setVisible(false);

        JPanel alertTextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        alertTextPanel.setOpaque(false);
        JLabel alertIcon = new JLabel(VectorIcons.warning(DesignSystem.CRITICAL, 18));
        alertTextPanel.add(alertIcon);

        lblAlertMessage = new JLabel("CRITICAL SAFETY ALERT: Active hazardous complaints require immediate human review & technician dispatch!");
        lblAlertMessage.setFont(DesignSystem.FONT_BODY_BOLD);
        lblAlertMessage.setForeground(DesignSystem.CRITICAL);
        alertTextPanel.add(lblAlertMessage);
        safetyAlertBanner.add(alertTextPanel, BorderLayout.CENTER);

        btnAlertDispatch = new ModernButton("Immediate Dispatch", ModernButton.ButtonType.DANGER);
        btnAlertDispatch.setIcon(VectorIcons.bolt(Color.WHITE, 14));
        btnAlertDispatch.setIconTextGap(6);
        btnAlertDispatch.setPreferredSize(new Dimension(176, 32));
        btnAlertDispatch.addActionListener(e -> {
            sidebar.setActiveItem("EMERGENCY");
            cardLayout.show(mainContentPanel, "EMERGENCY");
            emergencyPanel.loadEmergencyData();
        });
        safetyAlertBanner.add(btnAlertDispatch, BorderLayout.EAST);

        JPanel topWrapper = new JPanel();
        topWrapper.setOpaque(false);
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        topWrapper.add(topCommandBar);
        topWrapper.add(Box.createVerticalStrut(10));
        topWrapper.add(safetyAlertBanner);
        overviewPanel.add(topWrapper, BorderLayout.NORTH);

        JPanel centerContent = new JPanel();
        centerContent.setOpaque(false);
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));

        JPanel statsContainer = new JPanel(new GridLayout(2, 4, 12, 12));
        statsContainer.setOpaque(false);

        cardTotal = new StatCard("Total Complaints", "0", "All time entries", DesignSystem.ELECTRIC_BLUE);
        cardSubmitted = new StatCard("Submitted (New)", "0", "Awaiting review", new Color(0x63, 0x66, 0xF1));
        cardAcknowledged = new StatCard("Acknowledged", "0", "Assigned / Verified", DesignSystem.VIOLET);
        cardInProgress = new StatCard("In Progress", "0", "Under active repair", DesignSystem.WARNING);

        cardResolved = new StatCard("Resolved", "0", "Closed successfully", DesignSystem.SUCCESS);
        cardCritical = new StatCard("Critical Safety", "0", "Immediate attention", DesignSystem.CRITICAL);
        cardOverdue = new StatCard("Overdue (>72h)", "0", "SLA escalation required", new Color(0xBE, 0x12, 0x3C));
        cardAvgTime = new StatCard("Avg Turnaround", "0h", "Average turnaround", DesignSystem.CYAN);

        statsContainer.add(cardTotal);
        statsContainer.add(cardSubmitted);
        statsContainer.add(cardAcknowledged);
        statsContainer.add(cardInProgress);
        statsContainer.add(cardResolved);
        statsContainer.add(cardCritical);
        statsContainer.add(cardOverdue);
        statsContainer.add(cardAvgTime);
        centerContent.add(statsContainer);
        centerContent.add(Box.createVerticalStrut(12));

        CardPanel quickActionsCard = new CardPanel(new BorderLayout(10, 0));
        quickActionsCard.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel lblQuick = new JLabel("OPERATIONS QUICK ACTIONS:");
        lblQuick.setFont(DesignSystem.FONT_CARD_LABEL);
        lblQuick.setForeground(DesignSystem.TEXT_MUTED);
        quickActionsCard.add(lblQuick, BorderLayout.WEST);

        JPanel actButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actButtons.setOpaque(false);

        ModernButton qaRegister = new ModernButton("Log Incident", ModernButton.ButtonType.PRIMARY);
        qaRegister.setIcon(VectorIcons.of(VectorIcons.Kind.PLUS, Color.WHITE, 13));
        qaRegister.setIconTextGap(6);

        ModernButton qaDispatch = new ModernButton("Dispatch Staff", ModernButton.ButtonType.SECONDARY);
        qaDispatch.setIcon(VectorIcons.bolt(DesignSystem.TEXT_PRIMARY, 14));
        qaDispatch.setIconTextGap(6);

        ModernButton qaReport = new ModernButton("Executive Report", ModernButton.ButtonType.SECONDARY);
        qaReport.setIcon(VectorIcons.chart(DesignSystem.TEXT_PRIMARY, 14));
        qaReport.setIconTextGap(6);

        ModernButton qaDb = new ModernButton("DB Health", ModernButton.ButtonType.GHOST);
        qaDb.setIcon(VectorIcons.database(DesignSystem.TEXT_SECONDARY, 14));
        qaDb.setIconTextGap(6);

        actButtons.add(qaRegister);
        actButtons.add(qaDispatch);
        actButtons.add(qaReport);
        actButtons.add(qaDb);
        quickActionsCard.add(actButtons, BorderLayout.CENTER);

        qaRegister.addActionListener(e -> {
            SubmitComplaintDialog dlg = new SubmitComplaintDialog(this);
            dlg.setVisible(true);
            if (dlg.isSubmitted()) {
                ToastNotification.showSuccess(this, "Incident registered successfully.");
                loadDashboardData();
            }
        });

        qaDispatch.addActionListener(e -> {
            sidebar.setActiveItem("COMPLAINTS");
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            allComplaintsPanel.loadComplaints();
        });

        qaReport.addActionListener(e -> {
            sidebar.setActiveItem("REPORTS");
            cardLayout.show(mainContentPanel, "REPORTS");
            reportsPanel.loadReportData();
        });

        qaDb.addActionListener(e -> new DatabaseConnectionStatusDialog(this).setVisible(true));

        centerContent.add(quickActionsCard);
        centerContent.add(Box.createVerticalStrut(12));

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        chartsRow.setOpaque(false);
        chartsRow.setPreferredSize(new Dimension(1000, 220));

        CardPanel trendCard = new CardPanel(new BorderLayout(6, 6));
        trendCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel lblTrendTitle = new JLabel("14-Day Complaint Volume Dynamics");
        lblTrendTitle.setFont(DesignSystem.FONT_SUBTITLE);
        lblTrendTitle.setForeground(DesignSystem.TEXT_PRIMARY);
        trendCard.add(lblTrendTitle, BorderLayout.NORTH);
        trendChart = new TrendLineChartPanel();
        trendCard.add(trendChart, BorderLayout.CENTER);
        chartsRow.add(trendCard);

        CardPanel catCard = new CardPanel(new BorderLayout(6, 6));
        catCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel lblCatTitle = new JLabel("Complaints by Category Distribution");
        lblCatTitle.setFont(DesignSystem.FONT_SUBTITLE);
        lblCatTitle.setForeground(DesignSystem.TEXT_PRIMARY);
        catCard.add(lblCatTitle, BorderLayout.NORTH);
        categoryChart = new DonutChartPanel();
        catCard.add(categoryChart, BorderLayout.CENTER);
        chartsRow.add(catCard);

        centerContent.add(chartsRow);
        centerContent.add(Box.createVerticalStrut(12));

        JPanel splitRow = new JPanel(new BorderLayout(12, 0));
        splitRow.setOpaque(false);
        splitRow.setPreferredSize(new Dimension(1000, 260));

        CardPanel urgentCard = new CardPanel(new BorderLayout(10, 10));
        urgentCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel urgentHeader = new JPanel(new BorderLayout());
        urgentHeader.setOpaque(false);
        JLabel urgentTitle = new JLabel("High-Priority & Safety Action Queue");
        urgentTitle.setFont(DesignSystem.FONT_SUBTITLE);
        urgentTitle.setForeground(DesignSystem.TEXT_PRIMARY);

        ModernButton btnGoToAll = new ModernButton("Open Full Console", ModernButton.ButtonType.GHOST);
        btnGoToAll.setFont(DesignSystem.FONT_TINY);
        btnGoToAll.setPreferredSize(new Dimension(130, 28));
        btnGoToAll.addActionListener(e -> {
            sidebar.setActiveItem("COMPLAINTS");
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            allComplaintsPanel.loadComplaints();
        });

        urgentHeader.add(urgentTitle, BorderLayout.WEST);
        urgentHeader.add(btnGoToAll, BorderLayout.EAST);
        urgentCard.add(urgentHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Student", "Assigned Staff", "Date"};
        urgentModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableUrgent = new JTable(urgentModel);
        tableUrgent.setRowHeight(32);
        tableUrgent.setFont(DesignSystem.FONT_SMALL);
        tableUrgent.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableUrgent.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableUrgent.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableUrgent.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableUrgent.getColumnModel().getColumn(1).setPreferredWidth(190);
        tableUrgent.getColumnModel().getColumn(2).setPreferredWidth(120);
        tableUrgent.getColumnModel().getColumn(3).setPreferredWidth(120);
        tableUrgent.getColumnModel().getColumn(4).setPreferredWidth(80);
        tableUrgent.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableUrgent.getColumnModel().getColumn(6).setPreferredWidth(90);
        tableUrgent.getColumnModel().getColumn(7).setPreferredWidth(110);
        tableUrgent.getColumnModel().getColumn(8).setPreferredWidth(120);
        tableUrgent.getColumnModel().getColumn(9).setPreferredWidth(110);

        tableUrgent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableUrgent.getSelectedRow() >= 0) {
                    int id = (Integer) urgentModel.getValueAt(tableUrgent.getSelectedRow(), 0);
                    new ComplaintDetailsDialog(AdminDashboard.this, id).setVisible(true);
                }
            }
        });
        urgentCard.add(new JScrollPane(tableUrgent), BorderLayout.CENTER);
        splitRow.add(urgentCard, BorderLayout.CENTER);

        activityFeedPanel = new ActivityFeedPanel();
        activityFeedPanel.setPreferredSize(new Dimension(360, 260));
        splitRow.add(activityFeedPanel, BorderLayout.EAST);

        centerContent.add(splitRow);

        JScrollPane mainScroll = new JScrollPane(centerContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        overviewPanel.add(mainScroll, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        overviewPanel.add(progressBar, BorderLayout.SOUTH);

        mainContentPanel.add(overviewPanel, "OVERVIEW");
        mainContentPanel.add(allComplaintsPanel, "COMPLAINTS");
        mainContentPanel.add(staffPanel, "STAFF");
        mainContentPanel.add(studentPanel, "STUDENTS");
        mainContentPanel.add(userPanel, "USERS");
        mainContentPanel.add(reportsPanel, "REPORTS");
        mainContentPanel.add(auditPanel, "AUDIT");
        mainContentPanel.add(emergencyPanel, "EMERGENCY");
        mainContentPanel.add(predictivePanel, "PREDICTIVE");

        add(mainContentPanel, BorderLayout.CENTER);

        monitoringService.setCycleListener(result -> {
            if (result.totalNewEvents() > 0 && isDisplayable()) {
                String msg = result.notifications().isEmpty()
                        ? result.totalNewEvents() + " new SLA escalation event(s) detected."
                        : result.notifications().get(0);
                ToastNotification.showWarning(this, msg);
                loadDashboardData();
                if (emergencyPanel.isShowing()) {
                    emergencyPanel.loadEmergencyData();
                }
            }
        });
        monitoringService.start();

        loadDashboardData();
    }

    private void handleNavigation(String navId) {
        if ("DB".equals(navId)) {
            new DatabaseConnectionStatusDialog(this).setVisible(true);
            return;
        }

        cardLayout.show(mainContentPanel, navId);

        switch (navId) {
            case "OVERVIEW" -> loadDashboardData();
            case "COMPLAINTS" -> allComplaintsPanel.loadComplaints();
            case "STAFF" -> staffPanel.loadStaffList();
            case "STUDENTS" -> studentPanel.loadStudents();
            case "USERS" -> userPanel.loadUsers();
            case "REPORTS" -> reportsPanel.loadReportData();
            case "AUDIT" -> auditPanel.loadLogs();
            case "EMERGENCY" -> emergencyPanel.loadEmergencyData();
            case "PREDICTIVE" -> predictivePanel.loadPredictiveData();
        }
    }

    private void loadDashboardData() {
        progressBar.setVisible(true);

        new SwingWorker<CommandCenterData, Void>() {
            @Override
            protected CommandCenterData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> urgent = complaintService.listAllComplaintsForAdmin(
                        null, null, null, null, null, null, "priority", false);
                Map<String, Integer> trends = complaintService.getComplaintTrends(14);
                List<ActivityFeedItem> activities = complaintService.getRecentActivities(15);
                int safetyAlerts = complaintService.countUnassignedSafetyIssues();
                EmergencyEscalationService.EmergencyCommandData escData = escalationService.getEmergencyCommandData();
                int emergencyCount = escData.metrics().activeCriticalIncidents() + escData.metrics().breachedSlaCount();
                return new CommandCenterData(stats, urgent, trends, activities, safetyAlerts, emergencyCount,
                        escData.metrics().breachedSlaCount() + escData.metrics().approachingDeadlines());
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    CommandCenterData data = get();
                    DashboardStats stats = data.stats;

                    cardTotal.setValue(stats.getTotalComplaints());
                    cardSubmitted.setValue(stats.getSubmittedComplaints());
                    cardAcknowledged.setValue(stats.getAcknowledgedComplaints());
                    cardInProgress.setValue(stats.getInProgressComplaints());
                    cardResolved.setValue(stats.getResolvedComplaints());
                    cardCritical.setValue(stats.getCriticalComplaints());
                    cardOverdue.setValue(Math.max(stats.getOverdueComplaints(), data.slaAlertCount));
                    cardAvgTime.setValue(String.format("%.1fh", stats.getAvgResolutionHours()));

                    unassignedSafetyCount = data.safetyAlerts;
                    if (unassignedSafetyCount > 0) {
                        lblCampusStatus.setIcon(VectorIcons.warning(DesignSystem.CRITICAL, 13));
                        lblCampusStatus.setText("ACTIVE SAFETY HAZARDS: " + unassignedSafetyCount);
                        lblCampusStatus.setForeground(DesignSystem.CRITICAL);
                        lblAlertMessage.setText(String.format(
                                "CRITICAL SAFETY ALERT: %d unassigned hazardous complaint%s require immediate human review!",
                                unassignedSafetyCount, unassignedSafetyCount == 1 ? "" : "s"));
                        safetyAlertBanner.setVisible(true);
                    } else {
                        lblCampusStatus.setIcon(VectorIcons.statusDot(DesignSystem.SUCCESS, 10));
                        lblCampusStatus.setText("CAMPUS OPERATIONS: OPTIMAL");
                        lblCampusStatus.setForeground(DesignSystem.SUCCESS);
                        safetyAlertBanner.setVisible(false);
                    }

                    sidebar.setBadge("OVERVIEW", unassignedSafetyCount);
                    sidebar.setBadge("COMPLAINTS", stats.getSubmittedComplaints());
                    sidebar.setBadge("EMERGENCY", data.emergencyBadge);

                    trendChart.setData(data.trends);
                    categoryChart.setData(stats.getComplaintsByCategory());
                    categoryChart.setCenterText("Total", String.valueOf(stats.getTotalComplaints()));

                    urgentModel.setRowCount(0);
                    int count = 0;
                    for (Complaint c : data.urgentComplaints) {
                        if (count++ >= 12) break;
                        urgentModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " - " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getStudentName() != null ? c.getStudentName() : "ID #" + c.getStudentId(),
                                c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Unassigned",
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }

                    activityFeedPanel.updateFeed(data.activities);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Failed to load Command Center data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to sign out of the Operations Command Center?",
                "Sign Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            monitoringService.stop();
            UserSession.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    @Override
    public void dispose() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
        monitoringService.stop();
        super.dispose();
    }

    private record CommandCenterData(
            DashboardStats stats,
            List<Complaint> urgentComplaints,
            Map<String, Integer> trends,
            List<ActivityFeedItem> activities,
            int safetyAlerts,
            int emergencyBadge,
            int slaAlertCount
    ) {}
}
