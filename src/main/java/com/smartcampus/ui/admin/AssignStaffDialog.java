package com.smartcampus.ui.admin;

import com.smartcampus.model.Complaint;
import com.smartcampus.model.StaffRecommendationDTO;
import com.smartcampus.service.ComplaintService;
import com.smartcampus.ui.components.CardPanel;
import com.smartcampus.ui.components.ModernButton;
import com.smartcampus.ui.components.VectorIcons;
import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class AssignStaffDialog extends JDialog {
    private final int complaintId;
    private final String complaintCategory;
    private final JComboBox<UserItem> cmbStaff;
    private final JTextArea txtNotes;
    private final JProgressBar progressBar;
    private final ModernButton btnAssign;
    private final ModernButton btnCancel;

    private final CardPanel recommendationCard;
    private final JLabel lblRecReason;
    private final ModernButton btnSelectRec;
    private StaffRecommendationDTO topRecommendation = null;

    private final ComplaintService complaintService = new ComplaintService();
    private boolean assignedSuccessfully = false;

    public record UserItem(int id, String name, String dept, int activeTasks, boolean isRecommended) {
        @Override
        public String toString() {
            String badge = isRecommended ? "[RECOMMENDED] " : "";
            return String.format("%s%s (%s) - %d active", badge, name, dept, activeTasks);
        }
    }

    public AssignStaffDialog(JFrame parent, int complaintId, String complaintTitle) {
        this(parent, complaintId, complaintTitle, "General");
    }

    public AssignStaffDialog(JFrame parent, int complaintId, String complaintTitle, String complaintCategory) {
        super(parent, "Intelligent Staff Dispatch - Complaint #" + complaintId, true);
        this.complaintId = complaintId;
        this.complaintCategory = complaintCategory;

        setSize(560, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DesignSystem.MIDNIGHT_NAVY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Intelligent Maintenance Dispatch");
        titleLabel.setFont(DesignSystem.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        String titleSub = complaintTitle.length() > 45 ? complaintTitle.substring(0, 45) + "..." : complaintTitle;
        JLabel subLabel = new JLabel("Complaint #" + complaintId + " | " + titleSub + " [" + complaintCategory + "]");
        subLabel.setFont(DesignSystem.FONT_SMALL);
        subLabel.setForeground(DesignSystem.TEXT_MUTED);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(DesignSystem.BG_CANVAS);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        recommendationCard = new CardPanel(new BorderLayout(10, 8));
        recommendationCard.setBackgroundColor(new Color(238, 242, 255));
        recommendationCard.setBorderColor(DesignSystem.ELECTRIC_BLUE);
        recommendationCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        recommendationCard.setMaximumSize(new Dimension(520, 95));

        JPanel recHeader = new JPanel(new BorderLayout());
        recHeader.setOpaque(false);
        JLabel lblRecTitle = new JLabel("SMART ASSIGNMENT RECOMMENDATION",
                VectorIcons.star(DesignSystem.ELECTRIC_BLUE, 13), JLabel.LEFT);
        lblRecTitle.setIconTextGap(6);
        lblRecTitle.setFont(DesignSystem.FONT_CARD_LABEL);
        lblRecTitle.setForeground(DesignSystem.ELECTRIC_BLUE);
        recHeader.add(lblRecTitle, BorderLayout.WEST);

        btnSelectRec = new ModernButton("Select Recommended", ModernButton.ButtonType.PRIMARY);
        btnSelectRec.setFont(DesignSystem.FONT_TINY);
        btnSelectRec.setPreferredSize(new Dimension(150, 26));
        btnSelectRec.setVisible(false);
        recHeader.add(btnSelectRec, BorderLayout.EAST);
        recommendationCard.add(recHeader, BorderLayout.NORTH);

        lblRecReason = new JLabel("Analyzing staff trade specialization and current ticket queues...");
        lblRecReason.setFont(DesignSystem.FONT_SMALL);
        lblRecReason.setForeground(DesignSystem.TEXT_PRIMARY);
        recommendationCard.add(lblRecReason, BorderLayout.CENTER);

        centerPanel.add(recommendationCard);
        centerPanel.add(Box.createVerticalStrut(14));

        CardPanel formCard = new CardPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbStaff = new JComboBox<>();
        cmbStaff.setFont(DesignSystem.FONT_SMALL);
        cmbStaff.setPreferredSize(new Dimension(320, 32));

        txtNotes = new JTextArea(4, 25);
        txtNotes.setFont(DesignSystem.FONT_SMALL);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtNotes);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblStaff = new JLabel("Assigned Technician *:");
        lblStaff.setFont(DesignSystem.FONT_SMALL_BOLD);
        formCard.add(lblStaff, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formCard.add(cmbStaff, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblNotes = new JLabel("Dispatch Remarks:");
        lblNotes.setFont(DesignSystem.FONT_SMALL_BOLD);
        formCard.add(lblNotes, gbc);

        gbc.gridx = 1;
        formCard.add(scroll, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formCard.add(progressBar, gbc);

        centerPanel.add(formCard);
        add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesignSystem.CARD_BORDER));

        btnAssign = new ModernButton("Confirm Dispatch", ModernButton.ButtonType.PRIMARY);
        btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnAssign);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAssign.addActionListener(e -> handleAssign());
        btnCancel.addActionListener(e -> dispose());
        btnSelectRec.addActionListener(e -> applyRecommendation());

        loadStaffAndRecommendations();
    }

    private void loadStaffAndRecommendations() {
        progressBar.setVisible(true);

        new SwingWorker<List<StaffRecommendationDTO>, Void>() {
            @Override
            protected List<StaffRecommendationDTO> doInBackground() throws Exception {
                return complaintService.recommendStaffForComplaint(complaintCategory);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    List<StaffRecommendationDTO> recs = get();
                    DefaultComboBoxModel<UserItem> model = new DefaultComboBoxModel<>();

                    if (!recs.isEmpty()) {
                        topRecommendation = recs.get(0);
                        lblRecReason.setText("<html><b>" + topRecommendation.staff().getFullName() + "</b>: " +
                                topRecommendation.reason() + "</html>");
                        btnSelectRec.setVisible(true);

                        for (StaffRecommendationDTO r : recs) {
                            boolean isTop = (r == topRecommendation);
                            model.addElement(new UserItem(
                                    r.staff().getUserId(),
                                    r.staff().getFullName(),
                                    r.staff().getDepartment(),
                                    r.activeWorkload(),
                                    isTop
                            ));
                        }
                    } else {
                        lblRecReason.setText("No active maintenance staff found.");
                    }

                    cmbStaff.setModel(model);
                } catch (Exception ex) {
                    lblRecReason.setText("Recommendation calculation deferred: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyRecommendation() {
        if (topRecommendation != null) {
            for (int i = 0; i < cmbStaff.getItemCount(); i++) {
                UserItem item = cmbStaff.getItemAt(i);
                if (item.id() == topRecommendation.staff().getUserId()) {
                    cmbStaff.setSelectedIndex(i);
                    txtNotes.setText("Assigned via Automated Dispatch Recommendation. Specialization match: " + topRecommendation.staff().getDepartment());
                    break;
                }
            }
        }
    }

    private void handleAssign() {
        UserItem selected = (UserItem) cmbStaff.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a staff member to assign.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String notes = txtNotes.getText().trim();
        btnAssign.setEnabled(false);
        progressBar.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaintService.assignStaff(complaintId, selected.id(), notes);
                return null;
            }

            @Override
            protected void done() {
                btnAssign.setEnabled(true);
                progressBar.setVisible(false);
                try {
                    get();
                    assignedSuccessfully = true;
                    JOptionPane.showMessageDialog(AssignStaffDialog.this,
                            "Complaint #" + complaintId + " successfully assigned to " + selected.name() + "!",
                            "Assignment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(AssignStaffDialog.this,
                            "Assignment failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public boolean isAssignedSuccessfully() {
        return assignedSuccessfully;
    }
}
