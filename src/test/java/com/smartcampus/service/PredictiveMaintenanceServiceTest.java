package com.smartcampus.service;

import com.smartcampus.config.DBConnection;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.MaintenanceRiskAssessment;
import com.smartcampus.model.PreventiveRecommendation;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.RiskLevel;
import com.smartcampus.security.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Intelligence Engine B - Predictive Maintenance & Risk Scoring Tests")
class PredictiveMaintenanceServiceTest {
    private PredictiveMaintenanceService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        service = new PredictiveMaintenanceService();
        now = Instant.parse("2026-11-25T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        UserSession.logout();
    }

    private Complaint makeComplaint(int id, String building, String room, String category, String title,
                                    double score, PriorityLevel prio, boolean safety, int daysAgo) {
        Complaint c = new Complaint();
        c.setComplaintId(id);
        c.setStudentId(4);
        c.setBuilding(building);
        c.setRoomNumber(room);
        c.setCategory(category);
        c.setTitle(title);
        c.setDescription(title + " repeated issue");
        c.setPriorityScore(score);
        c.setPriority(prio);
        c.setSafetyCritical(safety);
        c.setCreatedAt(Timestamp.from(now.minus(daysAgo, ChronoUnit.DAYS)));
        return c;
    }

    @Test
    @DisplayName("Empty historical dataset should return clean zero-assessment result without error")
    void testEmptyDataset() {
        PredictiveMaintenanceService.AnalysisExecutionResult res = service.analyzeHistoricalComplaints(
                List.of(), List.of(), 30,
                Timestamp.from(now.minus(30, ChronoUnit.DAYS)), Timestamp.from(now), 1);
        assertEquals(0, res.analysis().getTotalComplaintsAnalyzed());
        assertTrue(res.assessments().isEmpty());
        assertTrue(res.recommendations().isEmpty());
    }

    @Test
    @DisplayName("Recurring electrical wiring failures in one building should detect pattern & outscore low-volume building")
    void testRecurrenceTrendAndRiskComparison() {
        List<Complaint> cur = List.of(
                makeComplaint(1, "Science Block A", "Lab 204", "Electrical", "Sparking electrical wiring in junction box", 85.0, PriorityLevel.CRITICAL, true, 1),
                makeComplaint(2, "Science Block A", "Lab 204", "Electrical", "Exposed wiring near bench 3", 78.0, PriorityLevel.CRITICAL, true, 2),
                makeComplaint(3, "Science Block A", "Lab 204", "Electrical", "Circuit breaker tripping due to wiring fault", 62.0, PriorityLevel.HIGH, false, 4),
                makeComplaint(4, "Science Block A", "Lab 201", "Electrical", "Voltage surge and burnt wiring odor", 70.0, PriorityLevel.HIGH, false, 6),
                makeComplaint(5, "Arts Wing B", "Room 12", "Plumbing and Water Supply", "Minor tap drip", 20.0, PriorityLevel.LOW, false, 20)
        );
        List<Complaint> prev = List.of(
                makeComplaint(6, "Science Block A", "Lab 204", "Electrical", "Minor switch fault", 30.0, PriorityLevel.MEDIUM, false, 45)
        );

        PredictiveMaintenanceService.AnalysisExecutionResult res = service.analyzeHistoricalComplaints(
                cur, prev, 30, Timestamp.from(now.minus(30, ChronoUnit.DAYS)), Timestamp.from(now), 1);

        assertEquals(2, res.assessments().size());
        MaintenanceRiskAssessment top = res.assessments().get(0);
        MaintenanceRiskAssessment second = res.assessments().get(1);

        assertEquals("Science Block A", top.getBuilding());
        assertEquals("Electrical", top.getCategory());
        assertEquals(4, top.getIncidentCount());
        assertTrue(top.getRiskScore() >= 50.0, "Recurring critical building must score HIGH or CRITICAL");
        assertTrue(top.getRiskScore() > second.getRiskScore(), "High recurrence building must outscore comparison building");
        assertTrue(top.getContributingFactors().contains("RULE 1"));
        assertTrue(top.getContributingFactors().contains("RULE 2"));
        assertTrue(top.getContributingFactors().contains("RULE 3"));
        assertTrue(top.getContributingFactors().contains("RULE 5"));
        assertTrue(top.getContributingFactors().contains("RULE 6"));
        assertTrue(top.getRecommendation().toLowerCase().contains("electrical"));
    }

