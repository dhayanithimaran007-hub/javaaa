package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintCategory;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ComplaintDetailsDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AllComplaintsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbCategory;
    private final JComboBox<String> cmbPriority;
    private final JComboBox<String> cmbStatus;
    private final JComboBox<String> cmbBuilding;
    private final JComboBox<String> cmbDateRange;
    private final JCheckBox chkSafetyCriticalOnly;
    private final JComboBox<String> cmbSort;

    private int currentPage = 1;
    private int pageSize = 15;
    private final JComboBox<Integer> cmbPageSize;
    private final JLabel lblPageStatus;
    private final ModernButton btnPrevPage;
    private final ModernButton btnNextPage;

    private final JTable tableComplaints;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;

    private final ModernButton btnViewDetails;
    private final ModernButton btnAssignStaff;
    private final ModernButton btnReject;
    private final ModernButton btnReopen;
    private final ModernButton btnExport;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final List<Complaint> fullList = new ArrayList<>();

    public AllComplaintsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel filterContainer = new CardPanel();
        filterContainer.setLayout(new BoxLayout(filterContainer, BoxLayout.Y_AXIS));
        filterContainer.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row1.setOpaque(false);

        row1.add(createFilterLabel("Search:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 30));
        row1.add(txtSearch);

        row1.add(createFilterLabel("Category:"));
        cmbCategory = new JComboBox<>();
        cmbCategory.addItem("ALL");
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            cmbCategory.addItem(cat.getDisplayName());
        }
        row1.add(cmbCategory);

        row1.add(createFilterLabel("Priority:"));
        cmbPriority = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        row1.add(cmbPriority);

        row1.add(createFilterLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"ALL", "SUBMITTED", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "REJECTED"});
        row1.add(cmbStatus);

        chkSafetyCriticalOnly = new JCheckBox("Safety Hazards Only");
        chkSafetyCriticalOnly.setFont(DesignSystem.FONT_SMALL_BOLD);
        chkSafetyCriticalOnly.setForeground(DesignSystem.CRITICAL);
        row1.add(chkSafetyCriticalOnly);

        filterContainer.add(row1);
        filterContainer.add(Box.createVerticalStrut(4));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row2.setOpaque(false);

        row2.add(createFilterLabel("Building:"));
        cmbBuilding = new JComboBox<>(new String[]{"ALL"});
        row2.add(cmbBuilding);

        row2.add(createFilterLabel("Timeframe:"));
        cmbDateRange = new JComboBox<>(new String[]{"ALL TIME", "Today", "Last 7 Days", "Last 30 Days"});
        row2.add(cmbDateRange);

        row2.add(createFilterLabel("Sort By:"));
        cmbSort = new JComboBox<>(new String[]{"Priority (Highest)", "Date (Newest)", "ID", "Status"});
        row2.add(cmbSort);

        btnRefresh = new ModernButton("Apply Filters", ModernButton.ButtonType.PRIMARY);
        btnRefresh.setPreferredSize(new Dimension(110, 30));
        row2.add(btnRefresh);

        filterContainer.add(row2);
        add(filterContainer, BorderLayout.NORTH);

        String[] columns = {"ID", "Title", "Category", "Location", "Priority", "Score", "Status", "Student", "Assigned Staff", "Submitted Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableComplaints = new JTable(tableModel);
        tableComplaints.setRowHeight(34);
        tableComplaints.setFont(DesignSystem.FONT_SMALL);
        tableComplaints.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

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

        JPanel paginationBar = new JPanel(new BorderLayout(10, 0));
        paginationBar.setBackground(Color.WHITE);
        paginationBar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JPanel leftPag = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPag.setOpaque(false);
        leftPag.add(new JLabel("Rows per page:"));
        cmbPageSize = new JComboBox<>(new Integer[]{10, 15, 25, 50});
        cmbPageSize.setSelectedItem(15);
        leftPag.add(cmbPageSize);
        paginationBar.add(leftPag, BorderLayout.WEST);

        lblPageStatus = new JLabel("Showing 0 - 0 of 0 complaints");
        lblPageStatus.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblPageStatus.setForeground(DesignSystem.TEXT_SECONDARY);
        lblPageStatus.setHorizontalAlignment(JLabel.CENTER);
        paginationBar.add(lblPageStatus, BorderLayout.CENTER);

        JPanel rightPag = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPag.setOpaque(false);
        btnPrevPage = new ModernButton("Prev", ModernButton.ButtonType.GHOST);
        btnPrevPage.setIcon(VectorIcons.chevronLeft(12));
        btnPrevPage.setIconTextGap(5);
        btnNextPage = new ModernButton("Next", ModernButton.ButtonType.GHOST);
        btnNextPage.setIcon(VectorIcons.chevronRight(12));
        btnNextPage.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnNextPage.setIconTextGap(5);
        rightPag.add(btnPrevPage);
        rightPag.add(btnNextPage);
        paginationBar.add(rightPag, BorderLayout.EAST);

        centerCard.add(paginationBar, BorderLayout.SOUTH);
        add(centerCard, BorderLayout.CENTER);

        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionToolbar.setOpaque(false);

        btnViewDetails = new ModernButton("View Full Details", ModernButton.ButtonType.PRIMARY);
        btnAssignStaff = new ModernButton("Assign / Dispatch Staff", ModernButton.ButtonType.SECONDARY);
        btnReject = new ModernButton("Reject Complaint", ModernButton.ButtonType.DANGER);
        btnReopen = new ModernButton("Reopen Complaint", ModernButton.ButtonType.SECONDARY);
        btnExport = new ModernButton("Export Filtered (CSV)", ModernButton.ButtonType.SECONDARY);

        actionToolbar.add(btnExport);
        actionToolbar.add(btnReopen);
        actionToolbar.add(btnReject);
        actionToolbar.add(btnAssignStaff);
        actionToolbar.add(btnViewDetails);
        add(actionToolbar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        txtSearch.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        cmbCategory.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        cmbPriority.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        cmbStatus.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        cmbBuilding.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        cmbSort.addActionListener(e -> { currentPage = 1; loadComplaints(); });
        chkSafetyCriticalOnly.addActionListener(e -> { currentPage = 1; loadComplaints(); });

        cmbPageSize.addActionListener(e -> {
            pageSize = (Integer) cmbPageSize.getSelectedItem();
            currentPage = 1;
            renderCurrentPage();
        });

        btnPrevPage.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });

        btnNextPage.addActionListener(e -> {
            int maxPage = (int) Math.ceil((double) fullList.size() / pageSize);
            if (currentPage < maxPage) {
                currentPage++;
                renderCurrentPage();
            }
        });

        btnViewDetails.addActionListener(e -> openSelectedDetails());
        btnAssignStaff.addActionListener(e -> openAssignDialog());
        btnReject.addActionListener(e -> handleReject());
        btnReopen.addActionListener(e -> handleReopen());
        btnExport.addActionListener(e -> handleExport());

        loadBuildings();
        loadComplaints();
    }

    private JLabel createFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(DesignSystem.FONT_SMALL_BOLD);
        l.setForeground(DesignSystem.TEXT_SECONDARY);
        return l;
    }

    private void loadBuildings() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return complaintService.getDistinctBuildings();
            }

            @Override
            protected void done() {
                try {
                    List<String> list = get();
                    for (String b : list) {
                        cmbBuilding.addItem(b);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void loadComplaints() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String cat = (String) cmbCategory.getSelectedItem();
        String prio = (String) cmbPriority.getSelectedItem();
        String status = (String) cmbStatus.getSelectedItem();
        String bldg = (String) cmbBuilding.getSelectedItem();
        Boolean safetyOnly = chkSafetyCriticalOnly.isSelected() ? true : null;

        String sortSelection = (String) cmbSort.getSelectedItem();
        String sortBy = "priority";
        boolean sortAsc = false;
        if ("Date (Newest)".equals(sortSelection)) sortBy = "date";
        else if ("ID".equals(sortSelection)) { sortBy = "id"; sortAsc = true; }
        else if ("Status".equals(sortSelection)) { sortBy = "status"; sortAsc = true; }

        final String fSearch = search.isEmpty() ? null : search;
        final String fCat = "ALL".equalsIgnoreCase(cat) ? null : cat;
        final String fPrio = "ALL".equalsIgnoreCase(prio) ? null : prio;
        final String fStat = "ALL".equalsIgnoreCase(status) ? null : status;
        final String fBldg = "ALL".equalsIgnoreCase(bldg) ? null : bldg;
        final String fSortBy = sortBy;
        final boolean fSortAsc = sortAsc;

        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintService.listAllComplaintsForAdmin(
                        fStat, fPrio, fCat, fBldg, safetyOnly, fSearch, fSortBy, fSortAsc);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<Complaint> res = get();
                    fullList.clear();
                    fullList.addAll(res);
                    currentPage = 1;
                    renderCurrentPage();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                            "Failed to load complaints: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderCurrentPage() {
        tableModel.setRowCount(0);
        int total = fullList.size();

        if (total == 0) {
            lblPageStatus.setText("Showing 0 - 0 of 0 complaints");
            btnPrevPage.setEnabled(false);
            btnNextPage.setEnabled(false);
            return;
        }

        int maxPage = (int) Math.ceil((double) total / pageSize);
        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int startIdx = (currentPage - 1) * pageSize;
        int endIdx = Math.min(startIdx + pageSize, total);

        for (int i = startIdx; i < endIdx; i++) {
            Complaint c = fullList.get(i);
            tableModel.addRow(new Object[]{
                    c.getComplaintId(),
                    c.getTitle() + (c.isSafetyCritical() ? " [SAFETY RISK]" : ""),
                    c.getCategory(),
                    c.getBuilding() + " - " + c.getRoomNumber(),
                    c.getPriority(),
                    c.getPriorityScore(),
                    c.getStatus(),
                    c.getStudentName() != null ? c.getStudentName() : "ID #" + c.getStudentId(),
                    c.getAssignedStaffName() != null ? c.getAssignedStaffName() : "Unassigned",
                    c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : ""
            });
        }

        lblPageStatus.setText(String.format("Showing %d - %d of %d complaints (Page %d of %d)",
                startIdx + 1, endIdx, total, currentPage, maxPage));
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < maxPage);
    }

    private Complaint getSelectedComplaint() {
        int row = tableComplaints.getSelectedRow();
        if (row < 0) return null;
        int compId = (Integer) tableModel.getValueAt(row, 0);
        for (Complaint c : fullList) {
            if (c.getComplaintId() == compId) return c;
        }
        return null;
    }

    private void openSelectedDetails() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to inspect.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ComplaintDetailsDialog(parent, c.getComplaintId()).setVisible(true);
    }

    private void openAssignDialog() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to assign.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Cannot assign a closed complaint.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        AssignStaffDialog dlg = new AssignStaffDialog(parent, c.getComplaintId(), c.getTitle(), c.getCategory());
        dlg.setVisible(true);
        if (dlg.isAssignedSuccessfully()) {
            ToastNotification.showSuccess(parent, "Staff dispatched for Complaint #" + c.getComplaintId());
            loadComplaints();
        }
    }

    private void handleReject() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to reject.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!c.getStatus().canTransitionTo(ComplaintStatus.REJECTED)) {
            JOptionPane.showMessageDialog(this, "Complaint in status " + c.getStatus() + " cannot be rejected.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this,
                "Enter mandatory administrative reason for rejection of Complaint #" + c.getComplaintId() + ":",
                "Reject Complaint", JOptionPane.QUESTION_MESSAGE);

        if (reason != null && !reason.trim().isEmpty()) {
            String trimmedReason = reason.trim();
            btnReject.setEnabled(false);
            progressBar.setVisible(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    complaintService.rejectComplaint(c.getComplaintId(), trimmedReason);
                    return null;
                }

                @Override
                protected void done() {
                    btnReject.setEnabled(true);
                    progressBar.setVisible(false);
                    try {
                        get();
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(AllComplaintsPanel.this);
                        ToastNotification.showSuccess(parent, "Complaint #" + c.getComplaintId() + " rejected.");
                        loadComplaints();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                                "Failed to reject complaint: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleReopen() {
        Complaint c = getSelectedComplaint();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to reopen.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "Only RESOLVED or REJECTED complaints can be reopened.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this,
                "Enter reason for reopening Complaint #" + c.getComplaintId() + ":",
                "Reopen Complaint Investigation", JOptionPane.QUESTION_MESSAGE);

        if (remarks != null && !remarks.trim().isEmpty()) {
            String trimmedRemarks = remarks.trim();
            btnReopen.setEnabled(false);
            progressBar.setVisible(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    complaintService.reopenComplaint(c.getComplaintId(), trimmedRemarks);
                    return null;
                }

                @Override
                protected void done() {
                    btnReopen.setEnabled(true);
                    progressBar.setVisible(false);
                    try {
                        get();
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(AllComplaintsPanel.this);
                        ToastNotification.showSuccess(parent, "Complaint #" + c.getComplaintId() + " reopened.");
                        loadComplaints();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                                "Failed to reopen complaint: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleExport() {
        if (fullList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No complaints matching current filters to export.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Filtered Complaints to CSV");
        chooser.setSelectedFile(new File("campus_complaints_export_" + System.currentTimeMillis() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Comma-Separated Values (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedTarget = chooser.getSelectedFile();
            if (!selectedTarget.getName().toLowerCase().endsWith(".csv")) {
                selectedTarget = new File(selectedTarget.getParentFile(), selectedTarget.getName() + ".csv");
            }
            final File target = selectedTarget;
            final List<Complaint> snapshot = new ArrayList<>(fullList);
            btnExport.setEnabled(false);
            progressBar.setVisible(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    complaintService.exportComplaintsToCsv(snapshot, target);
                    return null;
                }

                @Override
                protected void done() {
                    btnExport.setEnabled(true);
                    progressBar.setVisible(false);
                    try {
                        get();
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(AllComplaintsPanel.this);
                        ToastNotification.showSuccess(parent, "Exported " + snapshot.size() + " complaints to " + target.getName());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AllComplaintsPanel.this,
                                "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
