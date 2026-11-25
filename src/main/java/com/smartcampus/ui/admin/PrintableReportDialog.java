package com.smartcampus.ui.admin;

import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.StaffPerformanceDTO;
import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrintableReportDialog extends JDialog {
    private final JEditorPane editorPane;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PrintableReportDialog(JFrame parent, DashboardStats stats, Map<String, Integer> deptMap,
                                 List<StaffPerformanceDTO> staffMetrics) {
        super(parent, "Official Campus Operations Report - Executive Print Preview", true);
        setSize(880, 720);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DesignSystem.MIDNIGHT_NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Executive Facilities Audit & Performance Report");
        title.setFont(DesignSystem.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Formatted for official institutional review, compliance auditing, and print generation");
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(buildReportHtml(stats, deptMap, staffMetrics));
        editorPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(editorPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        ModernButton btnPrint = new ModernButton("Print Official Report", ModernButton.ButtonType.PRIMARY);
        ModernButton btnClose = new ModernButton("Close Preview", ModernButton.ButtonType.SECONDARY);

        bottomBar.add(btnClose);
        bottomBar.add(btnPrint);
        add(bottomBar, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> dispose());
        btnPrint.addActionListener(e -> {
            try {
                boolean complete = editorPane.print();
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Print job sent to system printer successfully.",
                            "Print Complete", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Failed to print: " + ex.getMessage(),
                        "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private String buildReportHtml(DashboardStats stats, Map<String, Integer> deptMap, List<StaffPerformanceDTO> staffMetrics) {
        User admin = UserSession.getCurrentUser();
        String adminName = admin != null ? admin.getFullName() : "Chief Operations Officer";
        String adminEmail = admin != null ? admin.getEmail() : "admin@campus.edu";
        String dateStr = dateFormat.format(new Date());

        double resolutionRate = (stats.getTotalComplaints() > 0)
                ? ((double) stats.getResolvedComplaints() / stats.getTotalComplaints()) * 100.0 : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><style>")
          .append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; color: #1e293b; line-height: 1.4; }")
          .append(".header-box { border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 20px; }")
          .append(".inst-title { font-size: 20px; font-weight: bold; color: #0f172a; letter-spacing: 0.5px; }")
          .append(".sub-title { font-size: 13px; color: #475569; margin-top: 3px; }")
          .append(".meta-bar { font-size: 11px; color: #64748b; margin-top: 8px; }")
          .append(".section-title { font-size: 14px; font-weight: bold; color: #0f172a; margin-top: 22px; margin-bottom: 8px; text-transform: uppercase; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; }")
          .append("table { width: 100%; border-collapse: collapse; margin-top: 8px; margin-bottom: 14px; font-size: 12px; }")
          .append("th { background-color: #f1f5f9; color: #334155; text-align: left; padding: 7px 10px; border: 1px solid #cbd5e1; font-weight: 600; }")
          .append("td { padding: 6px 10px; border: 1px solid #e2e8f0; }")
          .append(".kpi-box { display: inline-block; width: 18%; border: 1px solid #cbd5e1; background: #f8fafc; padding: 8px; margin-right: 1.5%; text-align: center; }")
          .append(".kpi-val { font-size: 18px; font-weight: bold; color: #0f172a; }")
          .append(".kpi-lbl { font-size: 10px; color: #64748b; text-transform: uppercase; }")
          .append(".sign-box { margin-top: 35px; border-top: 1px solid #cbd5e1; padding-top: 12px; font-size: 11px; }")
          .append("</style></head><body>");

        sb.append("<div class='header-box'>")
          .append("<div class='inst-title'>SMART CAMPUS OPERATIONS & FACILITIES INTELLIGENCE</div>")
          .append("<div class='sub-title'>Executive Facilities Management, SLA Tracking & Operations Compliance Audit</div>")
          .append("<div class='meta-bar'>Report Generated: <b>").append(dateStr).append("</b> | Prepared By: <b>")
          .append(adminName).append("</b> (").append(adminEmail).append(")</div>")
          .append("</div>");

        sb.append("<div class='section-title'>1. High-Level Operations Overview</div>");
        sb.append("<table>")
          .append("<tr><th>Metric</th><th>Value</th><th>Institutional Target / SLA</th><th>Status</th></tr>")
          .append("<tr><td>Total Logged Complaints</td><td><b>").append(stats.getTotalComplaints()).append("</b></td><td>Campus Volume Benchmark</td><td>Monitored</td></tr>")
          .append("<tr><td>Successfully Resolved</td><td><b>").append(stats.getResolvedComplaints()).append("</b></td><td>> 85% Target</td><td>")
          .append(resolutionRate >= 85 ? "<span style='color: #16a34a;'>Compliant</span>" : "<span style='color: #d97706;'>Review</span>").append("</td></tr>")
          .append("<tr><td>Campus Resolution Rate</td><td><b>").append(String.format("%.1f%%", resolutionRate)).append("</b></td><td>> 80% Baseline</td><td>Active</td></tr>")
          .append("<tr><td>Average Resolution Turnaround</td><td><b>").append(String.format("%.1f hours", stats.getAvgResolutionHours())).append("</b></td><td>< 24.0 Hours</td><td>")
          .append(stats.getAvgResolutionHours() <= 24 ? "<span style='color: #16a34a;'>Within SLA</span>" : "<span style='color: #dc2626;'>SLA Exceeded</span>").append("</td></tr>")
          .append("<tr><td>Active Safety-Critical Issues</td><td><b>").append(stats.getCriticalComplaints()).append("</b></td><td>0 (Zero Tolerance)</td><td>")
          .append(stats.getCriticalComplaints() == 0 ? "<span style='color: #16a34a;'>Clear</span>" : "<span style='color: #dc2626;'>Immediate Action</span>").append("</td></tr>")
          .append("<tr><td>Overdue Issues (> 72h Unresolved)</td><td><b>").append(stats.getOverdueComplaints()).append("</b></td><td>0 Overdue</td><td>")
          .append(stats.getOverdueComplaints() == 0 ? "<span style='color: #16a34a;'>Nominal</span>" : "<span style='color: #dc2626;'>Escalated</span>").append("</td></tr>")
          .append("</table>");

        sb.append("<div class='section-title'>2. Department-Wise Incident Distribution</div>");
        sb.append("<table><tr><th>Academic Department</th><th>Reported Complaints</th><th>Volume Share (%)</th></tr>");
        for (Map.Entry<String, Integer> entry : deptMap.entrySet()) {
            double share = (stats.getTotalComplaints() > 0) ? ((double) entry.getValue() / stats.getTotalComplaints()) * 100.0 : 0.0;
            sb.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue())
              .append("</td><td>").append(String.format("%.1f%%", share)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<div class='section-title'>3. Maintenance Staff Performance Scorecard</div>");
        sb.append("<table><tr><th>Technician Name</th><th>Specialization</th><th>Assigned</th><th>Resolved</th><th>Rate %</th><th>Avg Turnaround</th><th>Rating</th></tr>");
        for (StaffPerformanceDTO s : staffMetrics) {
            String ratingStr = (s.avgRating() > 0) ? String.format("%.1f / 5.0", s.avgRating()) : "N/A";
            sb.append("<tr>")
              .append("<td><b>").append(s.fullName()).append("</b></td>")
              .append("<td>").append(s.department() != null ? s.department() : "Facilities").append("</td>")
              .append("<td>").append(s.assignedCount()).append("</td>")
              .append("<td>").append(s.resolvedCount()).append("</td>")
              .append("<td>").append(String.format("%.1f%%", s.getResolutionRate())).append("</td>")
              .append("<td>").append(String.format("%.1fh", s.avgResolutionHours())).append("</td>")
              .append("<td>").append(ratingStr).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");

        sb.append("<div class='sign-box'>")
          .append("<table style='border: none;'>")
          .append("<tr style='border: none;'>")
          .append("<td style='border: none; width: 50%;'>")
          .append("<b>Certified By:</b><br><br><br>____________________________________<br>")
          .append(adminName).append("<br>Campus Director of Facilities & Operations")
          .append("</td>")
          .append("<td style='border: none; width: 50%; text-align: right;'>")
          .append("<b>Official Seal & Date:</b><br><br><br>Date: ________________________<br>")
          .append("Smart Campus System Authentication ID: #SC-").append(System.currentTimeMillis() % 100000)
          .append("</td>")
          .append("</tr></table>")
          .append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }
}
