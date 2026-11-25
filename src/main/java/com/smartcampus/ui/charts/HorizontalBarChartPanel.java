package com.smartcampus.ui.charts;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

public class HorizontalBarChartPanel extends JPanel {
    private final Map<String, Double> data = new LinkedHashMap<>();
    private Color barColor = DesignSystem.VIOLET;
    private int hoveredIndex = -1;

    public HorizontalBarChartPanel() {
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

    public void setBarColor(Color color) {
        this.barColor = color;
        repaint();
    }

    private void checkHover(int mx, int my) {
        if (data.isEmpty()) return;
        int numRows = data.size();
        int padTop = 15;
        int rowHeight = 32;

        if (my >= padTop && my < padTop + numRows * rowHeight) {
            int idx = (my - padTop) / rowHeight;
            if (idx >= 0 && idx < numRows) {
                int i = 0;
                for (Map.Entry<String, Double> entry : data.entrySet()) {
                    if (i == idx) {
                        hoveredIndex = idx;
                        setToolTipText(String.format("%s: %.0f complaints", entry.getKey(), entry.getValue()));
                        repaint();
                        return;
                    }
                    i++;
                }
            }
        }
        if (hoveredIndex != -1) {
            hoveredIndex = -1;
            setToolTipText(null);
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
            String empty = "No records to display";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(empty, (w - fm.stringWidth(empty)) / 2, h / 2);
            g2.dispose();
            return;
        }

        double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxVal <= 0) maxVal = 1.0;

        int padLeft = 140;
        int padRight = 50;
        int barAreaW = Math.max(w - padLeft - padRight, 60);

        int padTop = 15;
        int rowHeight = 32;
        int barThickness = 14;

        g2.setFont(DesignSystem.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();

        int idx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            int y = padTop + (idx * rowHeight);

            String label = entry.getKey();
            if (label.length() > 18) label = label.substring(0, 16) + "..";
            g2.setColor(idx == hoveredIndex ? DesignSystem.TEXT_PRIMARY : DesignSystem.TEXT_SECONDARY);
            g2.drawString(label, padLeft - fm.stringWidth(label) - 12, y + 12);

            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(padLeft, y, barAreaW, barThickness, 6, 6);

            int barLen = (int) Math.round((entry.getValue() / maxVal) * barAreaW);
            if (barLen > 0) {
                g2.setColor(idx == hoveredIndex ? barColor.brighter() : barColor);
                g2.fillRoundRect(padLeft, y, Math.max(barLen, 6), barThickness, 6, 6);
            }

            String valStr = String.valueOf((int) Math.round(entry.getValue()));
            g2.setColor(DesignSystem.TEXT_PRIMARY);
            g2.setFont(DesignSystem.FONT_SMALL_BOLD);
            g2.drawString(valStr, padLeft + barLen + 8, y + 12);

            g2.setFont(DesignSystem.FONT_SMALL);
            idx++;
        }

        g2.dispose();
    }
}
