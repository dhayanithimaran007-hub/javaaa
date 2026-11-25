package com.smartcampus.ui.charts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarChartPanelLayoutTest {

    @Test
    @DisplayName("3, 5, and 10 buildings with long names render without label overlap or clipping across sizes")
    void testNoLabelOverlapOrClippingAcrossCountsAndSizes() {
        List<Map<String, Double>> datasets = List.of(
                buildDataset(3),
                buildDataset(5),
                buildDataset(10)
        );
        List<Dimension> sizes = List.of(
                new Dimension(360, 185),
                new Dimension(480, 215),
                new Dimension(700, 260)
        );

        for (Map<String, Double> dataset : datasets) {
            BarChartPanel chart = new BarChartPanel();
            chart.setData(dataset);

            for (Dimension size : sizes) {
                chart.setSize(size);
                BarChartPanel.ChartLayout layout = chart.computeLayout(size.width, size.height);
                List<BarChartPanel.BarGeometry> bars = layout.bars();
                assertEquals(dataset.size(), bars.size(), "All buildings must remain present in chart");

                for (int i = 0; i < bars.size(); i++) {
                    BarChartPanel.BarGeometry bar = bars.get(i);
                    Rectangle2D bounds = bar.labelBounds().getBounds2D();
                    assertTrue(bounds.getMinX() >= 0,
                            "Label clipped left at " + size + " for " + bar.fullKey() + ": minX=" + bounds.getMinX());
                    assertTrue(bounds.getMaxX() <= size.width,
                            "Label clipped right at " + size + " for " + bar.fullKey() + ": maxX=" + bounds.getMaxX());
                    assertTrue(bounds.getMaxY() <= size.height,
                            "Label clipped bottom at " + size + " for " + bar.fullKey() + ": maxY=" + bounds.getMaxY());
                    assertTrue(bounds.getMinY() >= layout.padTop() + layout.chartH(),
                            "Label intrudes into plot area at " + size + " for " + bar.fullKey());

                    if (i > 0) {
                        Area prevArea = new Area(bars.get(i - 1).labelBounds());
                        Area curArea = new Area(bar.labelBounds());
                        prevArea.intersect(curArea);
                        assertTrue(prevArea.isEmpty(),
                                "Rotated labels overlap between '" + bars.get(i - 1).fullKey()
                                        + "' and '" + bar.fullKey() + "' at " + size);
                    }
                }

                BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                chart.paintComponent(g2);
                g2.dispose();
            }
        }
    }

    @Test
    @DisplayName("Hovering over each bar displays complete building name and risk metrics tooltip")
    void testRichTooltipContentOnHover() {
        BarChartPanel chart = new BarChartPanel();
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Science & Innovation Research Block A", 85.0);
        data.put("Central Library & Digital Learning Commons", 62.0);
        data.put("Mechanical Engineering Workshop Wing", 34.0);
        chart.setData(data);

        chart.setBarTooltip("Science & Innovation Research Block A",
                "<html>Building: <b>Science &amp; Innovation Research Block A</b><br>Risk Score: <b>85</b><br>Classification: <b>CRITICAL</b><br>Historical Complaints: <b>9</b></html>");
        chart.setBarTooltip("Central Library & Digital Learning Commons",
                "<html>Building: <b>Central Library &amp; Digital Learning Commons</b><br>Risk Score: <b>62</b><br>Classification: <b>HIGH</b><br>Historical Complaints: <b>6</b></html>");
        chart.setBarTooltip("Mechanical Engineering Workshop Wing",
                "<html>Building: <b>Mechanical Engineering Workshop Wing</b><br>Risk Score: <b>34</b><br>Classification: <b>MODERATE</b><br>Historical Complaints: <b>3</b></html>");

        chart.setSize(480, 215);
        BarChartPanel.ChartLayout layout = chart.computeLayout(480, 215);
        for (BarChartPanel.BarGeometry bar : layout.bars()) {
            int hoverY = layout.padTop() + Math.max(10, layout.chartH() / 2);
            chart.checkHover(bar.barCenterX(), hoverY);
            String tip = chart.getToolTipText();
            assertNotNull(tip, "Tooltip must be populated for " + bar.fullKey());
            String unescaped = tip.replace("&amp;", "&");
            assertTrue(unescaped.contains(bar.fullKey()), "Tooltip must retain complete building name: " + tip);
            assertTrue(unescaped.contains("Risk Score:"), "Tooltip must show Risk Score: " + tip);
            assertTrue(unescaped.contains("Classification:"), "Tooltip must show Classification: " + tip);
            assertTrue(unescaped.contains("Historical Complaints:"), "Tooltip must show Historical Complaints: " + tip);
        }
    }

    private static Map<String, Double> buildDataset(int count) {
        String[] sampleNames = {
                "Science Block A",
                "Main Academic Block",
                "Central Library & Digital Commons",
                "Mechanical Engineering Block",
                "North Campus Student Hostel",
                "Advanced Computer Science Center",
                "Administrative & Examination Tower",
                "Biotechnology Research Laboratory",
                "Electrical & High-Voltage Substation",
                "Multipurpose Indoor Sports Complex"
        };
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            map.put(sampleNames[i], 90.0 - (i * 6.5));
        }
        return map;
    }
}
