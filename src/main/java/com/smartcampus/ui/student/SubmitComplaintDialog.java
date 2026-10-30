package com.smartcampus.ui.student;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.enums.ComplaintCategory;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.service.PriorityService;
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
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class SubmitComplaintDialog extends JDialog {
    private final JTextField txtTitle;
    private final JTextArea txtDescription;
    private final JComboBox<String> cmbCategory;
    private final JTextField txtBuilding;
    private final JTextField txtRoom;
    private final JComboBox<Integer> cmbSeverity;
    private final JComboBox<Integer> cmbSafetyRisk;
    private final JSpinner spnPeopleAffected;
    private final JComboBox<Integer> cmbOperationalImpact;

    private final JLabel lblPreview;
    private final JProgressBar progressBar;
    private final ModernButton btnSubmit;
    private final ModernButton btnCancel;

    private final ComplaintService complaintService = new ComplaintService();
    private final PriorityService priorityService = new PriorityService();

    private boolean submittedSuccessfully = false;

    public SubmitComplaintDialog(JFrame parent) {
        super(parent, "Submit New Campus Complaint", true);
        setSize(650, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 32, 54));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Lodge a Campus Facility Complaint");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("The system automatically calculates priority score and dispatches maintenance staff");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtTitle = new JTextField();
        txtDescription = new JTextArea(4, 25);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDescription);

        cmbCategory = new JComboBox<>();
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            cmbCategory.addItem(cat.getDisplayName());
        }

        txtBuilding = new JTextField();
        txtRoom = new JTextField();

        cmbSeverity = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cmbSeverity.setSelectedIndex(2); // default 3

        cmbSafetyRisk = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5});
        cmbSafetyRisk.setSelectedIndex(0); // default 0

        spnPeopleAffected = new JSpinner(new SpinnerNumberModel(10, 1, 10000, 1));

        cmbOperationalImpact = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cmbOperationalImpact.setSelectedIndex(2); // default 3

        // Row 0: Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Complaint Title *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(txtTitle, gbc);

        // Row 1: Category
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Category *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(cmbCategory, gbc);

        // Row 2: Building
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Building / Location *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtBuilding, gbc);

        // Row 3: Room Number
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Room / Area Number *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtRoom, gbc);

        // Row 4: Description
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Detailed Description *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(descScroll, gbc);

        // Priority inputs divider
        JPanel prioPanel = new JPanel(new GridBagLayout());
        prioPanel.setBorder(BorderFactory.createTitledBorder("Priority Scoring Factors (Deterministic Calculation)"));
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(4, 6, 4, 6);
        pgbc.fill = GridBagConstraints.HORIZONTAL;

        pgbc.gridx = 0; pgbc.gridy = 0; pgbc.weightx = 0.5;
        prioPanel.add(new JLabel("Severity (1-5):"), pgbc);
        pgbc.gridx = 1;
        prioPanel.add(cmbSeverity, pgbc);

        pgbc.gridx = 0; pgbc.gridy = 1;
        prioPanel.add(new JLabel("Safety Risk (0-5, 0=None):"), pgbc);
        pgbc.gridx = 1;
        prioPanel.add(cmbSafetyRisk, pgbc);

        pgbc.gridx = 0; pgbc.gridy = 2;
        prioPanel.add(new JLabel("Estimated People Affected:"), pgbc);
        pgbc.gridx = 1;
        prioPanel.add(spnPeopleAffected, pgbc);

        pgbc.gridx = 0; pgbc.gridy = 3;
        prioPanel.add(new JLabel("Operational Impact (1-5):"), pgbc);
        pgbc.gridx = 1;
        prioPanel.add(cmbOperationalImpact, pgbc);

        // Preview label
        lblPreview = new JLabel("Live Priority Preview: Calculating...");
        lblPreview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPreview.setForeground(new Color(37, 99, 235));
        pgbc.gridx = 0; pgbc.gridy = 4; pgbc.gridwidth = 2;
        prioPanel.add(lblPreview, pgbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(prioPanel, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridy = 6;
        formPanel.add(progressBar, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnSubmit = new ModernButton("Submit Complaint", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSubmit);
        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners for live priority preview
        cmbSeverity.addActionListener(e -> updatePriorityPreview());
        cmbSafetyRisk.addActionListener(e -> updatePriorityPreview());
        spnPeopleAffected.addChangeListener(e -> updatePriorityPreview());
        cmbOperationalImpact.addActionListener(e -> updatePriorityPreview());
        cmbCategory.addActionListener(e -> updatePriorityPreview());

        btnSubmit.addActionListener(e -> handleSubmit());
        btnCancel.addActionListener(e -> dispose());

        updatePriorityPreview();
    }

    private void updatePriorityPreview() {
        try {
            int s = (Integer) cmbSeverity.getSelectedItem();
            int r = (Integer) cmbSafetyRisk.getSelectedItem();
            int a = (Integer) spnPeopleAffected.getValue();
            int i = (Integer) cmbOperationalImpact.getSelectedItem();
            String cat = (String) cmbCategory.getSelectedItem();

            PriorityService.PriorityResult res = priorityService.calculatePriority(s, r, a, i, cat);
            lblPreview.setText("<html>Preview: <b>Score: " + res.score() + "</b> &nbsp;|&nbsp; Priority: <b>" +
                    res.priorityLevel().name() + "</b>" +
                    (res.isSafetyCritical() ? " <font color='#dc2626'>[CRITICAL SAFETY HAZARD]</font>" : "") +
                    "</html>");
        } catch (Exception ignored) {}
    }

    private void handleSubmit() {
        String title = txtTitle.getText().trim();
        String desc = txtDescription.getText().trim();
        String category = (String) cmbCategory.getSelectedItem();
        String building = txtBuilding.getText().trim();
        String room = txtRoom.getText().trim();

        if (title.isEmpty() || desc.isEmpty() || building.isEmpty() || room.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields marked with *.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int severity = (Integer) cmbSeverity.getSelectedItem();
        int safetyRisk = (Integer) cmbSafetyRisk.getSelectedItem();
        int peopleAffected = (Integer) spnPeopleAffected.getValue();
        int operationalImpact = (Integer) cmbOperationalImpact.getSelectedItem();

        Complaint c = new Complaint();
        c.setTitle(title);
        c.setDescription(desc);
        c.setCategory(category);
        c.setBuilding(building);
        c.setRoomNumber(room);
        c.setSeverity(severity);
        c.setSafetyRisk(safetyRisk);
        c.setPeopleAffected(peopleAffected);
        c.setOperationalImpact(operationalImpact);

        setWorking(true);

        new SwingWorker<Complaint, Void>() {
            @Override
            protected Complaint doInBackground() throws Exception {
                return complaintService.submitComplaint(c);
            }

            @Override
            protected void done() {
                setWorking(false);
                try {
                    Complaint saved = get();
                    submittedSuccessfully = true;
                    JOptionPane.showMessageDialog(SubmitComplaintDialog.this,
                            "Complaint #" + saved.getComplaintId() + " successfully registered!\n" +
                            "Priority Assigned: " + saved.getPriority() + " (Score: " + saved.getPriorityScore() + ")\n" +
                            "Status: " + saved.getStatus(),
                            "Submission Successful", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(SubmitComplaintDialog.this,
                            "Error submitting complaint: " + msg, "Submission Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        btnSubmit.setEnabled(!working);
        btnCancel.setEnabled(!working);
        txtTitle.setEnabled(!working);
        txtDescription.setEnabled(!working);
        cmbCategory.setEnabled(!working);
        txtBuilding.setEnabled(!working);
        txtRoom.setEnabled(!working);
        cmbSeverity.setEnabled(!working);
        cmbSafetyRisk.setEnabled(!working);
        spnPeopleAffected.setEnabled(!working);
        cmbOperationalImpact.setEnabled(!working);
    }

    public boolean isSubmittedSuccessfully() {
        return submittedSuccessfully;
    }
}
