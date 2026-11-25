package com.smartcampus.ui;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.UserService;
import com.smartcampus.ui.admin.AdminDashboard;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.DatabaseConnectionStatusDialog;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.staff.StaffDashboard;
import com.smartcampus.ui.student.StudentDashboard;
import com.smartcampus.ui.theme.DesignSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoginFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LoginFrame.class);

    private record DemoAccount(String label, String email, String password) {
    }

    private static final List<DemoAccount> DEFAULT_DEMO_ACCOUNTS = List.of(
            new DemoAccount("Administrator", "admin@campus.edu", "Admin@123"),
            new DemoAccount("Electrician", "electrician@campus.edu", "Staff@123"),
            new DemoAccount("Plumber", "plumber@campus.edu", "Staff@123"),
            new DemoAccount("Network Technician", "karthik@campus.edu", "Staff@123"),
            new DemoAccount("Carpenter", "arun.carpenter@campus.edu", "Staff@123"),
            new DemoAccount("General Maintenance", "vikram.maintenance@campus.edu", "Staff@123"),
            new DemoAccount("Student (Alice)", "alice@student.campus.edu", "Student@123"),
            new DemoAccount("Student (Bob)", "bob@student.campus.edu", "Student@123"));

    private final JTextField txtEmail;
    private final JPasswordField txtPassword;
    private final JButton btnTogglePassword;
    private boolean passwordVisible = false;

    private final JProgressBar progressBar;
    private final ModernButton btnLogin;
    private final ModernButton btnRegister;
    private final ModernButton btnDbConfig;
    private final JComboBox<String> cmbQuickFill;
    private final JLabel lblDbTelemetry;

    private final UserService userService = new UserService();
    private final List<DemoAccount> demoAccounts;

    public LoginFrame() {
        super("Smart Campus Operations Platform - Enterprise Authentication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 660);
        setMinimumSize(new Dimension(860, 580));
        setLocationRelativeTo(null);
        setResizable(true);
        setLayout(new BorderLayout());

        JPanel leftBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                DesignSystem.applyQualityRendering(g2);
                g2.setColor(DesignSystem.MIDNIGHT_NAVY);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(71, 118, 255, 18));
                g2.fillOval(-60, -60, 260, 260);
                g2.fillOval(getWidth() - 140, getHeight() - 140, 240, 240);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        leftBanner.setPreferredSize(new Dimension(340, 660));
        leftBanner.setMinimumSize(new Dimension(300, 520));
        leftBanner.setOpaque(false);
        leftBanner.setLayout(new BoxLayout(leftBanner, BoxLayout.Y_AXIS));
        leftBanner.setBorder(BorderFactory.createEmptyBorder(44, 30, 36, 30));

        JLabel appBadge = new JLabel("CAMPUS OPERATIONS & INTELLIGENCE");
        appBadge.setFont(DesignSystem.FONT_CARD_LABEL);
        appBadge.setForeground(DesignSystem.ELECTRIC_BLUE);

        JLabel title1 = new JLabel("Smart Campus");
        title1.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title1.setForeground(Color.WHITE);

        JLabel title2 = new JLabel("Operations Platform");
        title2.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        title2.setForeground(new Color(226, 232, 240));

        JLabel desc = new JLabel(
                "<html><body style='width: 245px; color: #94a3b8; font-size: 11px; margin-top: 14px; line-height: 1.6;'>"
                        +
                        "&bull; Automated multi-factor priority scoring engine<br>" +
                        "&bull; Real-time technician dispatch & workload optimization<br>" +
                        "&bull; Campus safety hazard escalation & life-safety protocol<br>" +
                        "&bull; Complete cryptographic audit log & SLA telemetry" +
                        "</body></html>");

        JLabel version = new JLabel("v1.0.0 Enterprise Desktop Platform (MySQL 8.x)");
        version.setFont(DesignSystem.FONT_TINY);
        version.setForeground(new Color(100, 116, 139));

        leftBanner.add(appBadge);
        leftBanner.add(Box.createVerticalStrut(8));
        leftBanner.add(title1);
        leftBanner.add(title2);
        leftBanner.add(Box.createVerticalStrut(18));
        leftBanner.add(desc);
        leftBanner.add(Box.createVerticalGlue());
        leftBanner.add(version);

        add(leftBanner, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
        rightPanel.setBackground(DesignSystem.BG_CANVAS);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(24, 36, 18, 36));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 0);

        JLabel lblWelcome = new JLabel("Welcome Back");
        lblWelcome.setFont(DesignSystem.FONT_DISPLAY_MD);
        lblWelcome.setForeground(DesignSystem.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Enter your campus credentials to access your workspace");
        lblSub.setFont(DesignSystem.FONT_SMALL);
        lblSub.setForeground(DesignSystem.TEXT_SECONDARY);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 2, 0);
        formCard.add(lblWelcome, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(lblSub, gbc);

        CardPanel demoCard = new CardPanel(10, Color.WHITE, DesignSystem.CARD_BORDER);
        demoCard.setLayout(new GridBagLayout());
        demoCard.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.gridx = 0;
        dgbc.weightx = 1.0;
        dgbc.fill = GridBagConstraints.HORIZONTAL;
        dgbc.anchor = GridBagConstraints.WEST;

        JPanel demoHeader = new JPanel(new BorderLayout(8, 0));
        demoHeader.setOpaque(false);
        JLabel lblQuick = new JLabel("Demo Account Autofill");
        lblQuick.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblQuick.setForeground(DesignSystem.TEXT_PRIMARY);
        JLabel lblQuickHint = new JLabel("Select or click a role below");
        lblQuickHint.setFont(DesignSystem.FONT_TINY);
        lblQuickHint.setForeground(DesignSystem.TEXT_MUTED);
        demoHeader.add(lblQuick, BorderLayout.WEST);
        demoHeader.add(lblQuickHint, BorderLayout.EAST);

        dgbc.gridy = 0;
        dgbc.insets = new Insets(0, 0, 6, 0);
        demoCard.add(demoHeader, dgbc);

        demoAccounts = loadDemoAccounts();

        List<String> comboItems = new ArrayList<>();
        comboItems.add("-- Select Demo Account --");
        for (DemoAccount acc : demoAccounts) {
            comboItems.add(acc.label() + "  |  " + acc.email() + " / " + acc.password());
        }

        cmbQuickFill = new JComboBox<>(comboItems.toArray(new String[0]));
        cmbQuickFill.setFont(DesignSystem.FONT_SMALL);
        cmbQuickFill.setMaximumRowCount(6);
        cmbQuickFill.setPreferredSize(new Dimension(440, 36));
        cmbQuickFill.setMinimumSize(new Dimension(280, 36));
        cmbQuickFill.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                label.setFont(DesignSystem.FONT_SMALL);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return label;
            }
        });

        dgbc.gridy = 1;
        dgbc.insets = new Insets(0, 0, 8, 0);
        demoCard.add(cmbQuickFill, dgbc);

        JPanel credListPanel = new JPanel(new GridBagLayout());
        credListPanel.setBackground(DesignSystem.BG_CANVAS);
        credListPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.gridy = 0;
        cgbc.insets = new Insets(2, 0, 2, 0);
        cgbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < demoAccounts.size(); i++) {
            DemoAccount acc = demoAccounts.get(i);
            final int comboIndex = i + 1;
            JPanel rowPanel = new JPanel(new GridBagLayout());
            rowPanel.setOpaque(true);
            rowPanel.setBackground(DesignSystem.BG_CANVAS);
            rowPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            rowPanel.setToolTipText("Click to autofill " + acc.label() + " credentials");

            GridBagConstraints rgbc = new GridBagConstraints();
            rgbc.gridy = 0;
            rgbc.anchor = GridBagConstraints.WEST;

            JLabel lblRole = new JLabel(acc.label());
            lblRole.setFont(new Font(DesignSystem.FONT_FAMILY, Font.BOLD, 11));
            lblRole.setForeground(DesignSystem.TEXT_PRIMARY);
            lblRole.setPreferredSize(new Dimension(138, 18));
            rgbc.gridx = 0;
            rgbc.weightx = 0.0;
            rgbc.insets = new Insets(1, 4, 1, 8);
            rowPanel.add(lblRole, rgbc);

            JLabel lblCred = new JLabel(acc.email() + " / " + acc.password());
            lblCred.setFont(new Font("Consolas", Font.PLAIN, 11));
            lblCred.setForeground(DesignSystem.TEXT_SECONDARY);
            rgbc.gridx = 1;
            rgbc.weightx = 1.0;
            rgbc.fill = GridBagConstraints.HORIZONTAL;
            rgbc.insets = new Insets(1, 0, 1, 4);
            rowPanel.add(lblCred, rgbc);

            rowPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (cmbQuickFill.isEnabled()) {
                        cmbQuickFill.setSelectedIndex(comboIndex);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (cmbQuickFill.isEnabled()) {
                        rowPanel.setBackground(new Color(0xE8, 0xEE, 0xFF));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    rowPanel.setBackground(DesignSystem.BG_CANVAS);
                }
            });

            cgbc.gridx = 0;
            cgbc.gridy = i;
            cgbc.weightx = 1.0;
            cgbc.fill = GridBagConstraints.HORIZONTAL;
            credListPanel.add(rowPanel, cgbc);
        }

        JScrollPane credScrollPane = new JScrollPane(credListPanel);
        credScrollPane.setBorder(BorderFactory.createLineBorder(DesignSystem.CARD_BORDER_SUBTLE, 1, true));
        credScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        credScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        credScrollPane.setPreferredSize(new Dimension(420, 114));
        credScrollPane.setMinimumSize(new Dimension(280, 96));
        credScrollPane.getVerticalScrollBar().setUnitIncrement(12);

        dgbc.gridy = 2;
        dgbc.insets = new Insets(0, 0, 0, 0);
        demoCard.add(credScrollPane, dgbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(demoCard, gbc);

        JLabel lblEmail = new JLabel("Campus Email Address");
        lblEmail.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblEmail.setForeground(DesignSystem.TEXT_PRIMARY);
        txtEmail = new JTextField(24);
        txtEmail.setPreferredSize(new Dimension(320, 38));
        txtEmail.setMinimumSize(new Dimension(240, 38));
        txtEmail.setFont(DesignSystem.FONT_BODY);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblEmail, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(txtEmail, gbc);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(DesignSystem.FONT_SMALL_BOLD);
        lblPass.setForeground(DesignSystem.TEXT_PRIMARY);

        JPanel passPanel = new JPanel(new BorderLayout(6, 0));
        passPanel.setOpaque(false);
        txtPassword = new JPasswordField(20);
        txtPassword.setPreferredSize(new Dimension(270, 38));
        txtPassword.setMinimumSize(new Dimension(200, 38));
        txtPassword.setFont(DesignSystem.FONT_BODY);

        btnTogglePassword = new JButton(VectorIcons.eye(DesignSystem.TEXT_SECONDARY, 16));
        btnTogglePassword.setPreferredSize(new Dimension(44, 38));
        btnTogglePassword.setMargin(new Insets(0, 0, 0, 0));
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setToolTipText("Show / Hide Password");
        btnTogglePassword.addActionListener(e -> togglePasswordVisibility());

        passPanel.add(txtPassword, BorderLayout.CENTER);
        passPanel.add(btnTogglePassword, BorderLayout.EAST);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblPass, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(passPanel, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(progressBar, gbc);

        btnLogin = new ModernButton("Sign In to Portal", ModernButton.ButtonType.PRIMARY);
        btnLogin.setPreferredSize(new Dimension(320, 40));
        gbc.gridy = 8;
        gbc.insets = new Insets(2, 0, 8, 0);
        formCard.add(btnLogin, gbc);

        btnRegister = new ModernButton("New Student? Register Campus Account", ModernButton.ButtonType.SECONDARY);
        btnRegister.setPreferredSize(new Dimension(320, 38));
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(btnRegister, gbc);

        class ScrollableCenterPanel extends JPanel implements Scrollable {
            ScrollableCenterPanel() {
                setOpaque(false);
                add(formCard);
            }

            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                int targetW = Math.max(280, Math.min(w, 500));
                int prefH = formCard.getPreferredSize().height;
                int x = Math.max(0, (w - targetW) / 2);
                int y = Math.max(0, (h - prefH) / 2);
                formCard.setBounds(x, y, targetW, prefH);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = formCard.getPreferredSize();
                return new Dimension(Math.min(d.width, 500), d.height);
            }

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return getPreferredSize();
            }

            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 16;
            }

            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 48;
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getParent() != null && getParent().getHeight() >= formCard.getPreferredSize().height;
            }
        }

        ScrollableCenterPanel centerHost = new ScrollableCenterPanel();
        JScrollPane formScroll = new JScrollPane(centerHost);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        rightPanel.add(formScroll, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        lblDbTelemetry = new JLabel("Database: Connected (smart_campus_db)",
                VectorIcons.statusDot(DesignSystem.SUCCESS, 10), JLabel.LEFT);
        lblDbTelemetry.setIconTextGap(6);
        lblDbTelemetry.setFont(DesignSystem.FONT_TINY);
        lblDbTelemetry.setForeground(DesignSystem.SUCCESS);
        bottomBar.add(lblDbTelemetry, BorderLayout.CENTER);

        btnDbConfig = new ModernButton("Database Settings", ModernButton.ButtonType.GHOST);
        btnDbConfig.setFont(DesignSystem.FONT_TINY);
        btnDbConfig.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnDbConfig.setPreferredSize(new Dimension(145, 30));
        bottomBar.add(btnDbConfig, BorderLayout.EAST);

        rightPanel.add(bottomBar, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> handleLogin());
        btnRegister.addActionListener(e -> new RegistrationDialog(this).setVisible(true));
        btnDbConfig.addActionListener(e -> new DatabaseConnectionStatusDialog(this).setVisible(true));

        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        txtEmail.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        cmbQuickFill.addActionListener(e -> {
            int idx = cmbQuickFill.getSelectedIndex();
            if (idx >= 1 && idx <= demoAccounts.size()) {
                DemoAccount selected = demoAccounts.get(idx - 1);
                txtEmail.setText(selected.email());
                txtPassword.setText(selected.password());
            }
        });

        checkDatabaseTelemetryAsync();
    }

    private List<DemoAccount> loadDemoAccounts() {
        Map<String, DemoAccount> uniqueAccounts = new LinkedHashMap<>();
        try {
            List<User> activeUsers = userService.getActiveDemoUsers();
            Map<String, User> activeByEmail = new HashMap<>();
            for (User u : activeUsers) {
                if (u.getEmail() != null && u.isActive()) {
                    activeByEmail.putIfAbsent(u.getEmail().trim().toLowerCase(Locale.ROOT), u);
                }
            }

            for (DemoAccount def : DEFAULT_DEMO_ACCOUNTS) {
                String key = def.email().toLowerCase(Locale.ROOT);
                User dbUser = activeByEmail.get(key);
                if (dbUser != null) {
                    uniqueAccounts.putIfAbsent(key, new DemoAccount(
                            resolveDemoLabel(dbUser, def.label()),
                            dbUser.getEmail().trim(),
                            def.password()));
                }
            }
        } catch (Exception ex) {
            logger.warn("Unable to query active demo accounts from database; using configured defaults: {}",
                    ex.getMessage());
        }

        if (uniqueAccounts.isEmpty()) {
            for (DemoAccount def : DEFAULT_DEMO_ACCOUNTS) {
                uniqueAccounts.putIfAbsent(def.email().toLowerCase(Locale.ROOT), def);
            }
        }
        return List.copyOf(uniqueAccounts.values());
    }

    private String resolveDemoLabel(User user, String defaultLabel) {
        if (user == null || user.getRole() == null) {
            return defaultLabel;
        }
        if (user.getRole() == Role.ADMIN) {
            return "Administrator";
        }
        if (user.getRole() == Role.STAFF) {
            String dept = user.getDepartment() != null ? user.getDepartment().toLowerCase(Locale.ROOT) : "";
            if (dept.contains("electric")) {
                return "Electrician";
            }
            if (dept.contains("plumb")) {
                return "Plumber";
            }
            if (dept.contains("network") || dept.contains("it support")) {
                return "Network Technician";
            }
            if (dept.contains("carpent") || dept.contains("furniture")) {
                return "Carpenter";
            }
            if (dept.contains("general") || dept.contains("maintenance")) {
                return "General Maintenance";
            }
            return user.getDepartment() != null && !user.getDepartment().isBlank()
                    ? user.getDepartment().trim()
                    : defaultLabel;
        }
        if (user.getRole() == Role.STUDENT) {
            String fullName = user.getFullName() != null ? user.getFullName().trim() : "";
            if (!fullName.isEmpty()) {
                String firstName = fullName.split("\\s+")[0];
                return "Student (" + firstName + ")";
            }
            return defaultLabel;
        }
        return defaultLabel;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPassword.setEchoChar((char) 0);
            btnTogglePassword.setIcon(VectorIcons.eyeOff(DesignSystem.TEXT_SECONDARY, 16));
        } else {
            txtPassword.setEchoChar('\u2022');
            btnTogglePassword.setIcon(VectorIcons.eye(DesignSystem.TEXT_SECONDARY, 16));
        }
    }

    private void checkDatabaseTelemetryAsync() {
        Thread telemetryThread = new Thread(() -> {
            boolean ok = DBConnection.testConnection();
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (!isDisplayable()) {
                    return;
                }
                if (ok) {
                    lblDbTelemetry.setIcon(VectorIcons.statusDot(DesignSystem.SUCCESS, 10));
                    lblDbTelemetry.setText("MySQL 8.0: Connected (smart_campus_db)");
                    lblDbTelemetry.setForeground(DesignSystem.SUCCESS);
                } else {
                    lblDbTelemetry.setIcon(VectorIcons.cross(DesignSystem.CRITICAL, 12));
                    lblDbTelemetry.setText("Database Disconnected - Check Settings");
                    lblDbTelemetry.setForeground(DesignSystem.CRITICAL);
                }
            });
        }, "DB-Telemetry-Thread");
        telemetryThread.setDaemon(true);
        telemetryThread.start();
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        char[] pass = txtPassword.getPassword();

        if (email.isEmpty() || pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter both campus email and password.", "Login Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordStr = new String(pass);
        setWorking(true);

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return userService.authenticate(email, passwordStr);
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    User user = get();
                    UserSession.login(user);
                    openRoleDashboard(user);
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Login failed: " + msg, "Authentication Failure", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openRoleDashboard(User user) {
        dispose();
        switch (user.getRole()) {
            case STUDENT -> new StudentDashboard().setVisible(true);
            case ADMIN -> new AdminDashboard().setVisible(true);
            case STAFF -> new StaffDashboard().setVisible(true);
        }
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnLogin.setEnabled(!working);
        btnRegister.setEnabled(!working);
        btnDbConfig.setEnabled(!working);
        txtEmail.setEnabled(!working);
        txtPassword.setEnabled(!working);
        cmbQuickFill.setEnabled(!working);
    }
}
