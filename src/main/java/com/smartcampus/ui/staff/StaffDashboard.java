package com.smartcampus.ui.staff;

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
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.student.StudentProfilePanel;
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

public class StaffDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    private final ModernSidebar sidebar;

    private final JPanel overviewPanel;
    private final AssignedComplaintsPanel assignedPanel;
    private final StudentProfilePanel profilePanel;

    private final StatCard cardAssigned;
    private final StatCard cardAck;
    private final StatCard cardInProgress;
    private final StatCard cardCompleted;
    private final StatCard cardUrgent;

    private final JTable tableActive;
    private final DefaultTableModel activeModel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StaffDashboard() {
        super("Smart Campus - Maintenance Staff Workspace & Field Dispatch");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        sidebar = new ModernSidebar("Smart Campus", "Field Maintenance");
        sidebar.addNavItem("OVERVIEW", VectorIcons.of(VectorIcons.Kind.DASHBOARD, 16), "Tasks Overview");
        sidebar.addNavItem("ASSIGNED", VectorIcons.of(VectorIcons.Kind.WRENCH, 16), "Work Orders");
        sidebar.addNavItem("PROFILE", VectorIcons.of(VectorIcons.Kind.USER, 16), "Profile & Security");

        sidebar.setNavSelectedListener(this::handleNavigation);
        sidebar.setLogoutListener(this::handleLogout);
        add(sidebar, BorderLayout.WEST);

        overviewPanel = new JPanel(new BorderLayout(14, 14));
        overviewPanel.setBackground(DesignSystem.BG_CANVAS);
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 12));
        statsRow.setOpaque(false);

        cardAssigned = new StatCard("Total Assigned", "0", "All time tickets", DesignSystem.ELECTRIC_BLUE);
        cardAck = new StatCard("Acknowledged", "0", "Awaiting site inspection", DesignSystem.VIOLET);
        cardInProgress = new StatCard("In Progress", "0", "Under active repair", DesignSystem.WARNING);
        cardCompleted = new StatCard("Resolved", "0", "Successfully closed", DesignSystem.SUCCESS);
        cardUrgent = new StatCard("Urgent / Safety", "0", "Immediate attention", DesignSystem.CRITICAL);

        statsRow.add(cardAssigned);
        statsRow.add(cardAck);
        statsRow.add(cardInProgress);
        statsRow.add(cardCompleted);
        statsRow.add(cardUrgent);
        overviewPanel.add(statsRow, BorderLayout.NORTH);

        CardPanel tasksCard = new CardPanel(new BorderLayout(10, 10));
        tasksCard.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel tasksHeader = new JPanel(new BorderLayout());
        tasksHeader.setOpaque(false);
        JLabel tasksTitle = new JLabel("My Active Campus Work Orders & Action Queue");
        tasksTitle.setFont(DesignSystem.FONT_SUBTITLE);
        tasksTitle.setForeground(DesignSystem.TEXT_PRIMARY);

        ModernButton btnGoToAssigned = new ModernButton("Open Full Dispatch View", ModernButton.ButtonType.PRIMARY);
        tasksHeader.add(tasksTitle, BorderLayout.WEST);
        tasksHeader.add(btnGoToAssigned, BorderLayout.EAST);
        tasksCard.add(tasksHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Student", "Reported Time"};
        activeModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableActive = new JTable(activeModel);
        tableActive.setRowHeight(34);
        tableActive.setFont(DesignSystem.FONT_SMALL);
        tableActive.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableActive.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableActive.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableActive.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableActive.getColumnModel().getColumn(1).setPreferredWidth(210);
        tableActive.getColumnModel().getColumn(2).setPreferredWidth(130);
        tableActive.getColumnModel().getColumn(3).setPreferredWidth(130);
        tableActive.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableActive.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableActive.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableActive.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableActive.getColumnModel().getColumn(8).setPreferredWidth(110);

        tableActive.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableActive.getSelectedRow() >= 0) {
                    int cId = (Integer) activeModel.getValueAt(tableActive.getSelectedRow(), 0);
                    new ComplaintDetailsDialog(StaffDashboard.this, cId).setVisible(true);
                }
            }
        });

        tasksCard.add(new JScrollPane(tableActive), BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        tasksCard.add(progressBar, BorderLayout.SOUTH);

        overviewPanel.add(tasksCard, BorderLayout.CENTER);

        assignedPanel = new AssignedComplaintsPanel();
        profilePanel = new StudentProfilePanel();

        mainContentPanel.add(overviewPanel, "OVERVIEW");
        mainContentPanel.add(assignedPanel, "ASSIGNED");
        mainContentPanel.add(profilePanel, "PROFILE");

        add(mainContentPanel, BorderLayout.CENTER);

        btnGoToAssigned.addActionListener(e -> {
            sidebar.setActiveItem("ASSIGNED");
            cardLayout.show(mainContentPanel, "ASSIGNED");
            assignedPanel.loadAssignedComplaints();
        });

        loadOverviewData();
    }

    private void handleNavigation(String navId) {
        cardLayout.show(mainContentPanel, navId);

        switch (navId) {
            case "OVERVIEW" -> loadOverviewData();
            case "ASSIGNED" -> assignedPanel.loadAssignedComplaints();
            case "PROFILE" -> profilePanel.loadProfile();
        }
    }

    private void loadOverviewData() {
        progressBar.setVisible(true);

        new SwingWorker<StaffOverviewData, Void>() {
            @Override
            protected StaffOverviewData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> list = complaintService.listComplaints(
                        null, null, null, null, null, null, "priority", false);
                return new StaffOverviewData(stats, list);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    StaffOverviewData data = get();
                    DashboardStats s = data.stats;

                    cardAssigned.setValue(s.getTotalComplaints());
                    cardAck.setValue(s.getAcknowledgedComplaints());
                    cardInProgress.setValue(s.getInProgressComplaints());
                    cardCompleted.setValue(s.getResolvedComplaints());
                    cardUrgent.setValue(s.getCriticalComplaints());

                    activeModel.setRowCount(0);
                    int count = 0;
                    for (Complaint c : data.activeComplaints) {
                        if (c.getStatus() == com.smartcampus.model.enums.ComplaintStatus.RESOLVED) continue;
                        if (count++ >= 10) break;
                        activeModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " - " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getStudentName() != null ? c.getStudentName() : "Student #" + c.getStudentId(),
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffDashboard.this,
                            "Failed to load technician dashboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private record StaffOverviewData(DashboardStats stats, List<Complaint> activeComplaints) {}
}
