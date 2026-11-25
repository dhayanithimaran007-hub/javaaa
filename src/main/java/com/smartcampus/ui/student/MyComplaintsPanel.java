package com.smartcampus.ui.student;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintCategory;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MyComplaintsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbCategory;
    private final JComboBox<String> cmbPriority;
    private final JComboBox<String> cmbStatus;
    private final JTable tableComplaints;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnViewDetails;
    private final ModernButton btnGiveFeedback;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<Complaint> currentList = new ArrayList<>();

    public MyComplaintsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topBar = new CardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        topBar.add(new JLabel("Search My Tickets:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 32));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Category:"));
        cmbCategory = new JComboBox<>();
        cmbCategory.addItem("ALL");
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            cmbCategory.addItem(cat.getDisplayName());
        }
        topBar.add(cmbCategory);

        topBar.add(new JLabel("Priority:"));
        cmbPriority = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        topBar.add(cmbPriority);

        topBar.add(new JLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"ALL", "SUBMITTED", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "REJECTED"});
        topBar.add(cmbStatus);

        btnRefresh = new ModernButton("Filter Tickets", ModernButton.ButtonType.PRIMARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        String[] columns = {"ID", "Title", "Category", "Building & Room", "Priority", "Score", "Status", "Date Submitted"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tableComplaints = new JTable(tableModel);
        tableComplaints.setRowHeight(34);
        tableComplaints.setFont(DesignSystem.FONT_SMALL);
        tableComplaints.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableComplaints.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableComplaints.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableComplaints.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableComplaints.getColumnModel().getColumn(1).setPreferredWidth(230);
        tableComplaints.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableComplaints.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableComplaints.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableComplaints.getColumnModel().getColumn(5).setPreferredWidth(55);
        tableComplaints.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableComplaints.getColumnModel().getColumn(7).setPreferredWidth(120);

        tableComplaints.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableComplaints.getSelectedRow() >= 0) {
                    openSelectedDetails();
                }
            }
        });

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableComplaints), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnGiveFeedback = new ModernButton("Rate Service & Give Feedback", ModernButton.ButtonType.SECONDARY);
        btnViewDetails = new ModernButton("View Full Progress & Audit", ModernButton.ButtonType.PRIMARY);

        bottomBar.add(btnGiveFeedback);
        bottomBar.add(btnViewDetails);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadComplaints());
        txtSearch.addActionListener(e -> loadComplaints());
        cmbCategory.addActionListener(e -> loadComplaints());
        cmbPriority.addActionListener(e -> loadComplaints());
        cmbStatus.addActionListener(e -> loadComplaints());

        btnViewDetails.addActionListener(e -> openSelectedDetails());
        btnGiveFeedback.addActionListener(e -> handleFeedback());

        loadComplaints();
    }

    public void loadComplaints() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String cat = (String) cmbCategory.getSelectedItem();
        String prio = (String) cmbPriority.getSelectedItem();
        String status = (String) cmbStatus.getSelectedItem();

        final String fSearch = search.isEmpty() ? null : search;
        final String fCat = "ALL".equalsIgnoreCase(cat) ? null : cat;
        final String fPrio = "ALL".equalsIgnoreCase(prio) ? null : prio;
        final String fStat = "ALL".equalsIgnoreCase(status) ? null : status;

        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintService.listComplaints(
                        fStat, fPrio, fCat, null, null, fSearch, "date", false);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<Complaint> list = get();
                    currentList.clear();
                    currentList.addAll(list);
                    tableModel.setRowCount(0);

                    for (Complaint c : list) {
                        tableModel.addRow(new Object[]{
                                c.getComplaintId(),
                                c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                                c.getCategory(),
                                c.getBuilding() + " - " + c.getRoomNumber(),
                                c.getPriority(),
                                c.getPriorityScore(),
                                c.getStatus(),
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MyComplaintsPanel.this,
                            "Failed to load my complaints: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSelectedDetails() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to inspect.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ComplaintDetailsDialog(parent, c.getComplaintId()).setVisible(true);
    }

    private void handleFeedback() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resolved complaint to give feedback.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() != ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Feedback can only be provided for RESOLVED complaints.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        ComplaintFeedbackDialog dlg = new ComplaintFeedbackDialog(parent, c.getComplaintId(), c.getTitle());
        dlg.setVisible(true);
        if (dlg.isFeedbackSubmitted()) {
            loadComplaints();
        }
    }
}
