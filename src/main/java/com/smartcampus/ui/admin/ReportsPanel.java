package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;

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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends JPanel {
    private final StatCard cardTotal;
    private final StatCard cardResolved;
    private final StatCard cardAvgTime;
    private final StatCard cardOverdue;
    private final StatCard cardRate;

    private final JTable tableCategoryBreakdown;
    private final DefaultTableModel catModel;
    private final JTable tableStatusBreakdown;
    private final DefaultTableModel statusModel;

    private final JProgressBar progressBar;
    private final ModernButton btnExportCsv;
    private final ModernButton btnRefresh;

    private final ComplaintService complaintService = new ComplaintService();

    public ReportsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Top KPI Row
        JPanel topKpi = new JPanel(new GridLayout(1, 5, 12, 12));
        topKpi.setOpaque(false);

        cardTotal = new StatCard("Total Complaints", "0", "Campus all-time", new Color(37, 99, 235));
        cardResolved = new StatCard("Resolved Count", "0", "Successfully closed", new Color(22, 163, 74));
        cardRate = new StatCard("Resolution Rate", "0%", "Resolved / Total", new Color(13, 148, 136));
        cardAvgTime = new StatCard("Avg Resolution", "0h", "Calculated turn-around", new Color(217, 119, 6));
        cardOverdue = new StatCard("Overdue Issues", "0", ">72h unresolved", new Color(220, 38, 38));

        topKpi.add(cardTotal);
        topKpi.add(cardResolved);
        topKpi.add(cardRate);
        topKpi.add(cardAvgTime);
        topKpi.add(cardOverdue);

        add(topKpi, BorderLayout.NORTH);

        // Center Breakdowns
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 16, 16));
        tablesPanel.setOpaque(false);

        // 1. Category table
        JPanel pnlCat = new JPanel(new BorderLayout(8, 8));
        pnlCat.setBackground(Color.WHITE);
        pnlCat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblCat = new JLabel("Complaints by Category");
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pnlCat.add(lblCat, BorderLayout.NORTH);

        catModel = new DefaultTableModel(new String[]{"Category", "Total Count", "Share %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableCategoryBreakdown = new JTable(catModel);
        tableCategoryBreakdown.setRowHeight(28);
        pnlCat.add(new JScrollPane(tableCategoryBreakdown), BorderLayout.CENTER);
        tablesPanel.add(pnlCat);

        // 2. Status & Priority table
        JPanel pnlStatus = new JPanel(new BorderLayout(8, 8));
        pnlStatus.setBackground(Color.WHITE);
        pnlStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblStat = new JLabel("Lifecycle Status Distribution");
        lblStat.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pnlStatus.add(lblStat, BorderLayout.NORTH);

        statusModel = new DefaultTableModel(new String[]{"Status", "Count", "Percentage"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableStatusBreakdown = new JTable(statusModel);
        tableStatusBreakdown.setRowHeight(28);
        pnlStatus.add(new JScrollPane(tableStatusBreakdown), BorderLayout.CENTER);
        tablesPanel.add(pnlStatus);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerWrap.add(progressBar, BorderLayout.NORTH);
        centerWrap.add(tablesPanel, BorderLayout.CENTER);

        add(centerWrap, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnExportCsv = new ModernButton("Export Authorized Report (CSV)", ModernButton.ButtonType.PRIMARY);
        btnRefresh = new ModernButton("Refresh Metrics", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnRefresh);
        bottomBar.add(btnExportCsv);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadReportData());
        btnExportCsv.addActionListener(e -> handleExportCsv());

        loadReportData();
    }

    public void loadReportData() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        new SwingWorker<DashboardStats, Void>() {
            @Override
            protected DashboardStats doInBackground() throws Exception {
                return complaintService.getDashboardStats();
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    DashboardStats stats = get();
                    int total = stats.getTotalComplaints();
                    int resolved = stats.getResolvedComplaints();
                    double rate = total > 0 ? ((double) resolved / total) * 100.0 : 0.0;

                    cardTotal.setValue(total);
                    cardResolved.setValue(resolved);
                    cardRate.setValue(String.format("%.1f%%", rate));
                    cardAvgTime.setValue(String.format("%.1fh", stats.getAvgResolutionHours()));
                    cardOverdue.setValue(stats.getOverdueComplaints());

                    // Populate categories
                    catModel.setRowCount(0);
                    for (Map.Entry<String, Integer> entry : stats.getComplaintsByCategory().entrySet()) {
                        double share = total > 0 ? ((double) entry.getValue() / total) * 100.0 : 0.0;
                        catModel.addRow(new Object[]{
                                entry.getKey(),
                                entry.getValue(),
                                String.format("%.1f%%", share)
                        });
                    }

                    // Populate statuses
                    statusModel.setRowCount(0);
                    for (Map.Entry<String, Integer> entry : stats.getComplaintsByStatus().entrySet()) {
                        double share = total > 0 ? ((double) entry.getValue() / total) * 100.0 : 0.0;
                        statusModel.addRow(new Object[]{
                                entry.getKey(),
                                entry.getValue(),
                                String.format("%.1f%%", share)
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Failed to load analytics: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Complaints Report as CSV");
        String defaultName = "Campus_Complaints_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = chooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }

            File finalFile = fileToSave;
            progressBar.setVisible(true);
            btnExportCsv.setEnabled(false);

            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    List<Complaint> allComplaints = complaintService.listAllComplaintsForAdmin(
                            null, null, null, null, null, null, "priority", false);
                    complaintService.exportComplaintsToCsv(allComplaints, finalFile);
                    return allComplaints.size();
                }

                @Override
                protected void done() {
                    progressBar.setVisible(false);
                    btnExportCsv.setEnabled(true);
                    try {
                        int exportedCount = get();
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                "Successfully exported " + exportedCount + " complaints to:\n" + finalFile.getAbsolutePath(),
                                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                "Failed to export report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
