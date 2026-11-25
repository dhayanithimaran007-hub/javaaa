package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.StaffPerformanceDTO;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.charts.DonutChartPanel;
import com.smartcampus.ui.charts.HorizontalBarChartPanel;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends JPanel {
    private final StatCard cardTotal;
    private final StatCard cardResolved;
    private final StatCard cardRate;
    private final StatCard cardAvgTime;
    private final StatCard cardOverdue;

    private final DonutChartPanel donutCategory;
    private final HorizontalBarChartPanel barDept;

    private final JTable tableStaffScorecard;
    private final DefaultTableModel staffModel;

    private final JProgressBar progressBar;
    private final ModernButton btnPrintableReport;
    private final ModernButton btnExportCsv;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();

    private DashboardStats currentStats = new DashboardStats();
    private Map<String, Integer> currentDeptMap = Map.of();
    private List<StaffPerformanceDTO> currentStaffMetrics = new ArrayList<>();

    public ReportsPanel() {
        setLayout(new BorderLayout(14, 14));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel topKpi = new JPanel(new GridLayout(1, 5, 12, 12));
        topKpi.setOpaque(false);

        cardTotal = new StatCard("Total Logged", "0", "All-time campus entries", DesignSystem.ELECTRIC_BLUE);
        cardResolved = new StatCard("Resolved", "0", "Successfully closed", DesignSystem.SUCCESS);
        cardRate = new StatCard("Resolution Rate", "0%", "Closed / Total", DesignSystem.CYAN);
        cardAvgTime = new StatCard("Avg Turnaround", "0h", "Resolution turnaround", DesignSystem.WARNING);
        cardOverdue = new StatCard("Overdue Issues", "0", "Priority SLA breached", DesignSystem.CRITICAL);

        topKpi.add(cardTotal);
        topKpi.add(cardResolved);
        topKpi.add(cardRate);
        topKpi.add(cardAvgTime);
        topKpi.add(cardOverdue);
        add(topKpi, BorderLayout.NORTH);

        JPanel centerGrid = new JPanel(new GridLayout(2, 1, 14, 14));
        centerGrid.setOpaque(false);

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsRow.setOpaque(false);

        CardPanel pnlDonut = new CardPanel(new BorderLayout(6, 6));
        pnlDonut.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel lblDonut = new JLabel("Complaint Distribution by Category");
        lblDonut.setFont(DesignSystem.FONT_SUBTITLE);
        lblDonut.setForeground(DesignSystem.TEXT_PRIMARY);
        pnlDonut.add(lblDonut, BorderLayout.NORTH);
        donutCategory = new DonutChartPanel();
        donutCategory.setCenterText("Total", "0");
        pnlDonut.add(donutCategory, BorderLayout.CENTER);
        chartsRow.add(pnlDonut);

        CardPanel pnlBar = new CardPanel(new BorderLayout(6, 6));
        pnlBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel lblBar = new JLabel("Incidents by Academic Department");
        lblBar.setFont(DesignSystem.FONT_SUBTITLE);
        lblBar.setForeground(DesignSystem.TEXT_PRIMARY);
        pnlBar.add(lblBar, BorderLayout.NORTH);
        barDept = new HorizontalBarChartPanel();
        pnlBar.add(barDept, BorderLayout.CENTER);
        chartsRow.add(pnlBar);

        centerGrid.add(chartsRow);

        CardPanel scorecardCard = new CardPanel(new BorderLayout(8, 8));
        scorecardCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblScorecard = new JLabel("Maintenance Staff Productivity Scorecard");
        lblScorecard.setFont(DesignSystem.FONT_SUBTITLE);
        lblScorecard.setForeground(DesignSystem.TEXT_PRIMARY);
        scorecardCard.add(lblScorecard, BorderLayout.NORTH);

        String[] cols = {"Staff ID", "Technician Name", "Specialization", "Tasks Assigned", "Tasks Resolved", "Resolution Rate", "Avg Turnaround", "Student Rating", "Active Load"};
        staffModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableStaffScorecard = new JTable(staffModel);
        tableStaffScorecard.setRowHeight(32);
        tableStaffScorecard.setFont(DesignSystem.FONT_SMALL);
        tableStaffScorecard.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableStaffScorecard.getColumnModel().getColumn(0).setPreferredWidth(55);
        tableStaffScorecard.getColumnModel().getColumn(1).setPreferredWidth(170);
        tableStaffScorecard.getColumnModel().getColumn(2).setPreferredWidth(160);
        tableStaffScorecard.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableStaffScorecard.getColumnModel().getColumn(4).setPreferredWidth(100);
        tableStaffScorecard.getColumnModel().getColumn(5).setPreferredWidth(110);
        tableStaffScorecard.getColumnModel().getColumn(6).setPreferredWidth(110);
        tableStaffScorecard.getColumnModel().getColumn(7).setPreferredWidth(100);
        tableStaffScorecard.getColumnModel().getColumn(8).setPreferredWidth(95);

        scorecardCard.add(new JScrollPane(tableStaffScorecard), BorderLayout.CENTER);
        centerGrid.add(scorecardCard);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerWrap.add(progressBar, BorderLayout.NORTH);
        centerWrap.add(centerGrid, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnPrintableReport = new ModernButton("Printable Executive Report", ModernButton.ButtonType.PRIMARY);
        btnExportCsv = new ModernButton("Export Authorized CSV", ModernButton.ButtonType.SECONDARY);
        btnRefresh = new ModernButton("Refresh Analytics", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnRefresh);
        bottomBar.add(btnExportCsv);
        bottomBar.add(btnPrintableReport);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadReportData());
        btnExportCsv.addActionListener(e -> handleExportCsv());
        btnPrintableReport.addActionListener(e -> openPrintableReport());

        loadReportData();
    }

    public void loadReportData() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        new SwingWorker<ReportPayload, Void>() {
            @Override
            protected ReportPayload doInBackground() throws Exception {
                DashboardStats stats = complaintService.getDashboardStats();
                Map<String, Integer> deptMap = complaintService.getComplaintsByDepartment();
                List<StaffPerformanceDTO> metrics = complaintService.getStaffPerformanceMetrics();
                return new ReportPayload(stats, deptMap, metrics);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    ReportPayload data = get();
                    currentStats = data.stats;
                    currentDeptMap = data.deptMap;
                    currentStaffMetrics = data.staffMetrics;

                    cardTotal.setValue(currentStats.getTotalComplaints());
                    cardResolved.setValue(currentStats.getResolvedComplaints());
                    double rate = currentStats.getTotalComplaints() > 0
                            ? ((double) currentStats.getResolvedComplaints() / currentStats.getTotalComplaints()) * 100.0 : 0.0;
                    cardRate.setValue(String.format("%.1f%%", rate));
                    cardAvgTime.setValue(String.format("%.1fh", currentStats.getAvgResolutionHours()));
                    cardOverdue.setValue(currentStats.getOverdueComplaints());

                    donutCategory.setData(currentStats.getComplaintsByCategory());
                    donutCategory.setCenterText("Total", String.valueOf(currentStats.getTotalComplaints()));

                    barDept.setData(currentDeptMap);

                    staffModel.setRowCount(0);
                    for (StaffPerformanceDTO s : currentStaffMetrics) {
                        String rating = (s.avgRating() > 0) ? String.format("%.1f / 5.0", s.avgRating()) : "N/A";
                        staffModel.addRow(new Object[]{
                                s.staffId(),
                                s.fullName(),
                                s.department() != null ? s.department() : "Facilities",
                                s.assignedCount(),
                                s.resolvedCount(),
                                String.format("%.1f%%", s.getResolutionRate()),
                                String.format("%.1fh", s.avgResolutionHours()),
                                rating,
                                s.activeTasks() + " active"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Failed to load analytics: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openPrintableReport() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        PrintableReportDialog dlg = new PrintableReportDialog(parent, currentStats, currentDeptMap, currentStaffMetrics);
        dlg.setVisible(true);
    }

    private void handleExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Full Complaint Analytics Dataset");
        chooser.setSelectedFile(new File("campus_operations_audit_" + System.currentTimeMillis() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Comma-Separated Values (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getParentFile(), target.getName() + ".csv");
            }
            try {
                List<Complaint> all = complaintService.listAllComplaintsForAdmin(
                        null, null, null, null, null, null, "id", true);
                complaintService.exportComplaintsToCsv(all, target);
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                ToastNotification.showSuccess(parent, "Exported " + all.size() + " complaints to " + target.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private record ReportPayload(DashboardStats stats, Map<String, Integer> deptMap, List<StaffPerformanceDTO> staffMetrics) {}
}
