package com.smartcampus.ui.components;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    public enum ButtonType {
        PRIMARY(DesignSystem.ELECTRIC_BLUE, DesignSystem.BLUE_HOVER, Color.WHITE, null),
        SECONDARY(new Color(241, 245, 249), new Color(226, 232, 240), DesignSystem.TEXT_PRIMARY, DesignSystem.CARD_BORDER),
        SUCCESS(DesignSystem.SUCCESS, new Color(18, 161, 104), Color.WHITE, null),
        DANGER(DesignSystem.CRITICAL, new Color(219, 44, 58), Color.WHITE, null),
        WARNING(DesignSystem.WARNING, new Color(224, 148, 24), Color.WHITE, null),
        ACCENT(DesignSystem.VIOLET, new Color(117, 85, 235), Color.WHITE, null),
        GHOST(new Color(0, 0, 0, 0), new Color(241, 245, 249), DesignSystem.TEXT_SECONDARY, DesignSystem.CARD_BORDER);

        final Color normalBg;
        final Color hoverBg;
        final Color textColor;
        final Color borderColor;

        ButtonType(Color normalBg, Color hoverBg, Color textColor, Color borderColor) {
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.textColor = textColor;
            this.borderColor = borderColor;
        }

        public Color getTextColor() { return textColor; }
    }

    private ButtonType type;
    private boolean isHovered = false;
    private int cornerRadius = 8;

    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public ModernButton(String text, ButtonType type) {
        super(text);
        this.type = type != null ? type : ButtonType.PRIMARY;
        initButton();
    }

    private void initButton() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(DesignSystem.FONT_BODY_BOLD);
        setForeground(this.type.textColor);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setButtonType(ButtonType type) {
        this.type = type;
        setForeground(type.textColor);
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        DesignSystem.applyQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        if (!isEnabled()) {
            g2.setColor(new Color(226, 232, 240));
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
            setForeground(DesignSystem.TEXT_MUTED);
        } else {
            g2.setColor(isHovered ? type.hoverBg : type.normalBg);
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
            setForeground(type.textColor);

            if (type.borderColor != null) {
                g2.setColor(type.borderColor);
                g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 90), Math.max(d.height, 34));
    }
}
