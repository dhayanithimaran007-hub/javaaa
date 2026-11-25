package com.smartcampus.ui.components;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;

public class CardPanel extends JPanel {
    private int cornerRadius = 12;
    private Color backgroundColor = DesignSystem.CARD_BG;
    private Color borderColor = DesignSystem.CARD_BORDER;
    private boolean drawBorder = true;

    public CardPanel() {
        this(new BorderLayout());
    }

    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public CardPanel(int cornerRadius, Color backgroundColor, Color borderColor) {
        this(new BorderLayout());
        this.cornerRadius = cornerRadius;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        DesignSystem.applyQualityRendering(g2);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(backgroundColor != null ? backgroundColor : getBackground());
        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        if (drawBorder && borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Insets getInsets() {
        Insets in = super.getInsets();

        return new Insets(Math.max(in.top, 8), Math.max(in.left, 8), Math.max(in.bottom, 8), Math.max(in.right, 8));
    }
}
