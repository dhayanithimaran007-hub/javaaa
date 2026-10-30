package com.smartcampus.ui.student;

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

public class StudentDashboard extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContentPanel = new JPanel(cardLayout);

    // Nav buttons
    private final List<JButton> navButtons = new ArrayList<>();

    // Sub-panels
    private final JPanel overviewPanel;
    private final MyComplaintsPanel myComplaintsPanel;
    private final StudentProfilePanel profilePanel;

    // Stat Cards
    private final StatCard cardTotal;
    private final StatCard cardPending;
    private final StatCard cardInProgress;
    private final StatCard cardResolved;
    private final StatCard cardCritical;

    // Recent table
    private final JTable tableRecent;
    private final DefaultTableModel recentModel;
    private final JProgressBar progressBar;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudentDashboard() {
        super("Smart Campus - Student Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        User user = UserSession.getCurrentUser();

        // 1. Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 720));
        sidebar.setBackground(new Color(24, 32, 54));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 15));

        JLabel logo = new JLabel("CAMPUS STUDENT");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logo.setForeground(new Color(96, 165, 250));

        JLabel userName = new JLabel(user != null ? user.getFullName() : "Student");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        userName.setForeground(Color.WHITE);

        JLabel userEmail = new JLabel(user != null ? user.getEmail() : "");
        userEmail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userEmail.setForeground(new Color(148, 163, 184));

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(userName);
        sidebar.add(userEmail);
        sidebar.add(Box.createVerticalStrut(25));

        // Nav items
        JButton btnNavOverview = createNavButton("Overview / Dashboard", "OVERVIEW");
        JButton btnNavComplaints = createNavButton("My Complaints", "COMPLAINTS");
        JButton btnNavSubmit = createNavButton("+ Lodge Complaint", "SUBMIT");
        JButton btnNavProfile = createNavButton("Profile & Security", "PROFILE");
        JButton btnNavLogout = createNavButton("Sign Out", "LOGOUT");

        sidebar.add(btnNavOverview);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavComplaints);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSubmit);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavProfile);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnNavLogout);

        add(sidebar, BorderLayout.WEST);

        // 2. Setup Central Panels
        overviewPanel = new JPanel(new BorderLayout(15, 15));
        overviewPanel.setBackground(new Color(248, 250, 252));
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Overview Top: KPI Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 12));
        statsRow.setOpaque(false);

        cardTotal = new StatCard("Total Submitted", "0", "All time", new Color(37, 99, 235));
        cardPending = new StatCard("Pending Action", "0", "Submitted / Ack", new Color(147, 51, 234));
        cardInProgress = new StatCard("In Progress", "0", "Maintenance active", new Color(217, 119, 6));
        cardResolved = new StatCard("Resolved", "0", "Fixed & closed", new Color(22, 163, 74));
        cardCritical = new StatCard("Critical Safety", "0", "Immediate attention", new Color(220, 38, 38));

        statsRow.add(cardTotal);
        statsRow.add(cardPending);
        statsRow.add(cardInProgress);
        statsRow.add(cardResolved);
        statsRow.add(cardCritical);

        overviewPanel.add(statsRow, BorderLayout.NORTH);

        // Overview Center: Recent Complaints
        JPanel recentCard = new JPanel(new BorderLayout(10, 10));
        recentCard.setBackground(Color.WHITE);
        recentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(15, 18, 15, 18)
        ));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recently Updated Complaints");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        recentTitle.setForeground(new Color(15, 23, 42));

        ModernButton btnQuickSubmit = new ModernButton("+ Submit New Complaint", ModernButton.ButtonType.PRIMARY);
        btnQuickSubmit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(btnQuickSubmit, BorderLayout.EAST);
        recentCard.add(recentHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Date"};
        recentModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tableRecent = new JTable(recentModel);
        tableRecent.setRowHeight(30);
        tableRecent.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableRecent.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableRecent.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableRecent.getColumnModel().getColumn(1).setPreferredWidth(230);
        tableRecent.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableRecent.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableRecent.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableRecent.getColumnModel().getColumn(5).setPreferredWidth(55);
        tableRecent.getColumnModel().getColumn(6).setPreferredWidth(100);
        tableRecent.getColumnModel().getColumn(7).setPreferredWidth(120);

        tableRecent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableRecent.getSelectedRow() >= 0) {
                    int complaintId = (Integer) recentModel.getValueAt(tableRecent.getSelectedRow(), 0);
                    new ComplaintDetailsDialog(StudentDashboard.this, complaintId).setVisible(true);
                }
            }
        });

        JScrollPane recentScroll = new JScrollPane(tableRecent);
        recentScroll.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249)));
        recentCard.add(recentScroll, BorderLayout.CENTER);

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

        // Nav click actions
        btnQuickSubmit.addActionListener(e -> openSubmitComplaint());

        btnNavOverview.addActionListener(e -> {
            highlightNav(btnNavOverview);
            cardLayout.show(mainContentPanel, "OVERVIEW");
            loadDashboardData();
        });

        btnNavComplaints.addActionListener(e -> {
            highlightNav(btnNavComplaints);
            cardLayout.show(mainContentPanel, "COMPLAINTS");
            myComplaintsPanel.loadComplaints();
        });

        btnNavSubmit.addActionListener(e -> openSubmitComplaint());

        btnNavProfile.addActionListener(e -> {
            highlightNav(btnNavProfile);
            cardLayout.show(mainContentPanel, "PROFILE");
        });

        btnNavLogout.addActionListener(e -> handleLogout());

        highlightNav(btnNavOverview);
        loadDashboardData();
    }

    private void openSubmitComplaint() {
        SubmitComplaintDialog dlg = new SubmitComplaintDialog(this);
        dlg.setVisible(true);
        if (dlg.isSubmittedSuccessfully()) {
            loadDashboardData();
            myComplaintsPanel.loadComplaints();
        }
    }

    private void loadDashboardData() {
        progressBar.setVisible(true);

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                List<Complaint> recent = complaintService.listComplaints(
                        null, null, null, null, null, null, "date", false);
                return new DashboardData(stats, recent);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    DashboardData data = get();
                    DashboardStats stats = data.stats;
                    cardTotal.setValue(stats.getTotalComplaints());
                    cardPending.setValue(stats.getSubmittedComplaints() + stats.getAcknowledgedComplaints());
                    cardInProgress.setValue(stats.getInProgressComplaints());
                    cardResolved.setValue(stats.getResolvedComplaints());
                    cardCritical.setValue(stats.getCriticalComplaints());

                    recentModel.setRowCount(0);
                    int count = 0;
                    for (Complaint c : data.recentComplaints) {
                        if (count++ >= 8) break;
                        recentModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [CRITICAL SAFETY]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                            "Failed to load dashboard statistics: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private record DashboardData(DashboardStats stats, List<Complaint> recentComplaints) {}
}
