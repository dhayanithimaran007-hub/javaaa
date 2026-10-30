package com.smartcampus.ui.staff;

import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ResolutionDialog extends JDialog {
    private final int complaintId;
    private final JTextArea txtResolutionNote;
    private final JProgressBar progressBar;
    private final ModernButton btnResolve;
    private final ModernButton btnCancel;

    private final ComplaintService complaintService = new ComplaintService();
    private boolean resolvedSuccessfully = false;

    public ResolutionDialog(JFrame parent, int complaintId, String complaintTitle) {
        super(parent, "Mark Work Complete & Resolve - Complaint #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(480, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("Complete Complaint Resolution");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Complaint: " + (complaintTitle.length() > 40 ? complaintTitle.substring(0, 40) + "..." : complaintTitle));
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtResolutionNote = new JTextArea(4, 25);
        txtResolutionNote.setLineWrap(true);
        txtResolutionNote.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtResolutionNote);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        formPanel.add(new JLabel("Resolution Details *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        formPanel.add(scroll, gbc);

        JLabel hint = new JLabel("<html><font color='#64748b'>Describe parts replaced, repairs completed, or steps taken.</font></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(hint, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnResolve = new ModernButton("Confirm Resolution", ModernButton.ButtonType.SUCCESS);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnResolve);
        add(buttonPanel, BorderLayout.SOUTH);

        btnResolve.addActionListener(e -> handleResolve());
        btnCancel.addActionListener(e -> dispose());
    }

    private void handleResolve() {
        String note = txtResolutionNote.getText().trim();
        if (note.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide a resolution note describing the completed work.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        progressBar.setVisible(true);
        btnResolve.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.resolveComplaint(complaintId, note);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnResolve.setEnabled(true);
                try {
                    get();
                    resolvedSuccessfully = true;
                    JOptionPane.showMessageDialog(ResolutionDialog.this,
                            "Complaint #" + complaintId + " successfully marked as RESOLVED!",
                            "Resolution Complete", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(ResolutionDialog.this,
                            "Failed to resolve complaint: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isResolvedSuccessfully() {
        return resolvedSuccessfully;
    }
}
