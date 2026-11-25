package com.smartcampus.ui.admin;

import com.smartcampus.model.MaintenanceRiskAssessment;
import com.smartcampus.model.PredictiveAnalysis;
import com.smartcampus.model.PredictiveDashboardMetrics;
import com.smartcampus.model.PreventiveRecommendation;
import com.smartcampus.model.enums.ComplaintCategory;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.RiskLevel;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.PredictiveMaintenanceService;
import com.smartcampus.ui.charts.BarChartPanel;
import com.smartcampus.ui.charts.DonutChartPanel;
import com.smartcampus.ui.charts.HorizontalBarChartPanel;
import com.smartcampus.ui.charts.TrendLineChartPanel;
import com.smartcampus.ui.components.BadgeCellRenderer;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.StatCard;
import com.smartcampus.ui.components.ToastNotification;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PredictiveMaintenancePanel extends JPanel {
    private final StatCard cardBuildings;
    private final StatCard cardRecurringLocs;
    private final StatCard cardHighRiskBldgs;
    private final StatCard cardCriticalRisks;
    private final StatCard cardPendingActions;
    private final StatCard cardCompletedActions;

    private final JComboBox<String> cmbAnalysisPeriod;
    private final JComboBox<String> cmbBuildingFilter;
    private final JComboBox<String> cmbCategoryFilter;
    private final JComboBox<String> cmbRiskFilter;
    private final JComboBox<String> cmbDateRangeMonths;
    private final JLabel lblAnalysisBanner;
    private final JProgressBar progressBar;

    private final HorizontalBarChartPanel chartByBuilding;
    private final DonutChartPanel chartByCategory;
    private final BarChartPanel chartBuildingRisk;
    private final TrendLineChartPanel chartMonthlyTrend;

    private final DefaultTableModel riskModel;
    private final JTable tableRisk;
    private final DefaultTableModel recModel;
    private final JTable tableRec;

    private final PredictiveMaintenanceService predictiveService = new PredictiveMaintenanceService();
    private final ComplaintService complaintService = new ComplaintService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private List<MaintenanceRiskAssessment> currentAssessments = new ArrayList<>();
    private List<PreventiveRecommendation> currentRecommendations = new ArrayList<>();

    public PredictiveMaintenancePanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topSection = new JPanel();
        topSection.setOpaque(false);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));

        JPanel titleBar = new JPanel(new BorderLayout(12, 0));
        titleBar.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Predictive Maintenance & Recurring Failure Intelligence",
                VectorIcons.chart(DesignSystem.ELECTRIC_BLUE, 18), JLabel.LEFT);
        title.setIconTextGap(8);
        title.setFont(DesignSystem.FONT_DISPLAY_MD);
        title.setForeground(DesignSystem.TEXT_PRIMARY);

        lblAnalysisBanner = new JLabel("Rule-based historical frequency & trend risk indicator (click 'Run Predictive Analysis' to analyze MySQL history)");
        lblAnalysisBanner.setFont(DesignSystem.FONT_SMALL);
        lblAnalysisBanner.setForeground(DesignSystem.TEXT_SECONDARY);

        titleStack.add(title);
        titleStack.add(lblAnalysisBanner);
        titleBar.add(titleStack, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        topActions.add(new JLabel("Analysis Window:"));
        cmbAnalysisPeriod = new JComboBox<>(new String[]{"30 Days", "7 Days", "90 Days", "365 Days"});
        topActions.add(cmbAnalysisPeriod);

        ModernButton btnRunAnalysis = new ModernButton("Run Predictive Analysis", ModernButton.ButtonType.PRIMARY);
        btnRunAnalysis.setIcon(VectorIcons.bolt(Color.WHITE, 14));
        btnRunAnalysis.setIconTextGap(6);

        ModernButton btnRefresh = new ModernButton("Refresh", ModernButton.ButtonType.SECONDARY);
        btnRefresh.setIcon(VectorIcons.refresh(DesignSystem.TEXT_PRIMARY, 14));
        btnRefresh.setIconTextGap(6);

        topActions.add(btnRunAnalysis);
        topActions.add(btnRefresh);
        titleBar.add(topActions, BorderLayout.EAST);

        topSection.add(titleBar);
        topSection.add(Box.createVerticalStrut(10));

        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 10, 10));
        kpiGrid.setOpaque(false);

        cardBuildings = new StatCard("Buildings Analyzed", "0", "Evaluated facilities", DesignSystem.ELECTRIC_BLUE);
        cardRecurringLocs = new StatCard("Recurring Locations", "0", "Repeated failure spots", DesignSystem.VIOLET);
        cardHighRiskBldgs = new StatCard("High-Risk Buildings", "0", "Score >= 50", DesignSystem.WARNING);
        cardCriticalRisks = new StatCard("Critical Maint Risks", "0", "Score >= 75", DesignSystem.CRITICAL);
        cardPendingActions = new StatCard("Actions Pending", "0", "Open recommendations", DesignSystem.CYAN);
        cardCompletedActions = new StatCard("Completed Actions", "0", "Preventive completed", DesignSystem.SUCCESS);

        kpiGrid.add(cardBuildings);
        kpiGrid.add(cardRecurringLocs);
        kpiGrid.add(cardHighRiskBldgs);
        kpiGrid.add(cardCriticalRisks);
        kpiGrid.add(cardPendingActions);
        kpiGrid.add(cardCompletedActions);
        topSection.add(kpiGrid);
        topSection.add(Box.createVerticalStrut(10));

        CardPanel filterCard = new CardPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterCard.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        cmbBuildingFilter = new JComboBox<>(new String[]{"All Buildings"});
        cmbCategoryFilter = new JComboBox<>();
        cmbCategoryFilter.addItem("All Categories");
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            cmbCategoryFilter.addItem(cat.getDisplayName());
        }
        cmbRiskFilter = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MODERATE", "LOW"});
        cmbDateRangeMonths = new JComboBox<>(new String[]{"6 Months", "3 Months", "12 Months"});

        ModernButton btnApply = new ModernButton("Apply Filters", ModernButton.ButtonType.SECONDARY);

        filterCard.add(new JLabel("Building:"));
        filterCard.add(cmbBuildingFilter);
        filterCard.add(new JLabel("Category:"));
        filterCard.add(cmbCategoryFilter);
        filterCard.add(new JLabel("Risk Level:"));
        filterCard.add(cmbRiskFilter);
        filterCard.add(new JLabel("Trend Range:"));
        filterCard.add(cmbDateRangeMonths);
        filterCard.add(btnApply);

        topSection.add(filterCard);
        add(topSection, BorderLayout.NORTH);

        class ScrollableBodyPanel extends JPanel implements Scrollable {
            ScrollableBodyPanel() {
                setOpaque(false);
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            }

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return getPreferredSize();
            }

            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 16;
            }

            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 48;
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return false;
            }
        }

        JPanel centerBody = new ScrollableBodyPanel();

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerBody.add(progressBar);

        JPanel chartsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        chartsGrid.setOpaque(false);
        chartsGrid.setPreferredSize(new Dimension(880, 490));
        chartsGrid.setMinimumSize(new Dimension(480, 440));

        chartByBuilding = new HorizontalBarChartPanel();
        chartByCategory = new DonutChartPanel();
        chartBuildingRisk = new BarChartPanel();
        chartBuildingRisk.setDefaultBarColor(DesignSystem.WARNING);
        chartMonthlyTrend = new TrendLineChartPanel();

        chartsGrid.add(wrapChart("Complaints by Building", "Total historical incidents across facilities", chartByBuilding));
        chartsGrid.add(wrapChart("Recurring Failures by Category", "Category share of repeated incidents", chartByCategory));
        chartsGrid.add(wrapChart("Building Risk Comparison", "Peak historical maintenance risk score (0-100)", chartBuildingRisk));
        chartsGrid.add(wrapChart("Monthly Maintenance Trend", "Complaint volume trajectory over selected range", chartMonthlyTrend));

        centerBody.add(chartsGrid);
        centerBody.add(Box.createVerticalStrut(12));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setPreferredSize(new Dimension(1050, 290));

        String[] riskCols = {"Building", "Location", "Category", "Incidents", "Risk Score", "Risk Level", "Recommended Action"};
        riskModel = new DefaultTableModel(riskCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableRisk = new JTable(riskModel);
        tableRisk.setRowHeight(32);
        tableRisk.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRisk.setFont(DesignSystem.FONT_SMALL);
        tableRisk.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableRisk.getColumnModel().getColumn(5).setCellRenderer(new BadgeCellRenderer());
        tableRisk.getColumnModel().getColumn(6).setPreferredWidth(340);

        tableRisk.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableRisk.getSelectedRow() >= 0) {
                    showRiskExplanationPopup(currentAssessments.get(tableRisk.getSelectedRow()));
                }
            }
        });

        CardPanel riskTab = new CardPanel(new BorderLayout());
        riskTab.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        riskTab.add(new JScrollPane(tableRisk), BorderLayout.CENTER);
        tabs.addTab("Risk Ranking & Explainable Factors", riskTab);

        String[] recCols = {"ID", "Building", "Location", "Category", "Incidents", "Risk Score",
                "Urgency", "Status", "Scheduled Date", "Recommended Preventive Action"};
        recModel = new DefaultTableModel(recCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableRec = new JTable(recModel);
        tableRec.setRowHeight(32);
        tableRec.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRec.setFont(DesignSystem.FONT_SMALL);
        tableRec.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);
        tableRec.getColumnModel().getColumn(7).setCellRenderer(new BadgeCellRenderer());
        tableRec.getColumnModel().getColumn(0).setPreferredWidth(40);
        tableRec.getColumnModel().getColumn(9).setPreferredWidth(310);

        JPanel recTab = new JPanel(new BorderLayout(8, 8));
        recTab.setBackground(Color.WHITE);
        recTab.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        recTab.add(new JScrollPane(tableRec), BorderLayout.CENTER);

        JPanel recActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        recActions.setOpaque(false);
        ModernButton btnUnderReview = new ModernButton("Under Review", ModernButton.ButtonType.GHOST);
        ModernButton btnApprove = new ModernButton("Approve", ModernButton.ButtonType.PRIMARY);
        ModernButton btnSchedule = new ModernButton("Schedule Inspection", ModernButton.ButtonType.WARNING);
        ModernButton btnComplete = new ModernButton("Mark Completed", ModernButton.ButtonType.SUCCESS);
        ModernButton btnDismiss = new ModernButton("Dismiss", ModernButton.ButtonType.DANGER);

        recActions.add(btnUnderReview);
        recActions.add(btnApprove);
        recActions.add(btnSchedule);
        recActions.add(btnComplete);
        recActions.add(btnDismiss);
        recTab.add(recActions, BorderLayout.SOUTH);

        tabs.addTab("Preventive Maintenance Recommendations", recTab);
        centerBody.add(tabs);

        JScrollPane mainScroll = new JScrollPane(centerBody);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        btnRunAnalysis.addActionListener(e -> executePredictiveAnalysis());
        btnRefresh.addActionListener(e -> loadPredictiveData());
        btnApply.addActionListener(e -> loadPredictiveData());
        cmbRiskFilter.addActionListener(e -> loadPredictiveData());
        cmbCategoryFilter.addActionListener(e -> loadPredictiveData());
        cmbDateRangeMonths.addActionListener(e -> loadPredictiveData());

        btnUnderReview.addActionListener(e -> transitionRecommendation(RecommendationStatus.UNDER_REVIEW, false));
        btnApprove.addActionListener(e -> transitionRecommendation(RecommendationStatus.APPROVED, false));
        btnSchedule.addActionListener(e -> transitionRecommendation(RecommendationStatus.SCHEDULED, true));
        btnComplete.addActionListener(e -> transitionRecommendation(RecommendationStatus.COMPLETED, false));
        btnDismiss.addActionListener(e -> transitionRecommendation(RecommendationStatus.DISMISSED, false));
    }

    private CardPanel wrapChart(String title, String subtitle, JPanel chart) {
        CardPanel card = new CardPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(DesignSystem.FONT_SUBTITLE);
        t.setForeground(DesignSystem.TEXT_PRIMARY);
        JLabel s = new JLabel(subtitle);
        s.setFont(DesignSystem.FONT_TINY);
        s.setForeground(DesignSystem.TEXT_MUTED);
        hdr.add(t, BorderLayout.WEST);
        hdr.add(s, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    public void loadPredictiveData() {
        progressBar.setVisible(true);
        String bldg = cmbBuildingFilter.getSelectedItem() != null ? cmbBuildingFilter.getSelectedItem().toString() : "All Buildings";
        String bldgParam = "All Buildings".equals(bldg) ? null : bldg;
        String cat = cmbCategoryFilter.getSelectedItem() != null ? cmbCategoryFilter.getSelectedItem().toString() : "All Categories";
        String catParam = "All Categories".equals(cat) ? null : cat;
        String riskParam = cmbRiskFilter.getSelectedItem() != null ? cmbRiskFilter.getSelectedItem().toString() : "ALL";
        int months = parseLeadingInt((String) cmbDateRangeMonths.getSelectedItem(), 6);

        new SwingWorker<PredictivePayload, Void>() {
            @Override
            protected PredictivePayload doInBackground() throws Exception {
                PredictiveMaintenanceService.PredictiveDashboardData data = predictiveService.getPredictiveDashboardData(
                        bldgParam, catParam, riskParam, null, months);
                List<String> buildings = complaintService.getDistinctBuildings();
                return new PredictivePayload(data, buildings);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    PredictivePayload payload = get();
                    PredictiveMaintenanceService.PredictiveDashboardData data = payload.data;

                    PredictiveDashboardMetrics m = data.metrics();
                    cardBuildings.setValue(m.buildingsAnalyzed());
                    cardRecurringLocs.setValue(m.recurringFailureLocations());
                    cardHighRiskBldgs.setValue(m.highRiskBuildings());
                    cardCriticalRisks.setValue(m.criticalMaintenanceRisks());
                    cardPendingActions.setValue(m.preventiveActionsPending());
                    cardCompletedActions.setValue(m.recommendationsCompleted());

                    if (data.latestAnalysis().isPresent()) {
                        PredictiveAnalysis latest = data.latestAnalysis().get();
                        lblAnalysisBanner.setText(String.format("Last Analysis (%s): %s [Historical-data-based risk indicator, not guaranteed prediction]",
                                dateFormat.format(latest.getAnalysisTimestamp()), latest.getAnalysisSummary()));
                    }

                    Object selBldg = cmbBuildingFilter.getSelectedItem();
                    cmbBuildingFilter.removeAllItems();
                    cmbBuildingFilter.addItem("All Buildings");
                    for (String b : payload.buildings) cmbBuildingFilter.addItem(b);
                    if (selBldg != null) cmbBuildingFilter.setSelectedItem(selBldg);

                    chartByBuilding.setData(data.complaintsByBuilding());
                    chartByCategory.setData(data.recurringFailuresByCategory());
                    int totalRec = data.recurringFailuresByCategory().values().stream().mapToInt(Integer::intValue).sum();
                    chartByCategory.setCenterText("Recurring", String.valueOf(totalRec));

                    Map<String, Double> bldgRiskMap = new LinkedHashMap<>();
                    Map<String, Integer> assessmentIncidentsByBuilding = new LinkedHashMap<>();
                    for (MaintenanceRiskAssessment r : data.assessments()) {
                        bldgRiskMap.merge(r.getBuilding(), r.getRiskScore(), Math::max);
                        assessmentIncidentsByBuilding.merge(r.getBuilding(), r.getIncidentCount(), Integer::sum);
                    }
                    chartBuildingRisk.setData(bldgRiskMap);
                    for (Map.Entry<String, Double> e : bldgRiskMap.entrySet()) {
                        String building = e.getKey();
                        double score = e.getValue();
                        RiskLevel rl = RiskLevel.fromScore(score);
                        Color col = switch (rl) {
                            case CRITICAL -> DesignSystem.CRITICAL;
                            case HIGH -> DesignSystem.WARNING;
                            case MODERATE -> DesignSystem.ELECTRIC_BLUE;
                            case LOW -> DesignSystem.SUCCESS;
                        };
                        chartBuildingRisk.setBarColor(building, col);
                        int complaintsCount = data.complaintsByBuilding().getOrDefault(
                                building,
                                assessmentIncidentsByBuilding.getOrDefault(building, 0));
                        chartBuildingRisk.setBarTooltip(building, buildBuildingRiskTooltip(building, score, rl, complaintsCount));
                    }

                    chartMonthlyTrend.setData(data.monthlyTrends());

                    currentAssessments = data.assessments();
                    riskModel.setRowCount(0);
                    for (MaintenanceRiskAssessment r : currentAssessments) {
                        riskModel.addRow(new Object[]{
                                r.getBuilding(),
                                r.getLocation(),
                                r.getCategory(),
                                r.getIncidentCount(),
                                String.format("%.1f / 100", r.getRiskScore()),
                                r.getRiskLevel(),
                                r.getRecommendation()
                        });
                    }

                    currentRecommendations = data.recommendations();
                    recModel.setRowCount(0);
                    for (PreventiveRecommendation rec : currentRecommendations) {
                        recModel.addRow(new Object[]{
                                rec.getId(),
                                rec.getBuilding(),
                                rec.getLocation(),
                                rec.getCategory(),
                                rec.getSupportingComplaintCount(),
                                String.format("%.1f", rec.getRiskScore()),
                                rec.getUrgency(),
                                rec.getStatus(),
                                rec.getScheduledDate() != null ? dateFormat.format(rec.getScheduledDate()) : "-",
                                rec.getRecommendation()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PredictiveMaintenancePanel.this,
                            "Failed to load predictive analytics: " + ex.getMessage(),
                            "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void executePredictiveAnalysis() {
        int days = parseLeadingInt((String) cmbAnalysisPeriod.getSelectedItem(), 30);
        progressBar.setVisible(true);

        new SwingWorker<PredictiveMaintenanceService.AnalysisExecutionResult, Void>() {
            @Override
            protected PredictiveMaintenanceService.AnalysisExecutionResult doInBackground() throws Exception {
                return predictiveService.runPredictiveAnalysis(days);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    PredictiveMaintenanceService.AnalysisExecutionResult res = get();
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(PredictiveMaintenancePanel.this);
                    ToastNotification.showSuccess(parent,
                            String.format("Analysis complete: %d complaints -> %d risk patterns, %d recommendations.",
                                    res.analysis().getTotalComplaintsAnalyzed(), res.assessments().size(), res.recommendations().size()));
                    loadPredictiveData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PredictiveMaintenancePanel.this,
                            "Predictive analysis failed: " + ex.getMessage(),
                            "Analysis Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void transitionRecommendation(RecommendationStatus targetStatus, boolean askScheduleDate) {
        int row = tableRec.getSelectedRow();
        if (row < 0 || row >= currentRecommendations.size()) {
            JOptionPane.showMessageDialog(this, "Please select a preventive recommendation first.",
                    "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PreventiveRecommendation rec = currentRecommendations.get(row);
        Timestamp scheduledTs = null;

        if (askScheduleDate) {
            String defaultDate = LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE) + " 10:00";
            String input = JOptionPane.showInputDialog(this,
                    "Enter scheduled inspection date/time (yyyy-MM-dd HH:mm):", defaultDate);
            if (input == null || input.isBlank()) return;
            try {
                LocalDateTime ldt = LocalDateTime.parse(input.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                scheduledTs = Timestamp.valueOf(ldt);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Expected yyyy-MM-dd HH:mm.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        final Timestamp finalScheduledTs = scheduledTs;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                predictiveService.updateRecommendationLifecycle(rec.getId(), targetStatus, finalScheduledTs);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(PredictiveMaintenancePanel.this);
                    ToastNotification.showSuccess(parent, "Recommendation #" + rec.getId() + " updated to " + targetStatus.getDisplayName());
                    loadPredictiveData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PredictiveMaintenancePanel.this,
                            "Failed to update recommendation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showRiskExplanationPopup(MaintenanceRiskAssessment r) {
        JTextArea area = new JTextArea(
                "Building: " + r.getBuilding() + "\n" +
                "Location: " + r.getLocation() + "\n" +
                "Category: " + r.getCategory() + "\n" +
                "Historical Incidents: " + r.getIncidentCount() + "\n" +
                "Predictive Risk Score: " + String.format("%.2f / 100 (%s)", r.getRiskScore(), r.getRiskLevel()) + "\n\n" +
                "Contributing Factors & Triggered Rules:\n" + r.getContributingFactors() + "\n\n" +
                "Recommended Preventive Action:\n" + r.getRecommendation(),
                14, 58);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(DesignSystem.FONT_BODY);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Explainable Predictive Risk Breakdown", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String buildBuildingRiskTooltip(String building, double riskScore, RiskLevel level, int complaintsCount) {
        String safeBuilding = building != null
                ? building.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                : "Unknown";
        return String.format(
                "<html>Building: <b>%s</b><br>Risk Score: <b>%.0f</b><br>Classification: <b>%s</b><br>Historical Complaints: <b>%d</b></html>",
                safeBuilding, riskScore, level.name(), complaintsCount
        );
    }

    private static int parseLeadingInt(String text, int fallback) {
        if (text == null) return fallback;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return fallback;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private record PredictivePayload(
            PredictiveMaintenanceService.PredictiveDashboardData data,
            List<String> buildings
    ) {}
}
