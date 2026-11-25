package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.StudentProfileDTO;
import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

public class StudentProfileDialog extends JDialog {
    private final int studentId;
    private final UserService userService = new UserService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final JPanel mainContainer;
    private final JProgressBar progressBar;

    public StudentProfileDialog(JFrame parent, int studentId) {
        super(parent, "Campus Student Profile & Ticket History - ID #" + studentId, true);
        this.studentId = studentId;
        setSize(840, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DesignSystem.MIDNIGHT_NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Student Intelligence Profile");
        title.setFont(DesignSystem.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Official Campus Student Record & Facilities Interaction History");
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        mainContainer = new JPanel(new BorderLayout(12, 12));
        mainContainer.setBackground(DesignSystem.BG_CANVAS);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        mainContainer.add(progressBar, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        ModernButton btnClose = new ModernButton("Close Profile", ModernButton.ButtonType.SECONDARY);
        btnClose.addActionListener(e -> dispose());
        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);

        loadProfile();
    }

    private void loadProfile() {
        progressBar.setVisible(true);

        new SwingWorker<StudentProfileDTO, Void>() {
            @Override
            protected StudentProfileDTO doInBackground() throws Exception {
                return userService.getStudentProfile(studentId);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    StudentProfileDTO data = get();
                    renderContent(data);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentProfileDialog.this,
                            "Failed to load student profile: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderContent(StudentProfileDTO data) {
        mainContainer.removeAll();
        User u = data.student();

        JPanel topGrid = new JPanel(new BorderLayout(14, 0));
        topGrid.setOpaque(false);

        CardPanel infoCard = new CardPanel(new GridBagLayout());
        infoCard.setPreferredSize(new Dimension(340, 120));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 8, 3, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblName = new JLabel(u.getFullName());
        lblName.setFont(DesignSystem.FONT_TITLE);
        lblName.setForeground(DesignSystem.TEXT_PRIMARY);
        infoCard.add(lblName, gbc);

        gbc.gridy = 1;
        JLabel lblEmail = new JLabel(u.getEmail());
        lblEmail.setFont(DesignSystem.FONT_SMALL);
        lblEmail.setForeground(DesignSystem.TEXT_SECONDARY);
        infoCard.add(lblEmail, gbc);

        gbc.gridy = 2;
        JLabel lblDept = new JLabel("Department: " + (u.getDepartment() != null ? u.getDepartment() : "General"));
        lblDept.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblDept.setForeground(DesignSystem.ELECTRIC_BLUE);
        infoCard.add(lblDept, gbc);

        gbc.gridy = 3;
        JLabel lblPhone = new JLabel("Phone: " + (u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "Not provided"));
        lblPhone.setFont(DesignSystem.FONT_TINY);
        lblPhone.setForeground(DesignSystem.TEXT_MUTED);
        infoCard.add(lblPhone, gbc);

        topGrid.add(infoCard, BorderLayout.WEST);

        JPanel metricsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        metricsPanel.setOpaque(false);

        StatCard cTotal = new StatCard("Total Filed", String.valueOf(data.totalComplaints()), "All time tickets", DesignSystem.ELECTRIC_BLUE);
        StatCard cResolved = new StatCard("Resolved", String.valueOf(data.resolvedComplaints()), "Closed tickets", DesignSystem.SUCCESS);
        String ratingStr = (data.averageRatingGiven() > 0) ? String.format("%.1f / 5.0", data.averageRatingGiven()) : "N/A";
        StatCard cRating = new StatCard("Avg Rating Given", ratingStr, "Satisfaction score", DesignSystem.WARNING);

        metricsPanel.add(cTotal);
        metricsPanel.add(cResolved);
        metricsPanel.add(cRating);
        topGrid.add(metricsPanel, BorderLayout.CENTER);

        mainContainer.add(topGrid, BorderLayout.NORTH);

        CardPanel tableCard = new CardPanel(new BorderLayout(8, 8));
        tableCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel historyTitle = new JLabel("Complaint History Log (" + data.complaints().size() + " records)");
        historyTitle.setFont(DesignSystem.FONT_SUBTITLE);
        historyTitle.setForeground(DesignSystem.TEXT_PRIMARY);
        tableCard.add(historyTitle, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Assigned Staff", "Submitted Date"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Complaint c : data.complaints()) {
            model.addRow(new Object[]{
                    c.getComplaintId(),
                    c.getTitle(),
                    c.getCategory(),
                    c.getBuilding() + " - " + c.getRoomNumber(),
                    c.getPriority(),
                    c.getPriorityScore(),
                    c.getStatus(),
                    c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Unassigned",
                    c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(DesignSystem.FONT_SMALL);
        table.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        table.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(50);
        table.getColumnModel().getColumn(6).setPreferredWidth(95);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(110);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int cId = (Integer) model.getValueAt(table.getSelectedRow(), 0);
                    new ComplaintDetailsDialog((JFrame) getParent(), cId).setVisible(true);
                }
            }
        });

        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        mainContainer.add(tableCard, BorderLayout.CENTER);

        mainContainer.revalidate();
        mainContainer.repaint();
    }
}
