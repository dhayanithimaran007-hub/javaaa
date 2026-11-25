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
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DonutChartPanel extends JPanel {
    private final Map<String, Double> data = new LinkedHashMap<>();
    private final List<Color> sliceColors = new ArrayList<>();
    private int hoveredSlice = -1;
    private String centerLabel = "Total";
    private String centerValue = "0";

    private static final Color[] PALETTE = {
            DesignSystem.ELECTRIC_BLUE,
            DesignSystem.VIOLET,
            DesignSystem.CYAN,
            DesignSystem.WARNING,
            DesignSystem.CRITICAL,
            DesignSystem.SUCCESS,
            new Color(0x63, 0x66, 0xF1),
            new Color(0xEC, 0x48, 0x99)
    };

    public DonutChartPanel() {
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
        sliceColors.clear();
        double total = 0;
        int colorIdx = 0;

        if (rawData != null) {
            for (Map.Entry<String, ? extends Number> entry : rawData.entrySet()) {
                double val = entry.getValue() != null ? entry.getValue().doubleValue() : 0.0;
                if (val > 0) {
                    data.put(entry.getKey(), val);
                    total += val;
                    sliceColors.add(PALETTE[colorIdx % PALETTE.length]);
                    colorIdx++;
                }
            }
        }
        centerValue = String.valueOf((int) Math.round(total));
        repaint();
    }

    public void setCenterText(String label, String value) {
        this.centerLabel = label;
        this.centerValue = value;
        repaint();
    }

    private void checkHover(int mx, int my) {
        int w = getWidth();
        int h = getHeight();
        int chartSize = Math.min(w - 180, h - 30);
        if (chartSize < 50) return;

        int cx = 20 + chartSize / 2;
        int cy = h / 2;
        double dist = Math.hypot(mx - cx, my - cy);
        int outerR = chartSize / 2;
        int innerR = (int) (outerR * 0.58);

        if (dist >= innerR && dist <= outerR) {
            double angle = Math.toDegrees(Math.atan2(my - cy, mx - cx));
            if (angle < 0) angle += 360;

            double chartAngle = (360 - angle + 90) % 360;

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            double curAngle = 0;
            int idx = 0;
            int found = -1;

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double sweep = (entry.getValue() / total) * 360.0;
                if (chartAngle >= curAngle && chartAngle < curAngle + sweep) {
                    found = idx;
                    setToolTipText(String.format("%s: %.0f (%.1f%%)", entry.getKey(), entry.getValue(), (entry.getValue() / total) * 100));
                    break;
                }
                curAngle += sweep;
                idx++;
            }
            if (found != hoveredSlice) {
                hoveredSlice = found;
                repaint();
            }
        } else {
            if (hoveredSlice != -1) {
                hoveredSlice = -1;
                setToolTipText(null);
                repaint();
            }
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

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        int legendWidth = 170;
        int chartSize = Math.min(w - legendWidth - 30, h - 24);
        int chartX = 16;
        int chartY = (h - chartSize) / 2;

        int outerR = chartSize;
        int innerR = (int) (chartSize * 0.58);
        int innerOffset = (outerR - innerR) / 2;

        double curAngle = 90.0;
        int idx = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double sweep = (entry.getValue() / total) * 360.0;
            Color color = sliceColors.get(idx);

            int grow = (idx == hoveredSlice) ? 4 : 0;
            g2.setColor(color);
            Arc2D.Double arc = new Arc2D.Double(
                    chartX - grow, chartY - grow,
                    outerR + grow * 2, outerR + grow * 2,
                    curAngle, -sweep, Arc2D.PIE
            );
            g2.fill(arc);

            curAngle -= sweep;
            idx++;
        }

        g2.setColor(getBackground() != null && getBackground().getAlpha() > 0 ? getBackground() : DesignSystem.CARD_BG);
        g2.fillOval(chartX + innerOffset, chartY + innerOffset, innerR, innerR);

        g2.setColor(DesignSystem.TEXT_MUTED);
        g2.setFont(DesignSystem.FONT_TINY);
        FontMetrics fmTiny = g2.getFontMetrics();
        g2.drawString(centerLabel.toUpperCase(),
                chartX + innerOffset + (innerR - fmTiny.stringWidth(centerLabel.toUpperCase())) / 2,
                chartY + innerOffset + (innerR / 2) - 6);

        g2.setColor(DesignSystem.TEXT_PRIMARY);
        g2.setFont(DesignSystem.FONT_TITLE);
        FontMetrics fmVal = g2.getFontMetrics();
        g2.drawString(centerValue,
                chartX + innerOffset + (innerR - fmVal.stringWidth(centerValue)) / 2,
                chartY + innerOffset + (innerR / 2) + 16);

        int legendX = chartX + outerR + 24;
        int legendY = 24;
        idx = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            if (legendY + 18 > h) break;

            Color color = sliceColors.get(idx);
            double pct = (entry.getValue() / total) * 100.0;

            g2.setColor(color);
            g2.fillOval(legendX, legendY, 10, 10);

            g2.setColor(idx == hoveredSlice ? DesignSystem.TEXT_PRIMARY : DesignSystem.TEXT_SECONDARY);
            g2.setFont(idx == hoveredSlice ? DesignSystem.FONT_SMALL_BOLD : DesignSystem.FONT_SMALL);

            String text = entry.getKey();
            if (text.length() > 16) text = text.substring(0, 14) + "..";
            String line = String.format("%s (%.0f%%)", text, pct);
            g2.drawString(line, legendX + 16, legendY + 9);

            legendY += 22;
            idx++;
        }

        g2.dispose();
    }
}
