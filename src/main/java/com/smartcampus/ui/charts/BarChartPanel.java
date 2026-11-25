package com.smartcampus.ui.charts;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BarChartPanel extends JPanel {
    private static final double LABEL_ROTATION_RAD = -Math.toRadians(40.0);
    private static final double COS_ANGLE = Math.cos(Math.toRadians(40.0));
    private static final double SIN_ANGLE = Math.sin(Math.toRadians(40.0));

    private final Map<String, Double> data = new LinkedHashMap<>();
    private final Map<String, Color> barColors = new LinkedHashMap<>();
    private final Map<String, String> customTooltips = new LinkedHashMap<>();
    private Color defaultBarColor = DesignSystem.ELECTRIC_BLUE;
    private int hoveredBar = -1;

    public BarChartPanel() {
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkHover(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredBar != -1) {
                    hoveredBar = -1;
                    setToolTipText(null);
                    repaint();
                }
            }
        });
    }

    public synchronized void setData(Map<String, ? extends Number> rawData) {
        data.clear();
        barColors.clear();
        customTooltips.clear();
        hoveredBar = -1;
        setToolTipText(null);
        if (rawData != null) {
            for (Map.Entry<String, ? extends Number> entry : rawData.entrySet()) {
                double val = entry.getValue() != null ? entry.getValue().doubleValue() : 0.0;
                data.put(entry.getKey(), val);
            }
        }
        repaint();
    }

    public synchronized void setBarColor(String key, Color color) {
        barColors.put(key, color);
        repaint();
    }

    public synchronized void setBarTooltip(String key, String tooltip) {
        if (key == null) {
            return;
        }
        if (tooltip == null) {
            customTooltips.remove(key);
        } else {
            customTooltips.put(key, tooltip);
        }
    }

    public void setDefaultBarColor(Color color) {
        this.defaultBarColor = color;
        repaint();
    }

    synchronized void checkHover(int mx, int my) {
        if (data.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        ChartLayout layout = computeLayout(getWidth(), getHeight());
        if (layout.bars().isEmpty()) {
            return;
        }

        if (mx >= layout.padLeft() && mx <= layout.padLeft() + layout.chartW()
                && my >= layout.padTop() && my <= layout.padTop() + layout.chartH() + 10) {
            for (BarGeometry bar : layout.bars()) {
                if (mx >= bar.slotLeft() && mx < bar.slotRight()) {
                    hoveredBar = bar.index();
                    String tip = customTooltips.get(bar.fullKey());
                    if (tip == null) {
                        tip = String.format("%s: %.0f", bar.fullKey(), bar.value());
                    }
                    setToolTipText(tip);
                    repaint();
                    return;
                }
            }
        }

        if (hoveredBar != -1) {
            hoveredBar = -1;
            setToolTipText(null);
            repaint();
        }
    }

    synchronized ChartLayout computeLayout(int w, int h) {
        if (data.isEmpty() || w <= 80 || h <= 80) {
            return new ChartLayout(0, 0, 0, 0, 0, 0, 4, DesignSystem.FONT_TINY, List.of());
        }

        double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxVal <= 0) {
            maxVal = 1.0;
        }
        int gridCeiling = (int) Math.ceil(maxVal * 1.15);
        if (gridCeiling < 4) {
            gridCeiling = 4;
        }

        FontMetrics fmTiny = getFontMetrics(DesignSystem.FONT_TINY);
        int maxYLabelW = fmTiny.stringWidth(String.valueOf(gridCeiling));

        int numBars = data.size();
        int padRight = 18;
        int padTop = 22;

        int estChartW = Math.max(80, w - (maxYLabelW + 24) - padRight);
        int estSlotW = Math.max(12, estChartW / numBars);

        int fontSize = (estSlotW >= 46 && numBars <= 6) ? 11 : 10;
        Font labelFont = new Font(DesignSystem.FONT_FAMILY, Font.PLAIN, fontSize);
        FontMetrics fmLabel = getFontMetrics(labelFont);
        while (fontSize > 8 && estSlotW * SIN_ANGLE <= fmLabel.getHeight() + 1.0) {
            fontSize--;
            labelFont = new Font(DesignSystem.FONT_FAMILY, Font.PLAIN, fontSize);
            fmLabel = getFontMetrics(labelFont);
        }

        int maxRawLabelW = 0;
        String firstKey = data.keySet().iterator().next();
        for (String key : data.keySet()) {
            maxRawLabelW = Math.max(maxRawLabelW, fmLabel.stringWidth(key != null ? key : ""));
        }

        int minChartH = Math.max(58, (int) Math.round(h * 0.48));
        int maxAllowedBottom = Math.max(54, h - padTop - minChartH);
        int topOffsetY = (int) Math.ceil(fmLabel.getAscent() * COS_ANGLE) + 7;
        int neededBottom = topOffsetY + (int) Math.ceil(maxRawLabelW * SIN_ANGLE + fmLabel.getDescent() * COS_ANGLE) + 6;
        int padBottom = Math.max(56, Math.min(maxAllowedBottom, neededBottom));
        int chartH = Math.max(40, h - padTop - padBottom);

        int firstLabelW = fmLabel.stringWidth(firstKey != null ? firstKey : "");
        int desiredLeftForFirst = (int) Math.ceil(firstLabelW * COS_ANGLE + fmLabel.getAscent() * SIN_ANGLE) + 8;
        int basePadLeft = maxYLabelW + 18;
        int tentativeSlotW = Math.max(12, (w - basePadLeft - padRight) / numBars);
        int extraLeft = Math.max(0, desiredLeftForFirst - tentativeSlotW / 2 - basePadLeft);
        int maxPadLeft = Math.max(basePadLeft, Math.min(96, w / 5));
        int padLeft = Math.min(maxPadLeft, basePadLeft + extraLeft);

        int chartW = Math.max(40, w - padLeft - padRight);
        int slotWidth = Math.max(1, chartW / numBars);
        int barWidth = Math.max(6, Math.min((int) (slotWidth * 0.54), 46));
        int axisY = padTop + chartH;
        int anchorY = axisY + topOffsetY;

        double maxDiagVert = Math.max(18.0, (h - 4.0 - anchorY - fmLabel.getDescent() * COS_ANGLE) / SIN_ANGLE);
        double maxDiagSpacing = Math.max(18.0, slotWidth * COS_ANGLE + (h - anchorY) * SIN_ANGLE);

        List<BarGeometry> bars = new ArrayList<>(numBars);
        int idx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            String fullKey = entry.getKey() != null ? entry.getKey() : "";
            double val = entry.getValue();
            int slotLeft = padLeft + (idx * slotWidth);
            int slotRight = (idx == numBars - 1) ? (padLeft + chartW) : (slotLeft + slotWidth);
            int barX = slotLeft + (slotWidth - barWidth) / 2;
            int barCenterX = barX + barWidth / 2;
            int barHeight = val > 0 ? Math.max((int) Math.round((val / gridCeiling) * chartH), 3) : 0;
            int barY = axisY - barHeight;

            double maxDiagLeft = Math.max(18.0, (barCenterX - 4.0 - fmLabel.getAscent() * SIN_ANGLE) / COS_ANGLE);
            int maxLabelPx = (int) Math.floor(Math.min(maxDiagVert, Math.min(maxDiagLeft, maxDiagSpacing + 220.0)));
            String displayLabel = abbreviateToWidth(fullKey, fmLabel, maxLabelPx);
            int dispW = fmLabel.stringWidth(displayLabel);

            AffineTransform tx = AffineTransform.getTranslateInstance(barCenterX, anchorY);
            tx.rotate(LABEL_ROTATION_RAD);
            Rectangle2D localBox = new Rectangle2D.Double(-dispW, -fmLabel.getAscent(), dispW, fmLabel.getHeight());
            Shape labelBounds = tx.createTransformedShape(localBox);

            bars.add(new BarGeometry(
                    idx,
                    fullKey,
                    displayLabel,
                    val,
                    slotLeft,
                    slotRight,
                    barX,
                    barY,
                    barWidth,
                    barHeight,
                    barCenterX,
                    anchorY,
                    labelBounds
            ));
            idx++;
        }

        return new ChartLayout(padLeft, padRight, padTop, padBottom, chartW, chartH, gridCeiling, labelFont, bars);
    }

    private static String abbreviateToWidth(String text, FontMetrics fm, int maxWidthPx) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidthPx) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = fm.stringWidth(ellipsis);
        if (maxWidthPx <= ellipsisW) {
            return ellipsis;
        }
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end)) + ellipsisW > maxWidthPx) {
            end--;
        }
        return text.substring(0, end).trim() + ellipsis;
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
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

        ChartLayout layout = computeLayout(w, h);
        int padLeft = layout.padLeft();
        int padTop = layout.padTop();
        int chartW = layout.chartW();
        int chartH = layout.chartH();
        int axisY = padTop + chartH;
        int gridCeiling = layout.gridCeiling();

        g2.setFont(DesignSystem.FONT_TINY);
        FontMetrics fmTiny = g2.getFontMetrics();

        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            int y = padTop + chartH - (i * chartH / steps);
            int val = (gridCeiling * i) / steps;
            g2.setColor(i == 0 ? DesignSystem.CARD_BORDER : new Color(241, 245, 249));
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            g2.setColor(DesignSystem.TEXT_MUTED);
            String strVal = String.valueOf(val);
            g2.drawString(strVal, padLeft - fmTiny.stringWidth(strVal) - 8, y + 4);
        }

        Font labelFont = layout.labelFont();
        for (BarGeometry bar : layout.bars()) {
            Color c = barColors.getOrDefault(bar.fullKey(), defaultBarColor);
            if (bar.index() == hoveredBar) {
                c = c.brighter();
            }

            if (bar.barHeight() > 0) {
                g2.setColor(c);
                g2.fillRoundRect(bar.barX(), bar.barY(), bar.barWidth(), bar.barHeight(), 6, 6);

                g2.setFont(DesignSystem.FONT_TINY);
                g2.setColor(DesignSystem.TEXT_PRIMARY);
                String valStr = String.valueOf((int) Math.round(bar.value()));
                int valX = bar.barX() + (bar.barWidth() - fmTiny.stringWidth(valStr)) / 2;
                g2.drawString(valStr, valX, bar.barY() - 4);
            }

            g2.setColor(DesignSystem.CARD_BORDER);
            g2.drawLine(bar.barCenterX(), axisY, bar.barCenterX(), axisY + 4);

            g2.setFont(labelFont);
            g2.setColor(bar.index() == hoveredBar ? DesignSystem.TEXT_PRIMARY : DesignSystem.TEXT_SECONDARY);
            FontMetrics fmLbl = g2.getFontMetrics();
            int lblW = fmLbl.stringWidth(bar.displayLabel());

            AffineTransform saved = g2.getTransform();
            g2.translate(bar.barCenterX(), bar.anchorY());
            g2.rotate(LABEL_ROTATION_RAD);
            g2.drawString(bar.displayLabel(), -lblW, 0);
            g2.setTransform(saved);
        }

        g2.dispose();
    }

    record BarGeometry(
            int index,
            String fullKey,
            String displayLabel,
            double value,
            int slotLeft,
            int slotRight,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            int barCenterX,
            int anchorY,
            Shape labelBounds
    ) {}

    record ChartLayout(
            int padLeft,
            int padRight,
            int padTop,
            int padBottom,
            int chartW,
            int chartH,
            int gridCeiling,
            Font labelFont,
            List<BarGeometry> bars
    ) {}
}
