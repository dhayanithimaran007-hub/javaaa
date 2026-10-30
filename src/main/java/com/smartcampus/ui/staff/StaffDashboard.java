package com.smartcampus.ui.staff;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.LoginFrame;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.student.StudentProfilePanel;

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

public class StaffDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    private final List<JButton> navButtons = new ArrayList<>();

    // Panels
    private final JPanel overviewPanel;
    private final AssignedComplaintsPanel assignedPanel;
    private final StudentProfilePanel profilePanel;

    // Stat Cards
    private final StatCard cardAssigned;
    private final StatCard cardAck;
    private final StatCard cardInProgress;
    private final StatCard cardCompleted;
    private final StatCard cardUrgent;

    // Active tasks table
    private final JTable tableActive;
    private final DefaultTableModel activeModel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StaffDashboard() {
        super("Smart Campus - Maintenance Staff Workspace");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1160, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        // 1. Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 740));
        sidebar.setBackground(new Color(24, 32, 54));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 15));

        JLabel logo = new JLabel("CAMPUS MAINTENANCE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logo.setForeground(new Color(251, 146, 60));

        JLabel userName = new JLabel(user != null ? user.getFullName() : "Staff Member");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        userName.setForeground(Color.WHITE);

        JLabel userDept = new JLabel(user != null && user.getDepartment() != null ? user.getDepartment() : "Maintenance");
        userDept.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userDept.setForeground(new Color(148, 163, 184));

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(userName);
        sidebar.add(userDept);
        sidebar.add(Box.createVerticalStrut(25));

        JButton btnNavOverview = createNavButton("Overview / Tasks", "OVERVIEW");
        JButton btnNavAssigned = createNavButton("Assigned Complaints", "ASSIGNED");
        JButton btnNavProfile = createNavButton("Profile & Security", "PROFILE");
        JButton btnNavLogout = createNavButton("Sign Out", "LOGOUT");

        sidebar.add(btnNavOverview);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavAssigned);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavProfile);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnNavLogout);

        add(sidebar, BorderLayout.WEST);

        // 2. Setup Overview Panel
        overviewPanel = new JPanel(new BorderLayout(15, 15));
        overviewPanel.setBackground(new Color(248, 250, 252));
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // KPI Row
        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 12));
        statsRow.setOpaque(false);

        cardAssigned = new StatCard("Total Assigned", "0", "All active & closed", new Color(37, 99, 235));
        cardAck = new StatCard("Acknowledged", "0", "Awaiting start", new Color(139, 92, 246));
        cardInProgress = new StatCard("In Progress", "0", "Under active repair", new Color(217, 119, 6));
        cardCompleted = new StatCard("Completed", "0", "Successfully resolved", new Color(22, 163, 74));
        cardUrgent = new StatCard("Urgent / Safety", "0", "Immediate attention", new Color(220, 38, 38));

        statsRow.add(cardAssigned);
        statsRow.add(cardAck);
        statsRow.add(cardInProgress);
        statsRow.add(cardCompleted);
        statsRow.add(cardUrgent);

        overviewPanel.add(statsRow, BorderLayout.NORTH);

        // Active Tasks Center Card
        JPanel tasksCard = new JPanel(new BorderLayout(10, 10));
        tasksCard.setBackground(Color.WHITE);
        tasksCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));

        JPanel tasksHeader = new JPanel(new BorderLayout());
        tasksHeader.setOpaque(false);
        JLabel tasksTitle = new JLabel("My Active Work Orders");
        tasksTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tasksTitle.setForeground(new Color(15, 23, 42));

        ModernButton btnGoToAssigned = new ModernButton("Open Full Assigned View", ModernButton.ButtonType.PRIMARY);
        tasksHeader.add(tasksTitle, BorderLayout.WEST);
        tasksHeader.add(btnGoToAssigned, BorderLayout.EAST);
        tasksCard.add(tasksHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Student", "Assigned Date"};
        activeModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableActive = new JTable(activeModel);
        tableActive.setRowHeight(32);
        tableActive.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableActive.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableActive.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableActive.getColumnModel().getColumn(1).setPreferredWidth(220);
        tableActive.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableActive.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableActive.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableActive.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableActive.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableActive.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableActive.getColumnModel().getColumn(8).setPreferredWidth(120);

        tableActive.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableActive.getSelectedRow() >= 0) {
                    int id = (Integer) activeModel.getValueAt(tableActive.getSelectedRow(), 0);
                    new ComplaintDetailsDialog(StaffDashboard.this, id).setVisible(true);
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
        profilePanel = new StudentProfilePanel(); // Reusable profile panel for updating phone/dept/password

        mainContentPanel.add(overviewPanel, "OVERVIEW");
        mainContentPanel.add(assignedPanel, "ASSIGNED");
        mainContentPanel.add(profilePanel, "PROFILE");

        add(mainContentPanel, BorderLayout.CENTER);

        // Listeners
        btnGoToAssigned.addActionListener(e -> {
            highlightNav(btnNavAssigned);
            cardLayout.show(mainContentPanel, "ASSIGNED");
            assignedPanel.loadAssignedComplaints();
        });

        btnNavOverview.addActionListener(e -> {
            highlightNav(btnNavOverview);
            cardLayout.show(mainContentPanel, "OVERVIEW");
            loadDashboardData();
        });

        btnNavAssigned.addActionListener(e -> {
            highlightNav(btnNavAssigned);
            cardLayout.show(mainContentPanel, "ASSIGNED");
            assignedPanel.loadAssignedComplaints();
        });

        btnNavProfile.addActionListener(e -> {
            highlightNav(btnNavProfile);
            cardLayout.show(mainContentPanel, "PROFILE");
        });

        btnNavLogout.addActionListener(e -> handleLogout());

        highlightNav(btnNavOverview);
        loadDashboardData();
    }

    private void loadDashboardData() {
        progressBar.setVisible(true);

        new SwingWorker<StaffData, Void>() {
            @Override
            protected StaffData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> active = complaintService.listComplaints(
                        null, null, null, null, null, null, "priority", false);
                return new StaffData(stats, active);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    StaffData data = get();
                    DashboardStats stats = data.stats;
                    cardAssigned.setValue(stats.getTotalComplaints());
                    cardAck.setValue(stats.getAcknowledgedComplaints());
                    cardInProgress.setValue(stats.getInProgressComplaints());
                    cardCompleted.setValue(stats.getResolvedComplaints());
                    cardUrgent.setValue(stats.getCriticalComplaints());

                    activeModel.setRowCount(0);
                    int count = 0;
                    for (Complaint c : data.activeComplaints) {
                        if (count++ >= 10) break;
                        activeModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " - " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getStudentName() != null ? c.getStudentName() : "ID #" + c.getStudentId(),
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffDashboard.this,
                            "Failed to load staff dashboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        btn.setMaximumSize(new Dimension(210, 42));

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

    private record StaffData(DashboardStats stats, List<Complaint> activeComplaints) {}
}
