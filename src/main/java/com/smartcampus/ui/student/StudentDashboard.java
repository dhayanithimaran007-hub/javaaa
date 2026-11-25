package com.smartcampus.ui.student;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.LoginFrame;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ModernSidebar;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

public class StudentDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    private final ModernSidebar sidebar;

    private final JPanel overviewPanel;
    private final MyComplaintsPanel myComplaintsPanel;
    private final StudentProfilePanel profilePanel;

    private final StatCard cardTotal;
    private final StatCard cardPending;
    private final StatCard cardInProgress;
    private final StatCard cardResolved;
    private final StatCard cardCritical;

    private final JTable tableRecent;
    private final DefaultTableModel recentModel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudentDashboard() {
        super("Smart Campus - Student Workspace & Facilities Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        sidebar = new ModernSidebar("Smart Campus", "Student Portal");
        sidebar.addNavItem("OVERVIEW", VectorIcons.of(VectorIcons.Kind.DASHBOARD, 16), "My Dashboard");
        sidebar.addNavItem("COMPLAINTS", VectorIcons.of(VectorIcons.Kind.CLIPBOARD, 16), "My Complaints");
        sidebar.addNavItem("SUBMIT", VectorIcons.of(VectorIcons.Kind.PLUS, 16), "Lodge Complaint");
        sidebar.addNavItem("PROFILE", VectorIcons.of(VectorIcons.Kind.USER, 16), "Profile & Security");

        sidebar.setNavSelectedListener(this::handleNavigation);
        sidebar.setLogoutListener(this::handleLogout);
        add(sidebar, BorderLayout.WEST);

        overviewPanel = new JPanel(new BorderLayout(14, 14));
        overviewPanel.setBackground(DesignSystem.BG_CANVAS);
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 12));
        statsRow.setOpaque(false);

        cardTotal = new StatCard("Total Submitted", "0", "All time requests", DesignSystem.ELECTRIC_BLUE);
        cardPending = new StatCard("Pending Review", "0", "Awaiting dispatch", DesignSystem.VIOLET);
        cardInProgress = new StatCard("In Progress", "0", "Active technician work", DesignSystem.WARNING);
        cardResolved = new StatCard("Resolved", "0", "Closed successfully", DesignSystem.SUCCESS);
        cardCritical = new StatCard("Safety Critical", "0", "Emergency attention", DesignSystem.CRITICAL);

        statsRow.add(cardTotal);
        statsRow.add(cardPending);
        statsRow.add(cardInProgress);
        statsRow.add(cardResolved);
        statsRow.add(cardCritical);
        overviewPanel.add(statsRow, BorderLayout.NORTH);

        CardPanel recentCard = new CardPanel(new BorderLayout(10, 10));
        recentCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("My Recently Updated Facilities Complaints");
        recentTitle.setFont(DesignSystem.FONT_SUBTITLE);
        recentTitle.setForeground(DesignSystem.TEXT_PRIMARY);

        ModernButton btnQuickSubmit = new ModernButton("+ Lodge New Complaint", ModernButton.ButtonType.PRIMARY);
        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(btnQuickSubmit, BorderLayout.EAST);
        recentCard.add(recentHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Assigned Technician", "Submitted Date"};
        recentModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableRecent = new JTable(recentModel);
        tableRecent.setRowHeight(34);
        tableRecent.setFont(DesignSystem.FONT_SMALL);
        tableRecent.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableRecent.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableRecent.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableRecent.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableRecent.getColumnModel().getColumn(1).setPreferredWidth(210);
        tableRecent.getColumnModel().getColumn(2).setPreferredWidth(130);
        tableRecent.getColumnModel().getColumn(3).setPreferredWidth(130);
        tableRecent.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableRecent.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableRecent.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableRecent.getColumnModel().getColumn(7).setPreferredWidth(130);
        tableRecent.getColumnModel().getColumn(8).setPreferredWidth(110);

        tableRecent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableRecent.getSelectedRow() >= 0) {
                    int cId = (Integer) recentModel.getValueAt(tableRecent.getSelectedRow(), 0);
                    new ComplaintDetailsDialog(StudentDashboard.this, cId).setVisible(true);
                }
            }
        });

        recentCard.add(new JScrollPane(tableRecent), BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        recentCard.add(progressBar, BorderLayout.SOUTH);

        overviewPanel.add(recentCard, BorderLayout.CENTER);

        myComplaintsPanel = new MyComplaintsPanel();
        profilePanel = new StudentProfilePanel();

        mainContentPanel.add(overviewPanel, "OVERVIEW");
        mainContentPanel.add(myComplaintsPanel, "COMPLAINTS");
        mainContentPanel.add(profilePanel, "PROFILE");

        add(mainContentPanel, BorderLayout.CENTER);

        btnQuickSubmit.addActionListener(e -> openSubmitDialog());

        loadOverviewData();
    }

    private void handleNavigation(String navId) {
        if ("SUBMIT".equals(navId)) {
            openSubmitDialog();
            return;
        }

        cardLayout.show(mainContentPanel, navId);

        switch (navId) {
            case "OVERVIEW" -> loadOverviewData();
            case "COMPLAINTS" -> myComplaintsPanel.loadComplaints();
            case "PROFILE" -> profilePanel.loadProfile();
        }
    }

    private void openSubmitDialog() {
        SubmitComplaintDialog dlg = new SubmitComplaintDialog(this);
        dlg.setVisible(true);
        if (dlg.isSubmitted()) {
            ToastNotification.showSuccess(this, "Complaint submitted successfully.");
            loadOverviewData();
            sidebar.setActiveItem("COMPLAINTS");
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            myComplaintsPanel.loadComplaints();
        } else {
            sidebar.setActiveItem("OVERVIEW");
        }
    }

    private void loadOverviewData() {
        progressBar.setVisible(true);

        new SwingWorker<StudentOverviewData, Void>() {
            @Override
            protected StudentOverviewData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> list = complaintService.listComplaints(
                        null, null, null, null, null, null, "date", false);
                return new StudentOverviewData(stats, list);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    StudentOverviewData data = get();
                    DashboardStats s = data.stats;

                    cardTotal.setValue(s.getTotalComplaints());
                    cardPending.setValue(s.getSubmittedComplaints() + s.getAcknowledgedComplaints());
                    cardInProgress.setValue(s.getInProgressComplaints());
                    cardResolved.setValue(s.getResolvedComplaints());
                    cardCritical.setValue(s.getCriticalComplaints());

                    recentModel.setRowCount(0);
                    int count = 0;
                    for (Complaint c : data.complaints) {
                        if (count++ >= 8) break;
                        recentModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " - " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Awaiting Dispatch",
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                            "Failed to load student dashboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private record StudentOverviewData(DashboardStats stats, List<Complaint> complaints) {}
}
