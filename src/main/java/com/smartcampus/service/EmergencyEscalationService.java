package com.smartcampus.service;

import com.smartcampus.config.AppConfig;
import com.smartcampus.config.DBConnection;
import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.dao.ComplaintDAO;
import com.smartcampus.dao.ComplaintStatusHistoryDAO;
import com.smartcampus.dao.EmergencyEscalationDAO;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintEscalation;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.EmergencyDashboardMetrics;
import com.smartcampus.model.EmergencyIncidentDTO;
import com.smartcampus.model.SlaSnapshot;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.Role;
import com.smartcampus.model.enums.SlaStatus;
import com.smartcampus.security.UserSession;
import com.smartcampus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class EmergencyEscalationService {
    private static final Logger logger = LoggerFactory.getLogger(EmergencyEscalationService.class);

    public record EscalationCycleResult(
            int scannedComplaints,
            int newWarnings,
            int newBreaches,
            int newAdminEscalations,
            List<String> notifications
    ) {
        public int totalNewEvents() {
            return newWarnings + newBreaches + newAdminEscalations;
        }
    }

    public record EmergencyCommandData(
            EmergencyDashboardMetrics metrics,
            List<EmergencyIncidentDTO> incidents
    ) {}

    private final ComplaintDAO complaintDAO;
    private final EmergencyEscalationDAO escalationDAO;
    private final ComplaintStatusHistoryDAO historyDAO;
    private final AuditLogDAO auditLogDAO;
    private final UserDAO userDAO;
    private final PriorityService priorityService;
    private final Clock clock;
    private final ReentrantLock evaluationLock = new ReentrantLock();

    public EmergencyEscalationService() {
        this(new ComplaintDAO(), new EmergencyEscalationDAO(), new ComplaintStatusHistoryDAO(),
                new AuditLogDAO(), new UserDAO(), new PriorityService(), Clock.systemDefaultZone());
    }

    public EmergencyEscalationService(Clock clock) {
        this(new ComplaintDAO(), new EmergencyEscalationDAO(), new ComplaintStatusHistoryDAO(),
                new AuditLogDAO(), new UserDAO(), new PriorityService(), clock);
    }

    public EmergencyEscalationService(ComplaintDAO complaintDAO, EmergencyEscalationDAO escalationDAO,
                                      ComplaintStatusHistoryDAO historyDAO, AuditLogDAO auditLogDAO,
                                      UserDAO userDAO, PriorityService priorityService, Clock clock) {
        this.complaintDAO = complaintDAO;
        this.escalationDAO = escalationDAO;
        this.historyDAO = historyDAO;
        this.auditLogDAO = auditLogDAO;
        this.userDAO = userDAO;
        this.priorityService = priorityService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public SlaSnapshot computeSlaSnapshot(Complaint c) {
        return computeSlaSnapshot(c, clock.instant());
    }

    public SlaSnapshot computeSlaSnapshot(Complaint c, Instant now) {
        if (c == null) {
            throw new IllegalArgumentException("Complaint cannot be null.");
        }
        Instant effectiveNow = now != null ? now : clock.instant();
        int slaHours = priorityService.getSlaHours(c.getPriority());

        Instant slaStart = (c.getCreatedAt() != null) ? c.getCreatedAt().toInstant() : effectiveNow;
        Instant slaDeadline = slaStart.plus(Duration.ofHours(slaHours));

        if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            Instant endInstant = (c.getResolvedAt() != null) ? c.getResolvedAt().toInstant() : effectiveNow;
            long elapsedMins = Math.max(0L, Duration.between(slaStart, endInstant).toMinutes());
            long totalMins = Math.max(1L, Duration.ofHours(slaHours).toMinutes());
            double pct = (elapsedMins * 100.0) / totalMins;
            return new SlaSnapshot(
                    c.getComplaintId(),
                    slaHours,
                    Timestamp.from(slaStart),
                    Timestamp.from(slaDeadline),
                    elapsedMins,
                    0L,
                    0L,
                    pct,
                    false,
                    SlaStatus.RESOLVED,
                    EscalationLevel.NORMAL
            );
        }

        long totalMillis = Math.max(1L, Duration.ofHours(slaHours).toMillis());
        long elapsedMillis = Math.max(0L, Duration.between(slaStart, effectiveNow).toMillis());
        long elapsedMinutes = elapsedMillis / 60_000L;
        double elapsedPercent = (elapsedMillis * 100.0) / totalMillis;

        long remainingMinutes = effectiveNow.isBefore(slaDeadline)
                ? Math.max(0L, Duration.between(effectiveNow, slaDeadline).toMinutes())
                : 0L;
        long breachMinutes = effectiveNow.isAfter(slaDeadline)
                ? Math.max(1L, Duration.between(slaDeadline, effectiveNow).toMinutes())
                : 0L;

        int early50 = AppConfig.getIntProperty("sla.warning.early.pct", 50);
        int approaching75 = AppConfig.getIntProperty("sla.warning.approaching.pct", 75);
        int breach100 = AppConfig.getIntProperty("sla.breach.pct", 100);
        int admin125 = AppConfig.getIntProperty("sla.escalation.admin.threshold.pct", 125);

        boolean early50Flag = elapsedPercent >= early50;
        SlaStatus slaStatus;
        EscalationLevel targetLevel;

        if (elapsedPercent >= admin125) {
            slaStatus = SlaStatus.BREACHED;
            targetLevel = EscalationLevel.ADMIN_ESCALATION;
        } else if (elapsedPercent >= breach100) {
            slaStatus = SlaStatus.BREACHED;
            targetLevel = EscalationLevel.SLA_BREACH;
        } else if (elapsedPercent >= approaching75) {
            slaStatus = SlaStatus.APPROACHING_DEADLINE;
            targetLevel = EscalationLevel.EARLY_WARNING;
        } else {
            slaStatus = SlaStatus.ON_TRACK;
            targetLevel = EscalationLevel.NORMAL;
        }

        return new SlaSnapshot(
                c.getComplaintId(),
                slaHours,
                Timestamp.from(slaStart),
                Timestamp.from(slaDeadline),
                elapsedMinutes,
                remainingMinutes,
                breachMinutes,
                elapsedPercent,
                early50Flag,
                slaStatus,
                targetLevel
        );
    }

    public String buildSafetyClassificationReason(Complaint c) {
        if (c == null) return "";
        List<String> reasons = new ArrayList<>();
        PriorityService.ContextHazardAssessment ctx = priorityService.assessContextHazard(
                c.getCategory(), c.getTitle(), c.getDescription(), c.getRoomNumber());
        if (ctx.reason() != null) {
            reasons.add(ctx.reason());
        }
        if (c.getSafetyRisk() >= 4) {
            reasons.add("Severe physical safety hazard (Safety Risk = " + c.getSafetyRisk() + "/5)");
        } else if (c.getSeverity() == 5 && c.getSafetyRisk() >= 2) {
            reasons.add("Maximum technical severity (5/5) paired with physical safety risk (" + c.getSafetyRisk() + "/5)");
        }
        if (c.getPriority() == PriorityLevel.CRITICAL) {
            reasons.add(String.format("Priority score %.1f/100 meets CRITICAL threshold (>=75.0)", c.getPriorityScore()));
        }
        if (c.getCategory() != null && c.getCategory().equalsIgnoreCase("Safety and Security")
                && (c.getSafetyRisk() >= 3 || c.getSeverity() >= 4)) {
            reasons.add("Classified under Safety & Security urgent response category");
        }
        if (reasons.isEmpty()) {
            return c.isSafetyCritical()
                    ? "Safety-critical override triggered during priority assessment."
                    : "Standard SLA monitoring (" + priorityService.getSlaHours(c.getPriority()) + "h target).";
        }
        return String.join("; ", reasons);
    }

    public EscalationCycleResult evaluateAndEscalateActiveComplaints() throws SQLException {
        return evaluateAndEscalateActiveComplaints(clock.instant());
    }

    public EscalationCycleResult evaluateAndEscalateActiveComplaints(Instant evaluationInstant) throws SQLException {
        if (!evaluationLock.tryLock()) {
            logger.debug("Skipping overlapping emergency evaluation cycle; another cycle is active.");
            return new EscalationCycleResult(0, 0, 0, 0, List.of());
        }
        try {
            List<Complaint> allActive = complaintDAO.findActiveComplaintsForEmergency(null);

            Map<Integer, ComplaintEscalation> latestMap = escalationDAO.findLatestByComplaintMap();
            int actorId = resolveAuditActorId();

            int newWarnings = 0;
            int newBreaches = 0;
            int newAdminEsc = 0;
            List<String> notifications = new ArrayList<>();

            for (Complaint c : allActive) {
                int currentLevel = latestMap.containsKey(c.getComplaintId())
                        ? latestMap.get(c.getComplaintId()).getEscalationLevel()
                        : 0;
                int[] counts = evaluateSingleComplaint(c, evaluationInstant, currentLevel, actorId, notifications);
                newWarnings += counts[0];
                newBreaches += counts[1];
                newAdminEsc += counts[2];
            }

            if (newWarnings + newBreaches + newAdminEsc > 0) {
                logger.info("Emergency scan processed {} active complaints: warnings={}, breaches={}, adminEscalations={}",
                        allActive.size(), newWarnings, newBreaches, newAdminEsc);
            }

            return new EscalationCycleResult(allActive.size(), newWarnings, newBreaches, newAdminEsc, notifications);
        } finally {
            evaluationLock.unlock();
        }
    }

    public int[] evaluateSingleComplaint(Complaint c, Instant evaluationInstant, int currentLevel,
                                         int actorId, List<String> notifications) throws SQLException {
        int[] counts = new int[3];
        if (c == null || c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            return counts;
        }
        SlaSnapshot sla = computeSlaSnapshot(c, evaluationInstant);
        int targetLevel = sla.targetEscalationLevel().getLevel();
        if (targetLevel <= currentLevel) {
            return counts;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT status FROM complaints WHERE complaint_id = ? FOR UPDATE";
                try (PreparedStatement lockPs = conn.prepareStatement(lockSql)) {
                    lockPs.setInt(1, c.getComplaintId());
                    try (ResultSet rs = lockPs.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return counts;
                        }
                        ComplaintStatus dbStatus = ComplaintStatus.fromString(rs.getString("status"));
                        if (dbStatus == ComplaintStatus.RESOLVED || dbStatus == ComplaintStatus.REJECTED) {
                            conn.rollback();
                            return counts;
                        }
                    }
                }

                for (int lvl = Math.max(1, currentLevel + 1); lvl <= targetLevel; lvl++) {
                    EscalationLevel escEnum = EscalationLevel.fromLevel(lvl);
                    ComplaintEscalation esc = new ComplaintEscalation();
                    esc.setComplaintId(c.getComplaintId());
                    esc.setEscalationLevel(lvl);
                    esc.setPreviousLevel(lvl - 1);
                    esc.setSlaStatus(lvl >= 2 ? SlaStatus.BREACHED : SlaStatus.APPROACHING_DEADLINE);
                    esc.setBreachAt(lvl >= 2 ? sla.slaDeadline() : null);
                    esc.setEscalationReason(buildEscalationReasonText(c, sla, escEnum));

                    boolean inserted = escalationDAO.insertEscalationIfAbsent(esc, conn);
                    if (inserted) {
                        if (lvl == 1) {
                            counts[0]++;
                            if (notifications != null) {
                                notifications.add("SLA Warning (75%): #" + c.getComplaintId() + " " + c.getTitle() + " [" + c.getBuilding() + "]");
                            }
                        } else if (lvl == 2) {
                            counts[1]++;
                            if (notifications != null) {
                                notifications.add("SLA BREACH: #" + c.getComplaintId() + " " + c.getTitle() + " [" + c.getBuilding() + " - " + c.getRoomNumber() + "]");
                            }
                        } else if (lvl >= 3) {
                            counts[2]++;
                            if (notifications != null) {
                                notifications.add("ADMIN ESCALATION (Level 3): #" + c.getComplaintId() + " " + c.getTitle() + " requires immediate reassignment or intervention!");
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        for (int lvl = Math.max(1, currentLevel + 1); lvl <= targetLevel; lvl++) {
            if (counts[0] + counts[1] + counts[2] > 0) {
                String action = switch (lvl) {
                    case 1 -> "SLA_WARNING";
                    case 2 -> "SLA_BREACH";
                    default -> "ESCALATION_LEVEL_CHANGE";
                };
                auditLogDAO.log(new AuditLog(actorId, action, "COMPLAINT", c.getComplaintId(),
                        String.format("Escalated to Level %d (%s) at %.0f%% of %dh SLA. Location: %s - %s.",
                                lvl, EscalationLevel.fromLevel(lvl).getDisplayName(),
                                sla.elapsedPercent(), sla.slaTargetHours(), c.getBuilding(), c.getRoomNumber())));
            }
        }
        return counts;
    }

    public EmergencyCommandData getEmergencyCommandData() throws SQLException {
        UserSession.requireRole(Role.ADMIN, Role.STAFF);
        User currentUser = UserSession.getCurrentUser();
        Integer staffFilter = (currentUser.getRole() == Role.STAFF) ? currentUser.getUserId() : null;

        List<Complaint> activeComplaints = complaintDAO.findActiveComplaintsForEmergency(staffFilter);

        Map<Integer, ComplaintEscalation> latestMap = escalationDAO.findLatestByComplaintMap();
        Instant now = clock.instant();

        int activeCritical = 0;
        int approaching = 0;
        int breached = 0;
        int escalated = 0;

        List<EmergencyIncidentDTO> incidents = new ArrayList<>();
        for (Complaint c : activeComplaints) {
            SlaSnapshot sla = computeSlaSnapshot(c, now);
            ComplaintEscalation latestEsc = latestMap.get(c.getComplaintId());
            int recordedLevel = latestEsc != null ? latestEsc.getEscalationLevel() : 0;
            EscalationLevel effectiveLevel = EscalationLevel.fromLevel(
                    Math.max(recordedLevel, sla.targetEscalationLevel().getLevel())
            );

            boolean isEmergency = isEmergencyEligible(c, sla, effectiveLevel);

            if (c.isSafetyCritical() || c.getPriority() == PriorityLevel.CRITICAL) activeCritical++;
            if (sla.slaStatus() == SlaStatus.APPROACHING_DEADLINE) approaching++;
            if (sla.slaStatus() == SlaStatus.BREACHED) breached++;
            if (effectiveLevel.getLevel() >= 2) escalated++;

            if (isEmergency) {
                String safetyReason = buildSafetyClassificationReason(c);
                String escReason = latestEsc != null ? latestEsc.getEscalationReason()
                        : buildEscalationReasonText(c, sla, effectiveLevel);
                boolean ack = latestEsc != null && latestEsc.getAcknowledgedAt() != null;

                incidents.add(new EmergencyIncidentDTO(
                        c,
                        sla,
                        effectiveLevel,
                        escReason,
                        ack,
                        latestEsc != null ? latestEsc.getAcknowledgedAt() : null,
                        latestEsc != null ? latestEsc.getAcknowledgedByName() : null,
                        latestEsc != null ? latestEsc.getInterventionNote() : null,
                        safetyReason
                ));
            }
        }

        incidents.sort(Comparator
                .comparing((EmergencyIncidentDTO d) -> d.complaint().isSafetyCritical()).reversed()
                .thenComparing((EmergencyIncidentDTO d) -> d.currentEscalationLevel().getLevel(), Comparator.reverseOrder())
                .thenComparing(d -> d.slaSnapshot().breachMinutes(), Comparator.reverseOrder())
                .thenComparing(d -> d.complaint().getPriorityScore(), Comparator.reverseOrder()));

        double[] responseStats = escalationDAO.getEmergencyResponseStats();
        EmergencyDashboardMetrics metrics = new EmergencyDashboardMetrics(
                activeCritical, approaching, breached, escalated, responseStats[0], responseStats[1]
        );
        return new EmergencyCommandData(metrics, incidents);
    }

    public void acknowledgeAlert(int complaintId, String note) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User admin = UserSession.getCurrentUser();
        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint #" + complaintId + " not found."));

        String cleanNote = (note != null && !note.isBlank()) ? note.trim() : "Emergency alert acknowledged by administrator.";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                escalationDAO.acknowledgeEscalation(complaintId, admin.getUserId(), cleanNote, conn);
                historyDAO.record(new ComplaintStatusHistory(
                        complaintId, admin.getUserId(), complaint.getStatus(), complaint.getStatus(),
                        "Admin Emergency Acknowledgment: " + cleanNote), conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(admin.getUserId(), "ADMIN_EMERGENCY_ACK", "COMPLAINT", complaintId,
                "Acknowledged alert for '" + complaint.getTitle() + "' | Note: " + cleanNote));
    }

    public void addInterventionNote(int complaintId, String note) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User admin = UserSession.getCurrentUser();
        String validated = ValidationUtil.requireNonBlank(note, "Intervention Note");

        Complaint complaint = complaintDAO.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint #" + complaintId + " not found."));

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                escalationDAO.acknowledgeEscalation(complaintId, admin.getUserId(), validated, conn);
                historyDAO.record(new ComplaintStatusHistory(
                        complaintId, admin.getUserId(), complaint.getStatus(), complaint.getStatus(),
                        "Admin Intervention Note: " + validated), conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(admin.getUserId(), "EMERGENCY_INTERVENTION", "COMPLAINT", complaintId,
                "Intervention note added: " + validated));
    }

    public List<ComplaintEscalation> getEscalationHistory(int complaintId) throws SQLException {
        UserSession.requireLoggedIn();
        User current = UserSession.getCurrentUser();
        if (current.getRole() == Role.STUDENT) {
            Complaint c = complaintDAO.findById(complaintId)
                    .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));
            UserSession.checkStudentOwnership(c.getStudentId());
        }
        return escalationDAO.findByComplaintId(complaintId);
    }

    private String buildEscalationReasonText(Complaint c, SlaSnapshot sla, EscalationLevel level) {
        return switch (level) {
            case ADMIN_ESCALATION -> String.format(
                    "Administrative Escalation (Level 3): Complaint #%d in %s (%s) exceeded SLA by %s (%.0f%% elapsed). Immediate reassignment or supervisor intervention recommended.",
                    c.getComplaintId(), c.getBuilding(), c.getRoomNumber(),
                    sla.formatBreachDisplay(), sla.elapsedPercent());
            case SLA_BREACH -> String.format(
                    "SLA Deadline Breached (Level 2): %dh turnaround window expired %s ago in %s (%s).",
                    sla.slaTargetHours(), sla.formatBreachDisplay(), c.getBuilding(), c.getRoomNumber());
            case EARLY_WARNING -> String.format(
                    "SLA Approaching Deadline (Level 1): %.0f%% of %dh SLA elapsed (%s remaining).",
                    sla.elapsedPercent(), sla.slaTargetHours(), sla.formatCountdownDisplay());
            case NORMAL -> c.isSafetyCritical()
                    ? "Active safety-critical incident monitored from submission (" + sla.formatCountdownDisplay() + ")."
                    : "Within SLA (" + sla.formatCountdownDisplay() + ").";
        };
    }

    public boolean isEmergencyEligible(Complaint c, SlaSnapshot sla, EscalationLevel effectiveLevel) {
        if (c == null || c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
            return false;
        }
        boolean isCriticalOrSafety = c.isSafetyCritical() || c.getPriority() == PriorityLevel.CRITICAL;
        boolean isHighEscalationEligible = c.getPriority() == PriorityLevel.HIGH
                && (sla.earlyWarning50Triggered() || sla.slaStatus() != SlaStatus.ON_TRACK || effectiveLevel.getLevel() > 0);
        boolean isSlaEscalatedOrBreached = sla.slaStatus() == SlaStatus.APPROACHING_DEADLINE
                || sla.slaStatus() == SlaStatus.BREACHED
                || effectiveLevel.getLevel() > 0;
        return isCriticalOrSafety || isHighEscalationEligible || isSlaEscalatedOrBreached;
    }

    private int resolveAuditActorId() {
        User current = UserSession.getCurrentUser();
        if (current != null && current.getUserId() > 0) {
            return current.getUserId();
        }
        try {
            List<User> admins = userDAO.findAll("ADMIN", true, null);
            if (!admins.isEmpty()) {
                return admins.get(0).getUserId();
            }
            List<User> all = userDAO.findAll(null, true, null);
            if (!all.isEmpty()) {
                return all.get(0).getUserId();
            }
        } catch (SQLException ignored) {}
        return 1;
    }
}