    @Test
    @DisplayName("RiskLevel boundaries: 0-24 LOW, 25-49 MODERATE, 50-74 HIGH, 75-100 CRITICAL")
    void testRiskLevelBoundaries() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0.0));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(24.99));
        assertEquals(RiskLevel.MODERATE, RiskLevel.fromScore(25.0));
        assertEquals(RiskLevel.MODERATE, RiskLevel.fromScore(49.99));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(50.0));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(74.99));
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(75.0));
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100.0));
    }

    @Test
    @DisplayName("Demo B & Database Integration: run predictive analysis twice without duplicate recs & manage lifecycle")
    void testLivePredictiveLifecycleAndDeduplicationDemoB() throws Exception {
        Assumptions.assumeTrue(DBConnection.testConnection(), "MySQL Server must be running");

        UserDAO userDAO = new UserDAO();
        User admin = userDAO.findByEmail("admin@campus.edu").orElseThrow();
        User student = userDAO.findByEmail("alice@student.campus.edu").orElseThrow();

        ComplaintService complaintService = new ComplaintService();
        List<Integer> createdComplaintIds = new ArrayList<>();
        List<Integer> createdAnalysisIds = new ArrayList<>();

        try {

            UserSession.login(student);
            for (int i = 1; i <= 4; i++) {
                Complaint c = new Complaint();
                c.setTitle("[DEMO-B] Electrical breaker & wiring failure #" + i);
                c.setDescription("Repeated electrical fault in [DEMO-B] lab wing.");
                c.setCategory("Electrical");
                c.setBuilding("Demo-B Engineering Hall");
                c.setRoomNumber("Lab B-101");
                c.setSeverity(4);
                c.setSafetyRisk(4);
                c.setPeopleAffected(40);
                c.setOperationalImpact(4);
                createdComplaintIds.add(complaintService.submitComplaint(c).getComplaintId());
            }

            Complaint cmp = new Complaint();
            cmp.setTitle("[DEMO-B] Minor bench repair");
            cmp.setDescription("Loose desk screw.");
            cmp.setCategory("Furniture and Infrastructure");
            cmp.setBuilding("Demo-B Humanities Annex");
            cmp.setRoomNumber("Room H-12");
            cmp.setSeverity(1);
            cmp.setSafetyRisk(0);
            cmp.setPeopleAffected(2);
            cmp.setOperationalImpact(1);
            createdComplaintIds.add(complaintService.submitComplaint(cmp).getComplaintId());

            UserSession.login(admin);
            PredictiveMaintenanceService.AnalysisExecutionResult run1 = service.runPredictiveAnalysis(30);
            createdAnalysisIds.add(run1.analysis().getId());

            PredictiveMaintenanceService.AnalysisExecutionResult run2 = service.runPredictiveAnalysis(30);
            createdAnalysisIds.add(run2.analysis().getId());

            List<PreventiveRecommendation> demoRecs = service.getPredictiveDashboardData(
                    "Demo-B Engineering Hall", "Electrical", "ALL", null, 6).recommendations();
            assertEquals(1, demoRecs.size(), "Repeated analysis runs must update existing active recommendation instead of duplicating");

            PreventiveRecommendation rec = demoRecs.get(0);

            service.updateRecommendationLifecycle(rec.getId(), RecommendationStatus.APPROVED, null);
            service.updateRecommendationLifecycle(rec.getId(), RecommendationStatus.SCHEDULED,
                    Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS)));
            service.updateRecommendationLifecycle(rec.getId(), RecommendationStatus.COMPLETED, null);

            List<PreventiveRecommendation> afterComplete = service.getPredictiveDashboardData(
                    "Demo-B Engineering Hall", "Electrical", "ALL", "COMPLETED", 6).recommendations();
            assertFalse(afterComplete.isEmpty());
            assertEquals(RecommendationStatus.COMPLETED, afterComplete.get(0).getStatus());
            assertNotNull(afterComplete.get(0).getCompletedAt());

        } finally {

            try (Connection conn = DBConnection.getConnection()) {
                for (Integer cid : createdComplaintIds) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit_logs WHERE entity_type = 'COMPLAINT' AND entity_id = ?")) {
                        ps.setInt(1, cid);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM complaints WHERE complaint_id = ?")) {
                        ps.setInt(1, cid);
                        ps.executeUpdate();
                    }
                }
                for (Integer aid : createdAnalysisIds) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit_logs WHERE entity_type IN ('PREDICTIVE_ANALYSIS','MAINTENANCE_RISK','PREVENTIVE_RECOMMENDATION')")) {
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM predictive_analysis WHERE id = ?")) {
                        ps.setInt(1, aid);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }
}
