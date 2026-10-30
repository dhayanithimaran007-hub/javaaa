package com.smartcampus.ui.staff;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;

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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AssignedComplaintsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbStatus;
    private final JTable tableAssigned;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnViewDetails;
    private final ModernButton btnUpdateProgress;
    private final ModernButton btnMarkResolved;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<Complaint> currentList = new ArrayList<>();

    public AssignedComplaintsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        topBar.add(new JLabel("Search Assigned:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 30));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"ALL", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED"});
        topBar.add(cmbStatus);

        btnRefresh = new ModernButton("Refresh Tasks", ModernButton.ButtonType.SECONDARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"ID", "Title", "Category", "Building & Room", "Priority", "Score", "Status", "Student", "Assigned Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableAssigned = new JTable(tableModel);
        tableAssigned.setRowHeight(32);
        tableAssigned.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAssigned.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tableAssigned.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableAssigned.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableAssigned.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableAssigned.getColumnModel().getColumn(1).setPreferredWidth(220);
        tableAssigned.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableAssigned.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableAssigned.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableAssigned.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableAssigned.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableAssigned.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableAssigned.getColumnModel().getColumn(8).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(tableAssigned);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JPanel centerPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnUpdateProgress = new ModernButton("Update Work Progress", ModernButton.ButtonType.PRIMARY);
        btnMarkResolved = new ModernButton("Mark Work Resolved", ModernButton.ButtonType.SUCCESS);
        btnViewDetails = new ModernButton("View Full Details", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnViewDetails);
        bottomBar.add(btnUpdateProgress);
        bottomBar.add(btnMarkResolved);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadAssignedComplaints());
        txtSearch.addActionListener(e -> loadAssignedComplaints());
        cmbStatus.addActionListener(e -> loadAssignedComplaints());

        tableAssigned.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableAssigned.getSelectedRow() >= 0) {
                    openSelectedComplaintDetails();
                }
            }
        });

        btnViewDetails.addActionListener(e -> openSelectedComplaintDetails());
        btnUpdateProgress.addActionListener(e -> openUpdateProgressDialog());
        btnMarkResolved.addActionListener(e -> openResolveDialog());

        loadAssignedComplaints();
    }

    public void loadAssignedComplaints() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String status = (String) cmbStatus.getSelectedItem();

        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintService.listComplaints(
                        status, null, null, null, null, search, "priority", false
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
                                c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignedComplaintsPanel.this,
                            "Failed to load assigned complaints: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSelectedComplaintDetails() {
        int row = tableAssigned.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an assigned task to view.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int complaintId = (Integer) tableModel.getValueAt(row, 0);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ComplaintDetailsDialog(parent, complaintId).setVisible(true);
    }

    private void openUpdateProgressDialog() {
        int row = tableAssigned.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to update progress.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Cannot update progress on an already " + c.getStatus().getDisplayName() + " complaint.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        WorkProgressDialog dlg = new WorkProgressDialog(parent, c.getComplaintId(), c.getTitle(), c.getStatus());
        dlg.setVisible(true);
        if (dlg.isProgressUpdated()) {
            loadAssignedComplaints();
        }
    }

    private void openResolveDialog() {
        int row = tableAssigned.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to resolve.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Complaint c = currentList.get(row);
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Complaint is already resolved.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        ResolutionDialog dlg = new ResolutionDialog(parent, c.getComplaintId(), c.getTitle());
        dlg.setVisible(true);
        if (dlg.isResolvedSuccessfully()) {
            loadAssignedComplaints();
        }
    }
}
