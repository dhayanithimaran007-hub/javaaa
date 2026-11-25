package com.smartcampus.ui.components;

import com.smartcampus.model.User;
import com.smartcampus.security.UserSession;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModernSidebar extends JPanel {
    private static final int WIDTH_EXPANDED = 245;
    private static final int WIDTH_COLLAPSED = 72;

    private boolean isCollapsed = false;
    private final JPanel navItemsContainer;
    private final List<NavButton> navButtons = new ArrayList<>();
    private NavButton activeButton = null;

    private final JLabel lblAppTitle;
    private final JLabel lblAppSub;
    private final JButton btnToggle;
    private final JPanel userProfilePanel;
    private final JLabel lblUserName;
    private final JLabel lblUserRole;

    private Consumer<String> onNavSelected;
    private Runnable onLogoutRequested;

    public static class NavItem {
        public final String id;
        public final Icon icon;
        public final String label;

        public NavItem(String id, Icon icon, String label) {
            this.id = id;
            this.icon = icon;
            this.label = label;
        }
    }

    public ModernSidebar(String appTitle, String appSubtitle) {
        setLayout(new BorderLayout());
        setBackground(DesignSystem.MIDNIGHT_NAVY);
        setPreferredSize(new Dimension(WIDTH_EXPANDED, 800));
        setMinimumSize(new Dimension(WIDTH_COLLAPSED, 400));

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(22, 16, 20, 16));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        lblAppTitle = new JLabel(appTitle);
        lblAppTitle.setFont(DesignSystem.FONT_TITLE);
        lblAppTitle.setForeground(Color.WHITE);

        lblAppSub = new JLabel(appSubtitle.toUpperCase());
        lblAppSub.setFont(DesignSystem.FONT_CARD_LABEL);
        lblAppSub.setForeground(DesignSystem.ELECTRIC_BLUE);

        titlePanel.add(lblAppSub);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(lblAppTitle);

        btnToggle = new JButton(VectorIcons.of(VectorIcons.Kind.MENU, 16));
        btnToggle.setForeground(DesignSystem.TEXT_MUTED);
        btnToggle.setPreferredSize(new Dimension(28, 28));
        btnToggle.setContentAreaFilled(false);
        btnToggle.setBorderPainted(false);
        btnToggle.setFocusPainted(false);
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggle.setToolTipText("Toggle Sidebar");
        btnToggle.addActionListener(e -> toggleCollapse());

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(btnToggle, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        navItemsContainer = new JPanel();
        navItemsContainer.setOpaque(false);
        navItemsContainer.setLayout(new BoxLayout(navItemsContainer, BoxLayout.Y_AXIS));
        navItemsContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(navItemsContainer, BorderLayout.CENTER);

        userProfilePanel = new JPanel(new BorderLayout(10, 0));
        userProfilePanel.setOpaque(false);
        userProfilePanel.setBorder(new EmptyBorder(16, 12, 18, 12));

        User currentUser = UserSession.getCurrentUser();
        String name = (currentUser != null) ? currentUser.getFullName() : "Administrator";
        String role = (currentUser != null) ? currentUser.getRole().name() : "ADMIN";

        JLabel avatar = new JLabel(name.substring(0, Math.min(1, name.length())).toUpperCase()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                DesignSystem.applyQualityRendering(g2);
                g2.setColor(DesignSystem.DEEP_NAVY);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(DesignSystem.ELECTRIC_BLUE);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setFont(DesignSystem.FONT_BODY_BOLD);
        avatar.setForeground(Color.WHITE);
        userProfilePanel.add(avatar, BorderLayout.WEST);

        JPanel userInfo = new JPanel();
        userInfo.setOpaque(false);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));

        lblUserName = new JLabel(name);
        lblUserName.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblUserName.setForeground(Color.WHITE);

        lblUserRole = new JLabel(role);
        lblUserRole.setFont(DesignSystem.FONT_TINY);
        lblUserRole.setForeground(DesignSystem.TEXT_MUTED);

        userInfo.add(lblUserName);
        userInfo.add(lblUserRole);
        userProfilePanel.add(userInfo, BorderLayout.CENTER);

        JButton btnLogout = new JButton(VectorIcons.of(VectorIcons.Kind.LOGOUT, DesignSystem.CRITICAL, 16));
        btnLogout.setPreferredSize(new Dimension(28, 28));
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setToolTipText("Sign Out");
        btnLogout.addActionListener(e -> {
            if (onLogoutRequested != null) onLogoutRequested.run();
        });
        userProfilePanel.add(btnLogout, BorderLayout.EAST);

        add(userProfilePanel, BorderLayout.SOUTH);
    }

    public void addNavItem(String id, Icon icon, String label) {
        NavButton btn = new NavButton(id, icon, label);
        navButtons.add(btn);
        navItemsContainer.add(btn);
        navItemsContainer.add(Box.createVerticalStrut(4));

        if (activeButton == null) {
            setActiveItem(id);
        }
    }

    public void setNavSelectedListener(Consumer<String> listener) {
        this.onNavSelected = listener;
    }

    public void setLogoutListener(Runnable logoutListener) {
        this.onLogoutRequested = logoutListener;
    }

    public void setActiveItem(String id) {
        for (NavButton b : navButtons) {
            if (b.id.equals(id)) {
                if (activeButton != null) activeButton.setActive(false);
                activeButton = b;
                b.setActive(true);
            } else {
                b.setActive(false);
            }
        }
    }

    public void setBadge(String id, int count) {
        for (NavButton b : navButtons) {
            if (b.id.equals(id)) {
                b.setBadgeCount(count);
                break;
            }
        }
    }

    public void toggleCollapse() {
        isCollapsed = !isCollapsed;
        int targetWidth = isCollapsed ? WIDTH_COLLAPSED : WIDTH_EXPANDED;
        setPreferredSize(new Dimension(targetWidth, getHeight()));

        lblAppTitle.setVisible(!isCollapsed);
        lblAppSub.setVisible(!isCollapsed);
        lblUserName.setVisible(!isCollapsed);
        lblUserRole.setVisible(!isCollapsed);

        for (NavButton b : navButtons) {
            b.setCollapsed(isCollapsed);
        }

        revalidate();
        repaint();
    }

    private class NavButton extends JPanel {
        final String id;
        final Icon icon;
        final String label;
        private boolean isActive = false;
        private boolean isHovered = false;
        private int badgeCount = 0;

        private final JLabel lblIcon;
        private final JLabel lblText;
        private final JLabel lblBadge;

        NavButton(String id, Icon icon, String label) {
            this.id = id;
            this.icon = icon;
            this.label = label;

            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setMaximumSize(new Dimension(225, 42));
            setToolTipText(label);

            lblIcon = new JLabel(icon);
            lblIcon.setForeground(DesignSystem.TEXT_MUTED);
            lblIcon.setPreferredSize(new Dimension(24, 20));
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
            add(lblIcon, BorderLayout.WEST);

            lblText = new JLabel(label);
            lblText.setFont(DesignSystem.FONT_BODY_BOLD);
            lblText.setForeground(new Color(203, 213, 225));
            add(lblText, BorderLayout.CENTER);

            lblBadge = new JLabel("");
            lblBadge.setFont(DesignSystem.FONT_TINY);
            lblBadge.setForeground(Color.WHITE);
            lblBadge.setOpaque(false);
            add(lblBadge, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    setActiveItem(id);
                    if (onNavSelected != null) {
                        onNavSelected.accept(id);
                    }
                }
            });
        }

        void setActive(boolean active) {
            this.isActive = active;
            lblText.setForeground(active ? Color.WHITE : new Color(203, 213, 225));
            lblIcon.setForeground(active ? DesignSystem.ELECTRIC_BLUE : DesignSystem.TEXT_MUTED);
            repaint();
        }

        void setCollapsed(boolean collapsed) {
            lblText.setVisible(!collapsed);
            lblBadge.setVisible(!collapsed && badgeCount > 0);
            setMaximumSize(new Dimension(collapsed ? 52 : 225, 42));
        }

        void setBadgeCount(int count) {
            this.badgeCount = count;
            if (count > 0) {
                lblBadge.setText(" " + count + " ");
                lblBadge.setVisible(!isCollapsed);
            } else {
                lblBadge.setText("");
                lblBadge.setVisible(false);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            DesignSystem.applyQualityRendering(g2);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                g2.setColor(DesignSystem.DEEP_NAVY);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                g2.setColor(DesignSystem.ELECTRIC_BLUE);
                g2.fillRoundRect(0, 4, 4, h - 8, 3, 3);
            } else if (isHovered) {
                g2.setColor(DesignSystem.NAVY_HOVER);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            }

            if (badgeCount > 0 && !isCollapsed) {
                int bw = 20;
                int bh = 16;
                int bx = w - bw - 10;
                int by = (h - bh) / 2;
                g2.setColor(DesignSystem.CRITICAL);
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
