package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.LoginFrame;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.DatabaseConnectionStatusDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    private final List<JButton> navButtons = new ArrayList<>();

    // Sub-panels
    private final JPanel overviewPanel;
    private final AllComplaintsPanel allComplaintsPanel;
    private final StaffManagementPanel staffPanel;
    private final UserManagementPanel userPanel;
    private final ReportsPanel reportsPanel;
    private final AuditLogsPanel auditPanel;

    // Stat Cards
    private final StatCard cardTotal;
    private final StatCard cardSubmitted;
    private final StatCard cardAcknowledged;
    private final StatCard cardInProgress;
    private final StatCard cardResolved;
    private final StatCard cardCritical;
    private final StatCard cardOverdue;
    private final StatCard cardAvgTime;

    // Critical queue table
    private final JTable tableUrgent;
    private final DefaultTableModel urgentModel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public AdminDashboard() {
        super("Smart Campus - Administrator Control Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        // 1. Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 780));
        sidebar.setBackground(new Color(24, 32, 54));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 15));

        JLabel logo = new JLabel("CAMPUS ADMIN CONSOLE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logo.setForeground(new Color(239, 68, 68));

        JLabel userName = new JLabel(user != null ? user.getFullName() : "Administrator");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        userName.setForeground(Color.WHITE);

        JLabel userEmail = new JLabel(user != null ? user.getEmail() : "");
        userEmail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userEmail.setForeground(new Color(148, 163, 184));

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(userName);
        sidebar.add(userEmail);
        sidebar.add(Box.createVerticalStrut(25));

        JButton btnNavOverview = createNavButton("Dashboard Overview", "OVERVIEW");
        JButton btnNavAllComplaints = createNavButton("All Complaints", "COMPLAINTS");
        JButton btnNavStaff = createNavButton("Staff Directory", "STAFF");
        JButton btnNavUsers = createNavButton("User Directory", "USERS");
        JButton btnNavReports = createNavButton("Analytics & CSV Export", "REPORTS");
        JButton btnNavAudit = createNavButton("Audit Trails", "AUDIT");
        JButton btnNavDb = createNavButton("Database Health", "DB");
        JButton btnNavLogout = createNavButton("Sign Out", "LOGOUT");

        sidebar.add(btnNavOverview);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavAllComplaints);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavStaff);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavUsers);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavReports);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavAudit);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavDb);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnNavLogout);

        add(sidebar, BorderLayout.WEST);

        // 2. Setup Overview Panel
        overviewPanel = new JPanel(new BorderLayout(15, 15));
        overviewPanel.setBackground(new Color(248, 250, 252));
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // 2 Rows of KPI Cards
        JPanel statsContainer = new JPanel(new GridLayout(2, 4, 12, 12));
        statsContainer.setOpaque(false);

        cardTotal = new StatCard("Total Complaints", "0", "All time entries", new Color(37, 99, 235));
        cardSubmitted = new StatCard("Submitted (New)", "0", "Awaiting review", new Color(99, 102, 241));
        cardAcknowledged = new StatCard("Acknowledged", "0", "Assigned / Verified", new Color(139, 92, 246));
        cardInProgress = new StatCard("In Progress", "0", "Under active repair", new Color(217, 119, 6));

        cardResolved = new StatCard("Resolved", "0", "Closed successfully", new Color(22, 163, 74));
        cardCritical = new StatCard("Critical Safety", "0", "Immediate attention", new Color(220, 38, 38));
        cardOverdue = new StatCard("Overdue (>72h)", "0", "SLA escalation required", new Color(190, 24, 93));
        cardAvgTime = new StatCard("Avg Resolution", "0h", "Average turnaround", new Color(13, 148, 136));

        statsContainer.add(cardTotal);
        statsContainer.add(cardSubmitted);
        statsContainer.add(cardAcknowledged);
        statsContainer.add(cardInProgress);
        statsContainer.add(cardResolved);
        statsContainer.add(cardCritical);
        statsContainer.add(cardOverdue);
        statsContainer.add(cardAvgTime);

        overviewPanel.add(statsContainer, BorderLayout.NORTH);

        // Urgent Issues Center Box
        JPanel urgentCard = new JPanel(new BorderLayout(10, 10));
        urgentCard.setBackground(Color.WHITE);
        urgentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));

        JPanel urgentHeader = new JPanel(new BorderLayout());
        urgentHeader.setOpaque(false);
        JLabel urgentTitle = new JLabel("High-Priority & Safety-Critical Action Queue");
        urgentTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        urgentTitle.setForeground(new Color(15, 23, 42));

        ModernButton btnGoToAll = new ModernButton("Open Full Management Console", ModernButton.ButtonType.PRIMARY);
        urgentHeader.add(urgentTitle, BorderLayout.WEST);
        urgentHeader.add(btnGoToAll, BorderLayout.EAST);
        urgentCard.add(urgentHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Building & Room", "Priority", "Score", "Status", "Student", "Assigned Staff", "Date"};
        urgentModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableUrgent = new JTable(urgentModel);
        tableUrgent.setRowHeight(32);
        tableUrgent.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableUrgent.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableUrgent.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableUrgent.getColumnModel().getColumn(1).setPreferredWidth(210);
        tableUrgent.getColumnModel().getColumn(2).setPreferredWidth(130);
        tableUrgent.getColumnModel().getColumn(3).setPreferredWidth(130);
        tableUrgent.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableUrgent.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableUrgent.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableUrgent.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableUrgent.getColumnModel().getColumn(8).setPreferredWidth(130);
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

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        urgentCard.add(progressBar, BorderLayout.SOUTH);

        overviewPanel.add(urgentCard, BorderLayout.CENTER);

        // Instantiate sub-panels
        allComplaintsPanel = new AllComplaintsPanel();
        staffPanel = new StaffManagementPanel();
        userPanel = new UserManagementPanel();
        reportsPanel = new ReportsPanel();
        auditPanel = new AuditLogsPanel();

        mainContentPanel.add(overviewPanel, "OVERVIEW");
        mainContentPanel.add(allComplaintsPanel, "COMPLAINTS");
        mainContentPanel.add(staffPanel, "STAFF");
        mainContentPanel.add(userPanel, "USERS");
        mainContentPanel.add(reportsPanel, "REPORTS");
        mainContentPanel.add(auditPanel, "AUDIT");

        add(mainContentPanel, BorderLayout.CENTER);

        // Listeners
        btnGoToAll.addActionListener(e -> {
            highlightNav(btnNavAllComplaints);
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            allComplaintsPanel.loadComplaints();
        });

        btnNavOverview.addActionListener(e -> {
            highlightNav(btnNavOverview);
            cardLayout.show(mainContentPanel, "OVERVIEW");
            loadDashboardData();
        });

        btnNavAllComplaints.addActionListener(e -> {
            highlightNav(btnNavAllComplaints);
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            allComplaintsPanel.loadComplaints();
        });

        btnNavStaff.addActionListener(e -> {
            highlightNav(btnNavStaff);
            cardLayout.show(mainContentPanel, "STAFF");
            staffPanel.loadStaffList();
        });

        btnNavUsers.addActionListener(e -> {
            highlightNav(btnNavUsers);
            cardLayout.show(mainContentPanel, "USERS");
            userPanel.loadUsers();
        });

        btnNavReports.addActionListener(e -> {
            highlightNav(btnNavReports);
            cardLayout.show(mainContentPanel, "REPORTS");
            reportsPanel.loadReportData();
        });

        btnNavAudit.addActionListener(e -> {
            highlightNav(btnNavAudit);
            cardLayout.show(mainContentPanel, "AUDIT");
            auditPanel.loadLogs();
        });

        btnNavDb.addActionListener(e -> new DatabaseConnectionStatusDialog(this).setVisible(true));
        btnNavLogout.addActionListener(e -> handleLogout());

        highlightNav(btnNavOverview);
        loadDashboardData();
    }

    private void loadDashboardData() {
        progressBar.setVisible(true);

        new SwingWorker<AdminOverviewData, Void>() {
            @Override
            protected AdminOverviewData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> urgent = complaintService.listAllComplaintsForAdmin(
                        null, null, null, null, null, null, "priority", false);
                return new AdminOverviewData(stats, urgent);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    AdminOverviewData data = get();
                    DashboardStats stats = data.stats;
                    cardTotal.setValue(stats.getTotalComplaints());
                    cardSubmitted.setValue(stats.getSubmittedComplaints());
                    cardAcknowledged.setValue(stats.getAcknowledgedComplaints());
                    cardInProgress.setValue(stats.getInProgressComplaints());
                    cardResolved.setValue(stats.getResolvedComplaints());
                    cardCritical.setValue(stats.getCriticalComplaints());
                    cardOverdue.setValue(stats.getOverdueComplaints());
                    cardAvgTime.setValue(String.format("%.1fh", stats.getAvgResolutionHours()));

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
                                c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "— Unassigned —",
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Failed to load administrative overview: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to sign out?",
                "Sign Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            UserSession.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private JButton createNavButton(String text, String actionCommand) {
        JButton btn = new JButton(text);
        btn.setActionCommand(actionCommand);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(new Color(24, 32, 54));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        btn.setMaximumSize(new Dimension(220, 40));

        navButtons.add(btn);
        return btn;
    }

    private void highlightNav(JButton activeBtn) {
        for (JButton b : navButtons) {
            if (b == activeBtn) {
                b.setBackground(new Color(37, 99, 235));
                b.setForeground(Color.WHITE);
            } else {
                b.setBackground(new Color(24, 32, 54));
                b.setForeground(new Color(203, 213, 225));
            }
        }
    }

    private record AdminOverviewData(DashboardStats stats, List<Complaint> urgentComplaints) {}
}
