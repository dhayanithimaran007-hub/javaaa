package com.smartcampus.ui.admin;

import com.smartcampus.model.ActivityFeedItem;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

public class ActivityFeedPanel extends CardPanel {
    private final JPanel listContainer;

    public ActivityFeedPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titlePanel.setOpaque(false);

        JLabel pulseDot = new JLabel(VectorIcons.statusDot(DesignSystem.SUCCESS, 10));
        titlePanel.add(pulseDot);

        JLabel lblTitle = new JLabel("LIVE ACTIVITY STREAM");
        lblTitle.setFont(DesignSystem.FONT_CARD_LABEL);
        lblTitle.setForeground(DesignSystem.TEXT_PRIMARY);
        titlePanel.add(lblTitle);

        header.add(titlePanel, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Real-time campus operations events");
        lblSub.setFont(DesignSystem.FONT_TINY);
        lblSub.setForeground(DesignSystem.TEXT_MUTED);
        header.add(lblSub, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        listContainer = new JPanel();
        listContainer.setOpaque(false);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    public void updateFeed(List<ActivityFeedItem> items) {
        listContainer.removeAll();

        if (items == null || items.isEmpty()) {
            JLabel empty = new JLabel("No recent activity recorded.");
            empty.setFont(DesignSystem.FONT_SMALL);
            empty.setForeground(DesignSystem.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
            listContainer.add(empty);
        } else {
            for (ActivityFeedItem item : items) {
                listContainer.add(buildItemRow(item));
                listContainer.add(Box.createVerticalStrut(6));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel buildItemRow(ActivityFeedItem item) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                DesignSystem.applyQualityRendering(g2);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.setMaximumSize(new Dimension(800, 48));

        Color iconColor = switch (item.action()) {
            case "RESOLVE_COMPLAINT", "ADMIN_EMERGENCY_ACK", "MAINTENANCE_COMPLETED" -> DesignSystem.SUCCESS;
            case "REJECT_COMPLAINT", "SLA_BREACH", "SAFETY_INCIDENT_DETECTED", "ESCALATION_LEVEL_CHANGE" -> DesignSystem.CRITICAL;
            case "ASSIGN_STAFF", "SLA_WARNING", "EMERGENCY_INTERVENTION", "MAINTENANCE_SCHEDULED" -> DesignSystem.WARNING;
            default -> DesignSystem.ELECTRIC_BLUE;
        };

        Icon actionIcon = switch (item.action()) {
            case "SUBMIT_COMPLAINT" -> VectorIcons.of(VectorIcons.Kind.CLIPBOARD, iconColor, 15);
            case "ASSIGN_STAFF", "MAINTENANCE_SCHEDULED" -> VectorIcons.of(VectorIcons.Kind.WRENCH, iconColor, 15);
            case "UPDATE_PROGRESS" -> VectorIcons.of(VectorIcons.Kind.CLOCK, iconColor, 15);
            case "RESOLVE_COMPLAINT", "ADMIN_EMERGENCY_ACK", "RECOMMENDATION_APPROVED", "MAINTENANCE_COMPLETED" -> VectorIcons.check(iconColor, 15);
            case "REJECT_COMPLAINT", "RECOMMENDATION_DISMISSED" -> VectorIcons.cross(iconColor, 15);
            case "REOPEN_COMPLAINT" -> VectorIcons.refresh(iconColor, 15);
            case "SAFETY_INCIDENT_DETECTED", "SLA_WARNING", "SLA_BREACH", "ESCALATION_LEVEL_CHANGE" -> VectorIcons.warning(iconColor, 15);
            case "PREDICTIVE_ANALYSIS_RUN", "RISK_ASSESSMENT_CREATED" -> VectorIcons.chart(iconColor, 15);
            default -> VectorIcons.statusDot(iconColor, 9);
        };

        JLabel lblIcon = new JLabel(actionIcon);
        lblIcon.setPreferredSize(new Dimension(24, 24));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(lblIcon, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel actorLine = new JLabel(item.actorName() + " (" + item.actorRole() + ")");
        actorLine.setFont(DesignSystem.FONT_TINY);
        actorLine.setForeground(DesignSystem.TEXT_MUTED);

        String detailText = item.details() != null ? item.details() : item.action();
        if (detailText.length() > 50) detailText = detailText.substring(0, 48) + "..";
        JLabel detailLine = new JLabel(detailText);
        detailLine.setFont(DesignSystem.FONT_SMALL);
        detailLine.setForeground(item.isCritical() ? DesignSystem.CRITICAL : DesignSystem.TEXT_PRIMARY);

        center.add(actorLine);
        center.add(detailLine);
        row.add(center, BorderLayout.CENTER);

        JLabel lblTime = new JLabel(item.relativeTime());
        lblTime.setFont(DesignSystem.FONT_TINY);
        lblTime.setForeground(DesignSystem.TEXT_MUTED);
        row.add(lblTime, BorderLayout.EAST);

        return row;
    }
}
