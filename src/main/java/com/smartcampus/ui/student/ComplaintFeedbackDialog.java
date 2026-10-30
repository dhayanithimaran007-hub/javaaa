package com.smartcampus.ui.student;

import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.ModernButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
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

public class ComplaintFeedbackDialog extends JDialog {
    private final int complaintId;
    private final JComboBox<String> cmbRating;
    private final JTextArea txtComments;
    private final JProgressBar progressBar;
    private final ModernButton btnSubmit;
    private final ModernButton btnCancel;

    private final ComplaintService complaintService = new ComplaintService();
    private boolean feedbackSubmitted = false;

    public ComplaintFeedbackDialog(JFrame parent, int complaintId, String complaintTitle) {
        super(parent, "Submit Resolution Feedback - Complaint #" + complaintId, true);
        this.complaintId = complaintId;
        setSize(480, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("Resolution Feedback");
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

        cmbRating = new JComboBox<>(new String[]{
                "5 Stars - Excellent Resolution",
                "4 Stars - Good",
                "3 Stars - Satisfactory",
                "2 Stars - Poor",
                "1 Star - Unsatisfactory"
        });

        txtComments = new JTextArea(4, 25);
        txtComments.setLineWrap(true);
        txtComments.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtComments);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Rating *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(cmbRating, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Comments:"), gbc);
        gbc.gridx = 1;
        formPanel.add(scroll, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnSubmit = new ModernButton("Submit Feedback", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSubmit);
        add(buttonPanel, BorderLayout.SOUTH);

        btnSubmit.addActionListener(e -> handleSubmit());
        btnCancel.addActionListener(e -> dispose());
    }

    private void handleSubmit() {
        int rating = 5 - cmbRating.getSelectedIndex();
        String comments = txtComments.getText().trim();

        setWorking(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.submitFeedback(complaintId, rating, comments);
                return null;
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    get();
                    feedbackSubmitted = true;
                    JOptionPane.showMessageDialog(ComplaintFeedbackDialog.this,
                            "Thank you! Your feedback has been recorded.",
                            "Feedback Submitted", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(ComplaintFeedbackDialog.this,
                            "Failed to submit feedback: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnSubmit.setEnabled(!working);
        btnCancel.setEnabled(!working);
        cmbRating.setEnabled(!working);
        txtComments.setEnabled(!working);
    }

    public boolean isFeedbackSubmitted() {
        return feedbackSubmitted;
    }
}
