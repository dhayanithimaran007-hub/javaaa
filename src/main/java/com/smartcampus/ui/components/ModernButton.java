package com.smartcampus.ui.components;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {
    public enum ButtonType {
        PRIMARY(new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE),
        SECONDARY(new Color(241, 245, 249), new Color(226, 232, 240), new Color(51, 65, 85)),
        SUCCESS(new Color(22, 163, 74), new Color(21, 128, 61), Color.WHITE),
        DANGER(new Color(220, 38, 38), new Color(185, 28, 28), Color.WHITE);

        final Color normalBg;
        final Color hoverBg;
        final Color textColor;

        ButtonType(Color normalBg, Color hoverBg, Color textColor) {
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.textColor = textColor;
        }
    }

    private final ButtonType type;
    private boolean isHovered = false;

    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public ModernButton(String text, ButtonType type) {
        super(text);
        this.type = type != null ? type : ButtonType.PRIMARY;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!isEnabled()) {
            g2.setColor(new Color(203, 213, 225));
        } else if (isHovered) {
            g2.setColor(type.hoverBg);
        } else {
            g2.setColor(type.normalBg);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        if (type == ButtonType.SECONDARY && isEnabled()) {
            g2.setColor(new Color(203, 213, 225));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
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
