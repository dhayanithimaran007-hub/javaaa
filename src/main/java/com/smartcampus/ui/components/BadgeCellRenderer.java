package com.smartcampus.ui.components;

import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.RiskLevel;
import com.smartcampus.model.enums.SlaStatus;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BadgeCellRenderer extends JLabel implements TableCellRenderer {
    private java.awt.Color currentBg;
    private java.awt.Color currentFg;

    public BadgeCellRenderer() {
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setBorder(new EmptyBorder(4, 10, 4, 10));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (value == null) {
            setText("");
            currentBg = table.getBackground();
            currentFg = table.getForeground();
            return this;
        }

        String text = value.toString();
        setText(text);

        if (value instanceof PriorityLevel prio) {
            currentBg = prio.getBadgeBackgroundColor();
            currentFg = prio.getBadgeTextColor();
            setToolTipText("Priority Level: " + prio.getLabel());
            return this;
        }
        if (value instanceof ComplaintStatus status) {
            currentBg = status.getBadgeBackgroundColor();
            currentFg = status.getBadgeTextColor();
            setToolTipText("Lifecycle Status: " + status.getDisplayName());
            return this;
        }
        if (value instanceof SlaStatus sla) {
            currentBg = sla.getBadgeBg();
            currentFg = sla.getBadgeText();
            setToolTipText("SLA Status: " + sla.getDisplayName());
            return this;
        }
        if (value instanceof EscalationLevel esc) {
            currentBg = esc.getBadgeBg();
            currentFg = esc.getBadgeText();
            setToolTipText("Escalation Tier: " + esc.getDisplayName());
            return this;
        }
        if (value instanceof RiskLevel risk) {
            currentBg = risk.getBadgeBg();
            currentFg = risk.getBadgeText();
            setToolTipText("Predictive Risk: " + risk.getLabel());
            return this;
        }
        if (value instanceof RecommendationStatus rec) {
            currentBg = rec.getBadgeBg();
            currentFg = rec.getBadgeText();
            setToolTipText("Recommendation Status: " + rec.getDisplayName());
            return this;
        }

        try {
            PriorityLevel prio = PriorityLevel.fromString(text);
            currentBg = prio.getBadgeBackgroundColor();
            currentFg = prio.getBadgeTextColor();
            setToolTipText("Priority Level: " + prio.getLabel());
            return this;
        } catch (Exception ignored) {}

        try {
            ComplaintStatus status = ComplaintStatus.fromString(text);
            currentBg = status.getBadgeBackgroundColor();
            currentFg = status.getBadgeTextColor();
            setToolTipText("Lifecycle Status: " + status.getDisplayName());
            return this;
        } catch (Exception ignored) {}

        currentBg = new java.awt.Color(241, 245, 249);
        currentFg = new java.awt.Color(71, 85, 105);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth() - 8;
        int height = getHeight() - 6;
        int x = 4;
        int y = 3;

        g2.setColor(currentBg != null ? currentBg : getBackground());
        g2.fillRoundRect(x, y, width, height, 12, 12);

        setForeground(currentFg != null ? currentFg : getForeground());
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 16, 85), Math.max(d.height, 26));
    }
}
