package com.smartcampus.ui.staff;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ToastNotification;
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
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topBar = new CardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        topBar.add(new JLabel("Search My Orders:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 32));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"ALL", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED"});
        topBar.add(cmbStatus);

        btnRefresh = new ModernButton("Refresh Tasks", ModernButton.ButtonType.PRIMARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Building & Room", "Priority", "Score", "Status", "Student", "Assigned Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableAssigned = new JTable(tableModel);
        tableAssigned.setRowHeight(34);
        tableAssigned.setFont(DesignSystem.FONT_SMALL);
        tableAssigned.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableAssigned.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        tableAssigned.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());

        tableAssigned.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableAssigned.getColumnModel().getColumn(1).setPreferredWidth(220);
        tableAssigned.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableAssigned.getColumnModel().getColumn(3).setPreferredWidth(140);
        tableAssigned.getColumnModel().getColumn(4).setPreferredWidth(85);
        tableAssigned.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableAssigned.getColumnModel().getColumn(6).setPreferredWidth(95);
        tableAssigned.getColumnModel().getColumn(7).setPreferredWidth(120);
        tableAssigned.getColumnModel().getColumn(8).setPreferredWidth(120);

        tableAssigned.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableAssigned.getSelectedRow() >= 0) {
                    openSelectedDetails();
                }
            }
        });

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableAssigned), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnUpdateProgress = new ModernButton("Update Work Progress", ModernButton.ButtonType.SECONDARY);
        btnMarkResolved = new ModernButton("Complete & Resolve", ModernButton.ButtonType.SUCCESS);
        btnViewDetails = new ModernButton("View Full Work Order", ModernButton.ButtonType.PRIMARY);

        bottomBar.add(btnUpdateProgress);
        bottomBar.add(btnMarkResolved);
        bottomBar.add(btnViewDetails);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadAssignedComplaints());
        txtSearch.addActionListener(e -> loadAssignedComplaints());
        cmbStatus.addActionListener(e -> loadAssignedComplaints());

        btnViewDetails.addActionListener(e -> openSelectedDetails());
        btnUpdateProgress.addActionListener(e -> handleUpdateProgress());
        btnMarkResolved.addActionListener(e -> handleResolve());

        loadAssignedComplaints();
    }

    public void loadAssignedComplaints() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String status = (String) cmbStatus.getSelectedItem();

        final String fSearch = search.isEmpty() ? null : search;
        final String fStat = "ALL".equalsIgnoreCase(status) ? null : status;

        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintService.listComplaints(
                        fStat, null, null, null, null, fSearch, "priority", false);
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
                                c.getStudentName() != null ? c.getStudentName() : "Student #" + c.getStudentId(),
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

    private Complaint getSelectedComplaint() {
        int row = tableAssigned.getSelectedRow();
        if (row < 0) return null;
        return currentList.get(row);
    }

    private void openSelectedDetails() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select an assigned work order to inspect.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ComplaintDetailsDialog(parent, c.getComplaintId()).setVisible(true);
    }

    private void handleUpdateProgress() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a work order to update.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Cannot update progress on closed work order.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        WorkProgressDialog dlg = new WorkProgressDialog(parent, c.getComplaintId(), c.getTitle());
        dlg.setVisible(true);
        if (dlg.isProgressUpdated()) {
            ToastNotification.showSuccess(parent, "Work order #" + c.getComplaintId() + " progress updated.");
            loadAssignedComplaints();
        }
    }

    private void handleResolve() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a work order to resolve.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "This work order is already resolved.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        ResolutionDialog dlg = new ResolutionDialog(parent, c.getComplaintId(), c.getTitle());
        dlg.setVisible(true);
        if (dlg.isResolved()) {
            ToastNotification.showSuccess(parent, "Work order #" + c.getComplaintId() + " completed successfully.");
            loadAssignedComplaints();
        }
    }
}
