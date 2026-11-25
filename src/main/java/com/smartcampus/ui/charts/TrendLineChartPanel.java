package com.smartcampus.ui.charts;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrendLineChartPanel extends JPanel {
    private final Map<String, Double> data = new LinkedHashMap<>();
    private int hoveredPoint = -1;

    public TrendLineChartPanel() {
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkHover(e.getX(), e.getY());
            }
        });
    }

    public synchronized void setData(Map<String, ? extends Number> rawData) {
        data.clear();
        if (rawData != null) {
            for (Map.Entry<String, ? extends Number> entry : rawData.entrySet()) {
                data.put(entry.getKey(), entry.getValue() != null ? entry.getValue().doubleValue() : 0.0);
            }
        }
        repaint();
    }

    private void checkHover(int mx, int my) {
        if (data.size() < 2) return;
        int w = getWidth();
        int h = getHeight();
        int padL = 40, padR = 25, padT = 20, padB = 35;
        int chartW = w - padL - padR;
        int chartH = h - padT - padB;

        double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;
        int gridCeil = (int) Math.ceil(maxVal * 1.2);
        if (gridCeil < 4) gridCeil = 4;

        int numPoints = data.size();
        double stepX = (double) chartW / (numPoints - 1);

        int found = -1;
        int idx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            int px = (int) Math.round(padL + idx * stepX);
            int py = (int) Math.round(padT + chartH - (entry.getValue() / gridCeil) * chartH);

            if (Math.hypot(mx - px, my - py) <= 12) {
                found = idx;
                setToolTipText(String.format("%s: %.0f complaints", entry.getKey(), entry.getValue()));
                break;
            }
            idx++;
        }

        if (found != hoveredPoint) {
            hoveredPoint = found;
            if (found == -1) setToolTipText(null);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        DesignSystem.applyQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        if (data.isEmpty()) {
            g2.setColor(DesignSystem.TEXT_MUTED);
            g2.setFont(DesignSystem.FONT_SMALL);
            String empty = "No timeline data available";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(empty, (w - fm.stringWidth(empty)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int padL = 40, padR = 25, padT = 20, padB = 35;
        int chartW = w - padL - padR;
        int chartH = h - padT - padB;

        double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;
        int gridCeil = (int) Math.ceil(maxVal * 1.2);
        if (gridCeil < 4) gridCeil = 4;

        g2.setFont(DesignSystem.FONT_TINY);
        FontMetrics fmTiny = g2.getFontMetrics();
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            int y = padT + chartH - (i * chartH / steps);
            int val = (gridCeil * i) / steps;
            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(padL, y, padL + chartW, y);

            g2.setColor(DesignSystem.TEXT_MUTED);
            String strVal = String.valueOf(val);
            g2.drawString(strVal, padL - fmTiny.stringWidth(strVal) - 8, y + 4);
        }

        int numPoints = data.size();
        double stepX = (numPoints > 1) ? (double) chartW / (numPoints - 1) : chartW;

        List<java.awt.Point> points = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            int px = (int) Math.round(padL + idx * stepX);
            int py = (int) Math.round(padT + chartH - (entry.getValue() / gridCeil) * chartH);
            points.add(new java.awt.Point(px, py));
            idx++;
        }

        if (points.size() > 1) {
            Polygon poly = new Polygon();
            poly.addPoint(points.get(0).x, padT + chartH);
            for (java.awt.Point p : points) {
                poly.addPoint(p.x, p.y);
            }
            poly.addPoint(points.get(points.size() - 1).x, padT + chartH);

            GradientPaint gp = new GradientPaint(
                    0, padT, new Color(71, 118, 255, 65),
                    0, padT + chartH, new Color(71, 118, 255, 5)
            );
            g2.setPaint(gp);
            g2.fill(poly);

            g2.setColor(DesignSystem.ELECTRIC_BLUE);
            g2.setStroke(new java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            for (int i = 0; i < points.size() - 1; i++) {
                g2.drawLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
            }
        }

        idx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            java.awt.Point p = points.get(idx);
            boolean isHov = (idx == hoveredPoint);

            g2.setColor(Color.WHITE);
            g2.fillOval(p.x - 5, p.y - 5, 10, 10);
            g2.setColor(isHov ? DesignSystem.CRITICAL : DesignSystem.ELECTRIC_BLUE);
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawOval(p.x - 5, p.y - 5, 10, 10);

            g2.setFont(DesignSystem.FONT_TINY);
            g2.setColor(isHov ? DesignSystem.TEXT_PRIMARY : DesignSystem.TEXT_SECONDARY);
            String lbl = entry.getKey();
            int lblX = p.x - (fmTiny.stringWidth(lbl) / 2);
            g2.drawString(lbl, lblX, padT + chartH + 18);

            idx++;
        }

        g2.dispose();
    }
}
