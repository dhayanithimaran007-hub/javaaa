package com.smartcampus.ui.components;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

public class ToastNotification extends JWindow {

    public enum Type {
        SUCCESS(DesignSystem.SUCCESS, VectorIcons.Kind.CHECK),
        INFO(DesignSystem.ELECTRIC_BLUE, VectorIcons.Kind.INFO),
        WARNING(DesignSystem.WARNING, VectorIcons.Kind.ALERT),
        ERROR(DesignSystem.CRITICAL, VectorIcons.Kind.CROSS);

        final Color color;
        final VectorIcons.Kind iconKind;

        Type(Color color, VectorIcons.Kind iconKind) {
            this.color = color;
            this.iconKind = iconKind;
        }
    }

    private final Color typeColor;

    public ToastNotification(JFrame owner, Type type, String title, String message) {
        super(owner);
        this.typeColor = type.color;

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                DesignSystem.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                g2.setColor(DesignSystem.CARD_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                g2.setColor(typeColor);
                g2.fillRoundRect(0, 0, 6, h - 1, 6, 6);

                g2.dispose();
            }
        };

        content.setOpaque(false);
        content.setLayout(new BorderLayout(12, 6));
        content.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel lblIcon = new JLabel(VectorIcons.of(type.iconKind, type.color, 20));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(lblIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        if (title != null && !title.isBlank()) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(DesignSystem.FONT_SMALL_BOLD);
            lblTitle.setForeground(DesignSystem.TEXT_PRIMARY);
            textPanel.add(lblTitle);
            textPanel.add(Box.createVerticalStrut(2));
        }

        JLabel lblMsg = new JLabel("<html><body style='width: 260px;'>" + message + "</body></html>");
        lblMsg.setFont(DesignSystem.FONT_SMALL);
        lblMsg.setForeground(DesignSystem.TEXT_SECONDARY);
        textPanel.add(lblMsg);

        content.add(textPanel, BorderLayout.CENTER);
        setContentPane(content);

        pack();
        setSize(Math.max(getWidth(), 320), Math.max(getHeight(), 56));

        positionWindow(owner);

        Timer timer = new Timer(3500, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void positionWindow(JFrame owner) {
        if (owner != null && owner.isVisible()) {
            Point loc = owner.getLocationOnScreen();
            int x = loc.x + owner.getWidth() - getWidth() - 25;
            int y = loc.y + owner.getHeight() - getHeight() - 35;
            setLocation(Math.max(x, 10), Math.max(y, 10));
        } else {
            Dimension scr = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(scr.width - getWidth() - 30, scr.height - getHeight() - 50);
        }
    }

    public static void show(JFrame owner, Type type, String title, String message) {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        javax.swing.SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(owner, type, title, message);
            toast.setVisible(true);
        });
    }

    public static void showSuccess(JFrame owner, String message) {
        show(owner, Type.SUCCESS, "Success", message);
    }

    public static void showInfo(JFrame owner, String message) {
        show(owner, Type.INFO, "Notice", message);
    }

    public static void showWarning(JFrame owner, String message) {
        show(owner, Type.WARNING, "Warning", message);
    }

    public static void showError(JFrame owner, String message) {
        show(owner, Type.ERROR, "Error", message);
    }
}
