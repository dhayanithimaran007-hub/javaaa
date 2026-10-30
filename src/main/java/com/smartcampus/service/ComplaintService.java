package com.smartcampus.service;

import com.smartcampus.config.DBConnection;
import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.dao.ComplaintAssignmentDAO;
import com.smartcampus.dao.ComplaintDAO;
import com.smartcampus.dao.ComplaintFeedbackDAO;
import com.smartcampus.dao.ComplaintStatusHistoryDAO;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintAssignment;
import com.smartcampus.model.ComplaintDetailsDTO;
import com.smartcampus.model.ComplaintFeedback;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class ComplaintService {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintDAO complaintDAO;
    private final ComplaintAssignmentDAO assignmentDAO;
    private final ComplaintStatusHistoryDAO historyDAO;
    private final ComplaintFeedbackDAO feedbackDAO;
    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;
    private final PriorityService priorityService;

    public ComplaintService() {
        this(new ComplaintDAO(), new ComplaintAssignmentDAO(), new ComplaintStatusHistoryDAO(),
             new ComplaintFeedbackDAO(), new UserDAO(), new AuditLogDAO(), new PriorityService());
    }

    public ComplaintService(ComplaintDAO complaintDAO, ComplaintAssignmentDAO assignmentDAO,
                            ComplaintStatusHistoryDAO historyDAO, ComplaintFeedbackDAO feedbackDAO,
                            UserDAO userDAO, AuditLogDAO auditLogDAO, PriorityService priorityService) {
        this.complaintDAO = complaintDAO;
        this.assignmentDAO = assignmentDAO;
        this.historyDAO = historyDAO;
        this.feedbackDAO = feedbackDAO;
        this.userDAO = userDAO;
        this.auditLogDAO = auditLogDAO;
        this.priorityService = priorityService;
    }

    public Complaint submitComplaint(Complaint complaint) throws SQLException {
        UserSession.requireRole(Role.STUDENT, Role.ADMIN);
        User currentUser = UserSession.getCurrentUser();

        // 1. Validate fields
        ValidationUtil.requireNonBlank(complaint.getTitle(), "Title");
        ValidationUtil.requireNonBlank(complaint.getDescription(), "Description");
        ValidationUtil.requireNonBlank(complaint.getCategory(), "Category");
        ValidationUtil.requireNonBlank(complaint.getBuilding(), "Building");
        ValidationUtil.requireNonBlank(complaint.getRoomNumber(), "Room Number");

        ValidationUtil.validateRange(complaint.getSeverity(), 1, 5, "Severity");
        ValidationUtil.validateRange(complaint.getSafetyRisk(), 0, 5, "Safety Risk");
        ValidationUtil.validatePositive(complaint.getPeopleAffected(), "People Affected");
        ValidationUtil.validateRange(complaint.getOperationalImpact(), 1, 5, "Operational Impact");

        // Set ownership
        if (currentUser.getRole() == Role.STUDENT || complaint.getStudentId() <= 0) {
            complaint.setStudentId(currentUser.getUserId());
        }

        // 2. Deterministically calculate Priority via PriorityService (Never trust UI-supplied score)
        PriorityService.PriorityResult prioResult = priorityService.calculatePriority(
                complaint.getSeverity(),
                complaint.getSafetyRisk(),
                complaint.getPeopleAffected(),
                complaint.getOperationalImpact(),
                complaint.getCategory()
        );

        complaint.setPriorityScore(prioResult.score());
        complaint.setPriority(prioResult.priorityLevel());
        complaint.setSafetyCritical(prioResult.isSafetyCritical());
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        // 3. Persist atomically in a transaction
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                complaintDAO.create(complaint, conn);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaint.getComplaintId());
                history.setChangedBy(currentUser.getUserId());
                history.setOldStatus(null);
                history.setNewStatus(ComplaintStatus.SUBMITTED);
                history.setRemarks("Complaint submitted. Priority computed: " + prioResult.breakdown());
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} submitted by user ID {} [Priority: {}, Score: {}]",
                        complaint.getComplaintId(), currentUser.getUserId(), complaint.getPriority(), complaint.getPriorityScore());
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to submit complaint, transaction rolled back: {}", e.getMessage());
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentUser.getUserId(), "SUBMIT_COMPLAINT", "COMPLAINT",
                complaint.getComplaintId(), "Submitted: " + complaint.getTitle()));

        return complaint;
    }

    public void assignStaff(int complaintId, int staffId, String notes) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();

        User staff = userDAO.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found."));
        if (staff.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("Selected user is not a staff member.");
        }
        if (!staff.isActive()) {
            throw new IllegalStateException("Selected staff member is inactive.");
        }

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.REJECTED) {
            throw new IllegalStateException("Cannot assign a complaint that is already " + complaint.getStatus() + ".");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDAO.assign(complaintId, staffId, currentAdmin.getUserId(), notes, conn);

                ComplaintStatus oldStatus = complaint.getStatus();
                ComplaintStatus newStatus = (oldStatus == ComplaintStatus.SUBMITTED) ? ComplaintStatus.ACKNOWLEDGED : oldStatus;

                if (oldStatus != newStatus) {
                    complaintDAO.updateStatusAndResolvedAt(complaintId, newStatus, null, conn);
                }

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaintId);
                history.setChangedBy(currentAdmin.getUserId());
                history.setOldStatus(oldStatus);
                history.setNewStatus(newStatus);
                history.setRemarks("Assigned to staff " + staff.getFullName() + (notes != null && !notes.isBlank() ? " | Notes: " + notes.trim() : ""));
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} assigned to staff {} by admin {}", complaintId, staff.getFullName(), currentAdmin.getEmail());
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to assign staff, transaction rolled back: {}", e.getMessage());
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), "ASSIGN_STAFF", "COMPLAINT", complaintId,
                "Assigned to staff " + staff.getFullName()));
    }

    public void updateProgress(int complaintId, ComplaintStatus targetStatus, String workNote) throws SQLException {
        UserSession.requireRole(Role.STAFF, Role.ADMIN);
        User currentUser = UserSession.getCurrentUser();
        String note = ValidationUtil.requireNonBlank(workNote, "Work note");

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        // Staff assignment check
        if (currentUser.getRole() == Role.STAFF) {
            Optional<ComplaintAssignment> assignment = assignmentDAO.findActiveAssignment(complaintId);
            if (assignment.isEmpty() || assignment.get().getStaffId() != currentUser.getUserId()) {
                throw new SecurityException("Unauthorized: You can only update complaints assigned to you.");
            }
        }

        ComplaintStatus currentStatus = complaint.getStatus();
        if (targetStatus == null) {
            targetStatus = (currentStatus == ComplaintStatus.ACKNOWLEDGED || currentStatus == ComplaintStatus.SUBMITTED)
                    ? ComplaintStatus.IN_PROGRESS : currentStatus;
        }

        if (targetStatus != currentStatus && !currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                complaintDAO.updateStatusAndResolvedAt(complaintId, targetStatus, null, conn);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaintId);
                history.setChangedBy(currentUser.getUserId());
                history.setOldStatus(currentStatus);
                history.setNewStatus(targetStatus);
                history.setRemarks(note);
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} progress updated to {} by user ID {}", complaintId, targetStatus, currentUser.getUserId());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentUser.getUserId(), "UPDATE_PROGRESS", "COMPLAINT", complaintId,
                "Status: " + targetStatus + " | Note: " + note));
    }

    public void resolveComplaint(int complaintId, String resolutionNote) throws SQLException {
        UserSession.requireRole(Role.STAFF, Role.ADMIN);
        User currentUser = UserSession.getCurrentUser();
        String note = ValidationUtil.requireNonBlank(resolutionNote, "Resolution note");

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        if (currentUser.getRole() == Role.STAFF) {
            Optional<ComplaintAssignment> assignment = assignmentDAO.findActiveAssignment(complaintId);
            if (assignment.isEmpty() || assignment.get().getStaffId() != currentUser.getUserId()) {
                throw new SecurityException("Unauthorized: You can only resolve complaints assigned to you.");
            }
        }

        ComplaintStatus currentStatus = complaint.getStatus();
        if (!currentStatus.canTransitionTo(ComplaintStatus.RESOLVED)) {
            throw new IllegalStateException("Cannot transition from " + currentStatus + " directly to RESOLVED.");
        }

        Timestamp resolvedAt = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                complaintDAO.updateStatusAndResolvedAt(complaintId, ComplaintStatus.RESOLVED, resolvedAt, conn);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaintId);
                history.setChangedBy(currentUser.getUserId());
                history.setOldStatus(currentStatus);
                history.setNewStatus(ComplaintStatus.RESOLVED);
                history.setRemarks("Resolution: " + note);
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} marked RESOLVED by user ID {}", complaintId, currentUser.getUserId());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentUser.getUserId(), "RESOLVE_COMPLAINT", "COMPLAINT", complaintId,
                "Resolved: " + note));
    }

    public void rejectComplaint(int complaintId, String rejectionReason) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();
        String reason = ValidationUtil.requireNonBlank(rejectionReason, "Rejection reason");

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        ComplaintStatus currentStatus = complaint.getStatus();
        if (!currentStatus.canTransitionTo(ComplaintStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject complaint from current status " + currentStatus);
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                complaintDAO.updateStatusAndResolvedAt(complaintId, ComplaintStatus.REJECTED, null, conn);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaintId);
                history.setChangedBy(currentAdmin.getUserId());
                history.setOldStatus(currentStatus);
                history.setNewStatus(ComplaintStatus.REJECTED);
                history.setRemarks("Rejection: " + reason);
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} rejected by admin {}", complaintId, currentAdmin.getEmail());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), "REJECT_COMPLAINT", "COMPLAINT", complaintId,
                "Rejected: " + reason));
    }

    public void reopenComplaint(int complaintId, String remarks) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        ComplaintStatus currentStatus = complaint.getStatus();
        if (currentStatus != ComplaintStatus.RESOLVED && currentStatus != ComplaintStatus.REJECTED) {
            throw new IllegalStateException("Only RESOLVED or REJECTED complaints can be reopened.");
        }

        ComplaintStatus newStatus = ComplaintStatus.IN_PROGRESS;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                complaintDAO.updateStatusAndResolvedAt(complaintId, newStatus, null, conn);

                ComplaintStatusHistory history = new ComplaintStatusHistory();
                history.setComplaintId(complaintId);
                history.setChangedBy(currentAdmin.getUserId());
                history.setOldStatus(currentStatus);
                history.setNewStatus(newStatus);
                history.setRemarks("Complaint reopened: " + (remarks != null ? remarks : "Reopened for further investigation."));
                historyDAO.record(history, conn);

                conn.commit();
                logger.info("Complaint #{} reopened by admin {}", complaintId, currentAdmin.getEmail());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), "REOPEN_COMPLAINT", "COMPLAINT", complaintId,
                "Reopened: " + remarks));
    }

    public void submitFeedback(int complaintId, int rating, String comments) throws SQLException {
        UserSession.requireRole(Role.STUDENT);
        User currentStudent = UserSession.getCurrentUser();

        ValidationUtil.validateRange(rating, 1, 5, "Rating");

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        UserSession.checkStudentOwnership(complaint.getStudentId());

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("Feedback can only be submitted for RESOLVED complaints.");
        }

        if (feedbackDAO.findByComplaintId(complaintId).isPresent()) {
            throw new IllegalStateException("Feedback has already been submitted for this complaint.");
        }

        ComplaintFeedback feedback = new ComplaintFeedback(complaintId, currentStudent.getUserId(), rating, comments);
        feedbackDAO.create(feedback);
        logger.info("Feedback submitted for complaint #{} with rating {}", complaintId, rating);

        auditLogDAO.log(new AuditLog(currentStudent.getUserId(), "SUBMIT_FEEDBACK", "COMPLAINT", complaintId,
                "Rating: " + rating + " stars"));
    }

    public ComplaintDetailsDTO getComplaintDetails(int complaintId) throws SQLException {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint #" + complaintId + " not found."));

        // If student, check ownership
        if (currentUser.getRole() == Role.STUDENT) {
            UserSession.checkStudentOwnership(complaint.getStudentId());
        }

        List<ComplaintStatusHistory> history = historyDAO.findByComplaintId(complaintId);
        Optional<ComplaintAssignment> assignment = assignmentDAO.findActiveAssignment(complaintId);
        Optional<ComplaintFeedback> feedback = feedbackDAO.findByComplaintId(complaintId);

        return new ComplaintDetailsDTO(complaint, history, assignment, feedback);
    }

    public List<Complaint> listComplaints(String statusFilter, String priorityFilter,
                                         String categoryFilter, String buildingFilter,
                                         Boolean safetyCriticalOnly, String searchQuery,
                                         String sortBy, boolean sortAsc) throws SQLException {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();

        Integer studentId = null;
        Integer staffId = null;

        if (currentUser.getRole() == Role.STUDENT) {
            studentId = currentUser.getUserId(); // Strictly own complaints
        } else if (currentUser.getRole() == Role.STAFF) {
            staffId = currentUser.getUserId();   // Strictly assigned complaints
        }

        return complaintDAO.findComplaints(
                studentId, staffId, statusFilter, priorityFilter,
                categoryFilter, buildingFilter, safetyCriticalOnly,
                searchQuery, sortBy, sortAsc
        );
    }

    public List<Complaint> listAllComplaintsForAdmin(String statusFilter, String priorityFilter,
                                                    String categoryFilter, String buildingFilter,
                                                    Boolean safetyCriticalOnly, String searchQuery,
                                                    String sortBy, boolean sortAsc) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        return complaintDAO.findComplaints(
                null, null, statusFilter, priorityFilter,
                categoryFilter, buildingFilter, safetyCriticalOnly,
                searchQuery, sortBy, sortAsc
        );
    }

    public DashboardStats getDashboardStats() throws SQLException {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();

        Integer studentId = (currentUser.getRole() == Role.STUDENT) ? currentUser.getUserId() : null;
        Integer staffId = (currentUser.getRole() == Role.STAFF) ? currentUser.getUserId() : null;

        return complaintDAO.calculateDashboardStats(studentId, staffId);
    }

    public void exportComplaintsToCsv(List<Complaint> complaints, File destinationFile) throws IOException {
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();

        try (FileWriter writer = new FileWriter(destinationFile)) {
            // Header
            writer.write("Complaint ID,Title,Category,Building,Room Number,Severity,Safety Risk,People Affected,Operational Impact,Priority Score,Priority,Safety Critical,Status,Student Name,Student Email,Assigned Staff,Created At,Resolved At\n");

            for (Complaint c : complaints) {
                StringBuilder sb = new StringBuilder();
                sb.append(c.getComplaintId()).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getTitle())).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getCategory())).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getBuilding())).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getRoomNumber())).append(",");
                sb.append(c.getSeverity()).append(",");
                sb.append(c.getSafetyRisk()).append(",");
                sb.append(c.getPeopleAffected()).append(",");
                sb.append(c.getOperationalImpact()).append(",");
                sb.append(c.getPriorityScore()).append(",");
                sb.append(c.getPriority()).append(",");
                sb.append(c.isSafetyCritical()).append(",");
                sb.append(c.getStatus()).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getStudentName())).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getStudentEmail())).append(",");
                sb.append(ValidationUtil.escapeCsvCell(c.getAssignedStaffName())).append(",");
                sb.append(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "").append(",");
                sb.append(c.getResolvedAt() != null ? c.getResolvedAt().toString() : "");
                sb.append("\n");
                writer.write(sb.toString());
            }
        }

        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), "EXPORT_CSV", "REPORTS", null,
                "Exported " + complaints.size() + " complaints to " + destinationFile.getName()));
    }
}
