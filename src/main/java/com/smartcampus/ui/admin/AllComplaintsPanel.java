package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintCategory;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AllComplaintsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbCategory;
    private final JComboBox<String> cmbPriority;
    private final JComboBox<String> cmbStatus;
    private final JCheckBox chkSafetyCriticalOnly;
    private final JComboBox<String> cmbSort;

    private final JTable tableComplaints;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnViewDetails;
    private final ModernButton btnAssignStaff;
    private final ModernButton btnReject;
    private final ModernButton btnReopen;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<Complaint> currentList = new ArrayList<>();

    public AllComplaintsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Top Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        topBar.add(new JLabel("Search:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 30));
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

        chkSafetyCriticalOnly = new JCheckBox("Safety Hazards Only");
        topBar.add(chkSafetyCriticalOnly);

        topBar.add(new JLabel("Sort By:"));
        cmbSort = new JComboBox<>(new String[]{"Priority (Highest)", "Date (Newest)", "ID", "Status"});
        topBar.add(cmbSort);

        btnRefresh = new ModernButton("Apply Filters", ModernButton.ButtonType.SECONDARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Student", "Assigned Staff", "Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableComplaints = new JTable(tableModel);
        tableComplaints.setRowHeight(32);
        tableComplaints.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableComplaints.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tableComplaints.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableComplaints.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableComplaints.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableComplaints.getColumnModel().getColumn(1).setPreferredWidth(210);
        tableComplaints.getColumnModel().getColumn(2).setPreferredWidth(130);
        tableComplaints.getColumnModel().getColumn(3).setPreferredWidth(130);
        tableComplaints.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableComplaints.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableComplaints.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableComplaints.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableComplaints.getColumnModel().getColumn(8).setPreferredWidth(130);
        tableComplaints.getColumnModel().getColumn(9).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(tableComplaints);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JPanel centerPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnAssignStaff = new ModernButton("Assign to Staff", ModernButton.ButtonType.PRIMARY);
        btnReject = new ModernButton("Reject Complaint", ModernButton.ButtonType.DANGER);
        btnReopen = new ModernButton("Reopen Complaint", ModernButton.ButtonType.SECONDARY);
        btnViewDetails = new ModernButton("View Details & Audit Trail", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnReject);
        bottomBar.add(btnReopen);
        bottomBar.add(btnAssignStaff);
        bottomBar.add(btnViewDetails);
        add(bottomBar, BorderLayout.SOUTH);

        // Listeners
        btnRefresh.addActionListener(e -> loadComplaints());
        txtSearch.addActionListener(e -> loadComplaints());
        cmbCategory.addActionListener(e -> loadComplaints());
        cmbPriority.addActionListener(e -> loadComplaints());
        cmbStatus.addActionListener(e -> loadComplaints());
        chkSafetyCriticalOnly.addActionListener(e -> loadComplaints());
        cmbSort.addActionListener(e -> loadComplaints());

        tableComplaints.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableComplaints.getSelectedRow() >= 0) {
                    openSelectedComplaintDetails();
                }
            }
        });

        btnViewDetails.addActionListener(e -> openSelectedComplaintDetails());
        btnAssignStaff.addActionListener(e -> openAssignStaffDialog());
        btnReject.addActionListener(e -> handleReject());
        btnReopen.addActionListener(e -> handleReopen());

        loadComplaints();
    }

    public void loadComplaints() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String cat = (String) cmbCategory.getSelectedItem();
        String prio = (String) cmbPriority.getSelectedItem();
        String status = (String) cmbStatus.getSelectedItem();
        boolean safetyOnly = chkSafetyCriticalOnly.isSelected();

        String sortChoice = (String) cmbSort.getSelectedItem();
        String sortBy = "priority";
        boolean sortAsc = false;
        if (sortChoice != null) {
            if (sortChoice.startsWith("Date")) { sortBy = "date"; sortAsc = false; }
            else if (sortChoice.startsWith("ID")) { sortBy = "id"; sortAsc = false; }
            else if (sortChoice.startsWith("Status")) { sortBy = "status"; sortAsc = true; }
        }

        final String fSortBy = sortBy;
        final boolean fSortAsc = sortAsc;

        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintService.listAllComplaintsForAdmin(
                        status, prio, cat, null, safetyOnly, search, fSortBy, fSortAsc
                );
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
                                c.getStudentName() != null ? c.getStudentName() : "ID #" + c.getStudentId(),
                                c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "— Unassigned —",
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Failed to load complaints: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSelectedComplaintDetails() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to view.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int complaintId = (Integer) tableModel.getValueAt(row, 0);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ComplaintDetailsDialog(parent, complaintId).setVisible(true);
    }

    private void openAssignStaffDialog() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to assign.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Cannot assign a complaint that is already " + c.getStatus().getDisplayName() + ".",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        AssignStaffDialog dlg = new AssignStaffDialog(parent, c.getComplaintId(), c.getTitle());
        dlg.setVisible(true);
        if (dlg.isAssignedSuccessfully()) {
            loadComplaints();
        }
    }

    private void handleReject() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to reject.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Complaint is already " + c.getStatus().getDisplayName() + ".",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this,
                "Enter mandatory rejection reason for Complaint #" + c.getComplaintId() + ":",
                "Reject Complaint", JOptionPane.WARNING_MESSAGE);

        if (reason == null || reason.trim().isEmpty()) {
            return; // cancelled
        }

        progressBar.setVisible(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.rejectComplaint(c.getComplaintId(), reason.trim());
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Complaint #" + c.getComplaintId() + " rejected.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadComplaints();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Failed to reject complaint: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleReopen() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resolved/rejected complaint to reopen.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Only RESOLVED or REJECTED complaints can be reopened.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this,
                "Enter reason for reopening Complaint #" + c.getComplaintId() + ":",
                "Reopen Complaint", JOptionPane.QUESTION_MESSAGE);

        if (remarks == null || remarks.trim().isEmpty()) {
            return;
        }

        progressBar.setVisible(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.reopenComplaint(c.getComplaintId(), remarks.trim());
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Complaint #" + c.getComplaintId() + " reopened into IN_PROGRESS.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadComplaints();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Failed to reopen complaint: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public List<Complaint> getCurrentList() {
        return currentList;
    }
}
