package com.smartcampus.ui.admin;

import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AuditLogsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbAction;
    private final JComboBox<String> cmbEntity;
    private final JTable tableLogs;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final ModernButton btnRefresh;
    private final ModernButton btnInspect;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<AuditLog> currentLogs = new ArrayList<>();

    public AuditLogsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(DesignSystem.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        CardPanel topBar = new CardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        topBar.add(new JLabel("Search Audit:"));
        txtSearch = new JTextField(14);
        txtSearch.setPreferredSize(new Dimension(160, 32));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Action:"));
        cmbAction = new JComboBox<>(new String[]{
                "ALL", "LOGIN", "SUBMIT_COMPLAINT", "ASSIGN_STAFF", "UPDATE_PROGRESS",
                "RESOLVE_COMPLAINT", "REJECT_COMPLAINT", "REOPEN_COMPLAINT", "CREATE_STAFF",
                "REGISTER_STUDENT", "ACTIVATE_USER", "DEACTIVATE_USER", "EXPORT_CSV"
        });
        topBar.add(cmbAction);

        topBar.add(new JLabel("Entity:"));
        cmbEntity = new JComboBox<>(new String[]{"ALL", "COMPLAINT", "USER", "REPORTS"});
        topBar.add(cmbEntity);

        btnRefresh = new ModernButton("Filter Logs", ModernButton.ButtonType.PRIMARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        String[] cols = {"Log ID", "Timestamp", "Actor User", "Role", "Action Type", "Entity", "Entity ID", "Audit Event Details"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableLogs = new JTable(tableModel);
        tableLogs.setRowHeight(32);
        tableLogs.setFont(DesignSystem.FONT_SMALL);
        tableLogs.getTableHeader().setFont(DesignSystem.FONT_SMALL_BOLD);

        tableLogs.getColumnModel().getColumn(0).setPreferredWidth(55);
        tableLogs.getColumnModel().getColumn(1).setPreferredWidth(140);
        tableLogs.getColumnModel().getColumn(2).setPreferredWidth(150);
        tableLogs.getColumnModel().getColumn(3).setPreferredWidth(85);
        tableLogs.getColumnModel().getColumn(4).setPreferredWidth(140);
        tableLogs.getColumnModel().getColumn(5).setPreferredWidth(90);
        tableLogs.getColumnModel().getColumn(6).setPreferredWidth(70);
        tableLogs.getColumnModel().getColumn(7).setPreferredWidth(320);

        tableLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableLogs.getSelectedRow() >= 0) {
                    inspectSelectedLog();
                }
            }
        });

        CardPanel centerCard = new CardPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerCard.add(progressBar, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(tableLogs), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        btnInspect = new ModernButton("Inspect Audit Event Details", ModernButton.ButtonType.SECONDARY);
        bottomBar.add(btnInspect);
        add(bottomBar, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadLogs());
        txtSearch.addActionListener(e -> loadLogs());
        cmbAction.addActionListener(e -> loadLogs());
        cmbEntity.addActionListener(e -> loadLogs());
        btnInspect.addActionListener(e -> inspectSelectedLog());

        loadLogs();
    }

    public void loadLogs() {
        progressBar.setVisible(true);
        btnRefresh.setEnabled(false);

        String search = txtSearch.getText().trim();
        String action = (String) cmbAction.getSelectedItem();
        String entity = (String) cmbEntity.getSelectedItem();

        new SwingWorker<List<AuditLog>, Void>() {
            @Override
            protected List<AuditLog> doInBackground() throws Exception {
                return auditLogDAO.findAll(action, entity, search, 250);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnRefresh.setEnabled(true);
                try {
                    List<AuditLog> logs = get();
                    currentLogs.clear();
                    currentLogs.addAll(logs);
                    tableModel.setRowCount(0);

                    for (AuditLog l : logs) {
                        tableModel.addRow(new Object[]{
                                l.getAuditId(),
                                l.getCreatedAt() != null ? dateFormat.format(l.getCreatedAt()) : "",
                                l.getActorName() != null ? l.getActorName() : "User #" + l.getActorUserId(),
                                l.getActorRole() != null ? l.getActorRole() : "SYSTEM",
                                l.getAction(),
                                l.getEntityType(),
                                l.getEntityId() != null ? "#" + l.getEntityId() : "-",
                                l.getDetails()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AuditLogsPanel.this,
                            "Failed to load audit logs: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void inspectSelectedLog() {
        int row = tableLogs.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an audit log entry to inspect.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AuditLog log = currentLogs.get(row);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(parent, "Audit Event Inspector - #" + log.getAuditId(), true);
        dlg.setSize(520, 360);
        dlg.setLocationRelativeTo(parent);
        dlg.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DesignSystem.MIDNIGHT_NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("Audit Log Record #" + log.getAuditId());
        title.setFont(DesignSystem.FONT_TITLE);
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Action: " + log.getAction() + " | Recorded: " + (log.getCreatedAt() != null ? dateFormat.format(log.getCreatedAt()) : ""));
        sub.setFont(DesignSystem.FONT_SMALL);
        sub.setForeground(DesignSystem.TEXT_MUTED);
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        dlg.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBackground(DesignSystem.BG_CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JTextArea txt = new JTextArea();
        txt.setFont(DesignSystem.FONT_CODE);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setText(
                "Audit ID:    " + log.getAuditId() + "\n" +
                "Timestamp:   " + (log.getCreatedAt() != null ? dateFormat.format(log.getCreatedAt()) : "N/A") + "\n" +
                "Actor User:  " + (log.getActorName() != null ? log.getActorName() : "ID #" + log.getActorUserId()) + "\n" +
                "Actor Role:  " + log.getActorRole() + "\n" +
                "Action Type: " + log.getAction() + "\n" +
                "Entity Type: " + log.getEntityType() + "\n" +
                "Entity ID:   " + (log.getEntityId() != null ? "#" + log.getEntityId() : "N/A") + "\n" +
                "Details:\n" + log.getDetails()
        );

        content.add(new JScrollPane(txt), BorderLayout.CENTER);
        dlg.add(content, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setBackground(Color.WHITE);
        ModernButton btnClose = new ModernButton("Close", ModernButton.ButtonType.SECONDARY);
        btnClose.addActionListener(e -> dlg.dispose());
        bottom.add(btnClose);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
