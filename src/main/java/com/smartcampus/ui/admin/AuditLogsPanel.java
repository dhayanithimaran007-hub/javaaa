package com.smartcampus.ui.admin;

import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.List;

public class AuditLogsPanel extends JPanel {
    private final JTextField txtSearch;
    private final JComboBox<String> cmbAction;
    private final JComboBox<String> cmbEntity;
    private final JTable tableLogs;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final ModernButton btnRefresh;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AuditLogsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        topBar.add(new JLabel("Search Audit:"));
        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(140, 30));
        topBar.add(txtSearch);

        topBar.add(new JLabel("Action:"));
        cmbAction = new JComboBox<>(new String[]{
                "ALL", "LOGIN", "SUBMIT_COMPLAINT", "ASSIGN_STAFF", "UPDATE_PROGRESS",
                "RESOLVE_COMPLAINT", "REJECT_COMPLAINT", "REOPEN_COMPLAINT", "CREATE_STAFF", "ACTIVATE_USER", "DEACTIVATE_USER"
        });
        topBar.add(cmbAction);

        topBar.add(new JLabel("Entity:"));
        cmbEntity = new JComboBox<>(new String[]{"ALL", "COMPLAINT", "USER", "REPORTS"});
        topBar.add(cmbEntity);

        btnRefresh = new ModernButton("Filter Logs", ModernButton.ButtonType.SECONDARY);
        topBar.add(btnRefresh);

        add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Log ID", "Timestamp", "Actor User", "Role", "Action", "Entity", "Entity ID", "Audit Details"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableLogs = new JTable(tableModel);
        tableLogs.setRowHeight(30);
        tableLogs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableLogs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tableLogs.getColumnModel().getColumn(0).setPreferredWidth(55);
        tableLogs.getColumnModel().getColumn(1).setPreferredWidth(140);
        tableLogs.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableLogs.getColumnModel().getColumn(3).setPreferredWidth(90);
        tableLogs.getColumnModel().getColumn(4).setPreferredWidth(130);
        tableLogs.getColumnModel().getColumn(5).setPreferredWidth(90);
        tableLogs.getColumnModel().getColumn(6).setPreferredWidth(70);
        tableLogs.getColumnModel().getColumn(7).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(tableLogs);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JPanel centerPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        centerPanel.add(progressBar, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadLogs());
        txtSearch.addActionListener(e -> loadLogs());
        cmbAction.addActionListener(e -> loadLogs());
        cmbEntity.addActionListener(e -> loadLogs());

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
                    tableModel.setRowCount(0);
                    for (AuditLog l : logs) {
                        tableModel.addRow(new Object[]{
                                l.getAuditId(),
                                l.getCreatedAt() != null ? dateFormat.format(l.getCreatedAt()) : "",
                                l.getActorName() != null ? l.getActorName() : "User #" + l.getActorUserId(),
                                l.getActorRole() != null ? l.getActorRole() : "",
                                l.getAction(),
                                l.getEntityType(),
                                l.getEntityId() != null ? "#" + l.getEntityId() : "—",
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
}
