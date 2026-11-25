package com.smartcampus.service;

import com.smartcampus.config.DBConnection;
import com.smartcampus.dao.ComplaintDAO;
import com.smartcampus.dao.EmergencyEscalationDAO;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintEscalation;
import com.smartcampus.model.SlaSnapshot;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.SlaStatus;
import com.smartcampus.security.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Intelligence Engine A - Emergency Escalation & SLA Tests")
class EmergencyEscalationServiceTest {
    private EmergencyEscalationService service;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-11-25T10:00:00Z");
        service = new EmergencyEscalationService(Clock.fixed(baseTime, ZoneId.of("Asia/Kolkata")));
    }

    @AfterEach
    void tearDown() {
        UserSession.logout();
    }

    private Complaint makeComplaint(int id, PriorityLevel prio, ComplaintStatus status, Instant createdAt, boolean safety) {
        Complaint c = new Complaint();
        c.setComplaintId(id);
        c.setStudentId(4);
        c.setTitle("Test Hazard #" + id);
        c.setDescription("Sparking wiring hazard");
        c.setCategory("Electrical");
        c.setBuilding("Science Block A");
        c.setRoomNumber("Lab 204");
        c.setSeverity(5);
        c.setSafetyRisk(safety ? 5 : 1);
        c.setPeopleAffected(30);
        c.setOperationalImpact(4);
        c.setPriority(prio);
        c.setPriorityScore(prio == PriorityLevel.CRITICAL ? 90.0 : 40.0);
        c.setSafetyCritical(safety);
        c.setStatus(status);
        c.setCreatedAt(Timestamp.from(createdAt));
        return c;
    }

    @Test
    @DisplayName("SLA targets should map to 4h CRITICAL, 24h HIGH, 72h MEDIUM, 168h LOW")
    void testSlaTargetsAndDeadlines() {
        PriorityService ps = new PriorityService();
        assertEquals(4, ps.getSlaHours(PriorityLevel.CRITICAL));
        assertEquals(24, ps.getSlaHours(PriorityLevel.HIGH));
        assertEquals(72, ps.getSlaHours(PriorityLevel.MEDIUM));
        assertEquals(168, ps.getSlaHours(PriorityLevel.LOW));

        Complaint crit = makeComplaint(10, PriorityLevel.CRITICAL, ComplaintStatus.SUBMITTED, baseTime, true);
        SlaSnapshot snap = service.computeSlaSnapshot(crit, baseTime);
        assertEquals(Timestamp.from(baseTime.plus(4, ChronoUnit.HOURS)), snap.slaDeadline());
        assertEquals(SlaStatus.ON_TRACK, snap.slaStatus());
    }

    @Test
    @DisplayName("50%, 75%, 100%, and 125% SLA boundary progression should classify accurately")
    void testSlaBoundaryThresholds() {

        Complaint c = makeComplaint(11, PriorityLevel.CRITICAL, ComplaintStatus.ACKNOWLEDGED, baseTime, true);

        SlaSnapshot s40 = service.computeSlaSnapshot(c, baseTime.plus(96, ChronoUnit.MINUTES));
        assertEquals(SlaStatus.ON_TRACK, s40.slaStatus());
        assertEquals(EscalationLevel.NORMAL, s40.targetEscalationLevel());
        assertFalse(s40.earlyWarning50Triggered());

        SlaSnapshot s50 = service.computeSlaSnapshot(c, baseTime.plus(120, ChronoUnit.MINUTES));
        assertEquals(SlaStatus.ON_TRACK, s50.slaStatus());
        assertTrue(s50.earlyWarning50Triggered());

        SlaSnapshot s75 = service.computeSlaSnapshot(c, baseTime.plus(180, ChronoUnit.MINUTES));
        assertEquals(SlaStatus.APPROACHING_DEADLINE, s75.slaStatus());
        assertEquals(EscalationLevel.EARLY_WARNING, s75.targetEscalationLevel());

        SlaSnapshot s100 = service.computeSlaSnapshot(c, baseTime.plus(241, ChronoUnit.MINUTES));
        assertEquals(SlaStatus.BREACHED, s100.slaStatus());
        assertEquals(EscalationLevel.SLA_BREACH, s100.targetEscalationLevel());
        assertTrue(s100.breachMinutes() >= 1);

        SlaSnapshot s125 = service.computeSlaSnapshot(c, baseTime.plus(300, ChronoUnit.MINUTES));
        assertEquals(SlaStatus.BREACHED, s125.slaStatus());
        assertEquals(EscalationLevel.ADMIN_ESCALATION, s125.targetEscalationLevel());
    }

    @Test
    @DisplayName("Resolved and Rejected complaints must be excluded from breach escalation")
    void testResolvedAndRejectedExcluded() {
        Complaint resolved = makeComplaint(12, PriorityLevel.CRITICAL, ComplaintStatus.RESOLVED,
                baseTime.minus(24, ChronoUnit.HOURS), true);
        resolved.setResolvedAt(Timestamp.from(baseTime.minus(22, ChronoUnit.HOURS)));

        SlaSnapshot resSnap = service.computeSlaSnapshot(resolved, baseTime);
        assertEquals(SlaStatus.RESOLVED, resSnap.slaStatus());
        assertEquals(EscalationLevel.NORMAL, resSnap.targetEscalationLevel());
        assertEquals(0L, resSnap.breachMinutes());

        Complaint rejected = makeComplaint(13, PriorityLevel.HIGH, ComplaintStatus.REJECTED,
                baseTime.minus(48, ChronoUnit.HOURS), false);
        SlaSnapshot rejSnap = service.computeSlaSnapshot(rejected, baseTime);
        assertEquals(SlaStatus.RESOLVED, rejSnap.slaStatus());
        assertEquals(EscalationLevel.NORMAL, rejSnap.targetEscalationLevel());
    }

    @Test
    @DisplayName("Timezone correctness: UTC and IST clocks on same Instant yield identical SLA calculations")
    void testTimezoneCorrectness() {
        Complaint c = makeComplaint(14, PriorityLevel.HIGH, ComplaintStatus.SUBMITTED, baseTime, false);
        EmergencyEscalationService istSvc = new EmergencyEscalationService(Clock.fixed(baseTime.plus(19, ChronoUnit.HOURS), ZoneId.of("Asia/Kolkata")));
        EmergencyEscalationService utcSvc = new EmergencyEscalationService(Clock.fixed(baseTime.plus(19, ChronoUnit.HOURS), ZoneId.of("UTC")));

        SlaSnapshot istSnap = istSvc.computeSlaSnapshot(c);
        SlaSnapshot utcSnap = utcSvc.computeSlaSnapshot(c);

        assertEquals(istSnap.elapsedMinutes(), utcSnap.elapsedMinutes());
        assertEquals(istSnap.remainingMinutes(), utcSnap.remainingMinutes());
        assertEquals(istSnap.slaStatus(), utcSnap.slaStatus());
        assertEquals(SlaStatus.APPROACHING_DEADLINE, istSnap.slaStatus());
    }

    @Test
    @DisplayName("Critical safety override explanation should state clear safety hazard reason")
    void testSafetyReasonExplanation() {
        Complaint c = makeComplaint(15, PriorityLevel.HIGH, ComplaintStatus.SUBMITTED, baseTime, true);
        c.setSafetyRisk(5);
        String reason = service.buildSafetyClassificationReason(c);
        assertTrue(reason.contains("Safety Risk = 5/5"));
    }

    @Test
    @DisplayName("Demo A & Database Integration: restart reconciliation, duplicate prevention, concurrency safety, admin ack")
    void testLiveEscalationReconciliationDeduplicationAndDemoA() throws Exception {
        Assumptions.assumeTrue(DBConnection.testConnection(), "MySQL Server must be running");

        UserDAO userDAO = new UserDAO();
        User admin = userDAO.findByEmail("admin@campus.edu").orElseThrow();
        User student = userDAO.findByEmail("alice@student.campus.edu").orElseThrow();
        User staff = userDAO.findByEmail("electrician@campus.edu").orElseThrow();

        ComplaintService complaintService = new ComplaintService();
        ComplaintDAO complaintDAO = new ComplaintDAO();
        EmergencyEscalationDAO escDAO = new EmergencyEscalationDAO();

        UserSession.login(student);
        Complaint c = new Complaint();
        c.setTitle("[DEMO-A] Exposed 440V Panel Sparking");
        c.setDescription("High-voltage breaker panel arcing near student lab.");
        c.setCategory("Electrical");
        c.setBuilding("Demo Science Block");
        c.setRoomNumber("Lab 999");
        c.setSeverity(5);
        c.setSafetyRisk(5);
        c.setPeopleAffected(60);
        c.setOperationalImpact(5);

        Complaint saved = complaintDAO.findById(complaintService.submitComplaint(c).getComplaintId()).orElseThrow();
        int compId = saved.getComplaintId();
        assertTrue(saved.isSafetyCritical(), "Safety override must activate");
        assertEquals(PriorityLevel.CRITICAL, saved.getPriority());
        assertNotNull(saved.getCreatedAt(), "DB created_at timestamp must be populated");

        try {

            UserSession.login(admin);
            complaintService.assignStaff(compId, staff.getUserId(), "[DEMO-A] Immediate electrical isolation");

            Instant future6h = saved.getCreatedAt().toInstant().plus(6, ChronoUnit.HOURS);
            EmergencyEscalationService futureSvc = new EmergencyEscalationService(Clock.fixed(future6h, ZoneId.of("Asia/Kolkata")));

            ExecutorService pool = Executors.newFixedThreadPool(4);
            List<Callable<int[]>> tasks = List.of(
                    () -> futureSvc.evaluateSingleComplaint(saved, future6h, 0, admin.getUserId(), new java.util.ArrayList<>()),
                    () -> futureSvc.evaluateSingleComplaint(saved, future6h, 0, admin.getUserId(), new java.util.ArrayList<>()),
                    () -> futureSvc.evaluateSingleComplaint(saved, future6h, 0, admin.getUserId(), new java.util.ArrayList<>()),
                    () -> futureSvc.evaluateSingleComplaint(saved, future6h, 0, admin.getUserId(), new java.util.ArrayList<>())
            );
            List<Future<int[]>> futures = pool.invokeAll(tasks);
            pool.shutdown();
            for (Future<int[]> f : futures) {
                assertNotNull(f.get());
            }

            int[] secondRun = futureSvc.evaluateSingleComplaint(saved, future6h, 3, admin.getUserId(), new java.util.ArrayList<>());
            assertEquals(0, secondRun[0] + secondRun[1] + secondRun[2], "Second run must not create duplicate escalations");

            List<ComplaintEscalation> history = futureSvc.getEscalationHistory(compId);
            assertEquals(3, history.size(), "Restart reconciliation should log Levels 1, 2, 3 once each");
            assertEquals(1, history.get(0).getEscalationLevel());
            assertEquals(2, history.get(1).getEscalationLevel());
            assertEquals(3, history.get(2).getEscalationLevel());

            futureSvc.acknowledgeAlert(compId, "[DEMO-A] Admin dispatched backup crew");
            List<ComplaintEscalation> acked = futureSvc.getEscalationHistory(compId);
            assertTrue(acked.stream().anyMatch(e -> e.getAcknowledgedAt() != null && admin.getUserId() == e.getAcknowledgedBy()));

        } finally {

            try (Connection conn = DBConnection.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit_logs WHERE entity_type = 'COMPLAINT' AND entity_id = ?")) {
                    ps.setInt(1, compId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM complaints WHERE complaint_id = ?")) {
                    ps.setInt(1, compId);
                    ps.executeUpdate();
                }
            }
        }
    }

    @Test
    @DisplayName("Emergency Operations visibility: normal classroom MEDIUM excluded while ON_TRACK, surfaces on SLA breach; HIGH on >=50% warning; CRITICAL immediate; RESOLVED excluded")
    void testEmergencyVisibilityConditionsAllScenarios() {

        Complaint classroom = makeComplaint(101, PriorityLevel.MEDIUM, ComplaintStatus.ACKNOWLEDGED,
                baseTime.minus(1, ChronoUnit.HOURS), false);
        classroom.setTitle("Not working of Lights");
        SlaSnapshot classSla = service.computeSlaSnapshot(classroom, baseTime);
        assertFalse(service.isEmergencyEligible(classroom, classSla, EscalationLevel.NORMAL),
                "Fresh ON_TRACK classroom MEDIUM complaint should stay in All Complaints, not Emergency Operations");

        Complaint breachedMed = makeComplaint(102, PriorityLevel.MEDIUM, ComplaintStatus.ACKNOWLEDGED,
                baseTime.minus(75, ChronoUnit.HOURS), false);
        SlaSnapshot breachSla = service.computeSlaSnapshot(breachedMed, baseTime);
        assertEquals(SlaStatus.BREACHED, breachSla.slaStatus());
        assertTrue(service.isEmergencyEligible(breachedMed, breachSla, EscalationLevel.SLA_BREACH),
                "SLA-breached MEDIUM complaint must appear in Emergency Operations");

        Complaint corridorEarly = makeComplaint(103, PriorityLevel.HIGH, ComplaintStatus.SUBMITTED,
                baseTime.minus(2, ChronoUnit.HOURS), false);
        assertFalse(service.isEmergencyEligible(corridorEarly, service.computeSlaSnapshot(corridorEarly, baseTime), EscalationLevel.NORMAL));

        Complaint corridorWarn = makeComplaint(104, PriorityLevel.HIGH, ComplaintStatus.SUBMITTED,
                baseTime.minus(13, ChronoUnit.HOURS), false);
        SlaSnapshot warnSla = service.computeSlaSnapshot(corridorWarn, baseTime);
        assertTrue(warnSla.earlyWarning50Triggered());
        assertTrue(service.isEmergencyEligible(corridorWarn, warnSla, EscalationLevel.NORMAL),
                "HIGH complaint meeting >=50% escalation warning must appear in Emergency Operations");

        Complaint exitCrit = makeComplaint(105, PriorityLevel.CRITICAL, ComplaintStatus.SUBMITTED, baseTime, true);
        assertTrue(service.isEmergencyEligible(exitCrit, service.computeSlaSnapshot(exitCrit, baseTime), EscalationLevel.NORMAL),
                "Safety-critical complaint must appear immediately even before SLA warning");

        Complaint resolvedCrit = makeComplaint(106, PriorityLevel.CRITICAL, ComplaintStatus.RESOLVED,
                baseTime.minus(10, ChronoUnit.HOURS), true);
        resolvedCrit.setResolvedAt(Timestamp.from(baseTime.minus(8, ChronoUnit.HOURS)));
        assertFalse(service.isEmergencyEligible(resolvedCrit, service.computeSlaSnapshot(resolvedCrit, baseTime), EscalationLevel.ADMIN_ESCALATION),
                "Resolved complaints must never appear as active emergencies");
    }
}
