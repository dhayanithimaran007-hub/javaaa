package com.smartcampus.ui.components;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class StatCard extends JPanel {
    private final JLabel lblValue;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;
    private final Color accentColor;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        this.accentColor = accentColor != null ? accentColor : DesignSystem.ELECTRIC_BLUE;
        setLayout(new BorderLayout(8, 6));
        setBackground(DesignSystem.CARD_BG);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        setPreferredSize(new Dimension(200, 105));
        setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(DesignSystem.FONT_CARD_LABEL);
        lblTitle.setForeground(DesignSystem.TEXT_MUTED);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(DesignSystem.FONT_CARD_VALUE);
        lblValue.setForeground(DesignSystem.TEXT_PRIMARY);

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(DesignSystem.FONT_SMALL);
        lblSubtitle.setForeground(DesignSystem.TEXT_SECONDARY);

        add(headerPanel, BorderLayout.NORTH);
        add(lblValue, BorderLayout.CENTER);
        add(lblSubtitle, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setValue(int count) {
        lblValue.setText(String.valueOf(count));
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        DesignSystem.applyQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

        g2.setColor(DesignSystem.CARD_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        g2.setColor(accentColor);
        g2.fillRoundRect(0, 4, 5, h - 9, 4, 4);

        g2.dispose();
        super.paintComponent(g);
    }
}
