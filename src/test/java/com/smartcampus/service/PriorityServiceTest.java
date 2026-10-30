package com.smartcampus.service;

import com.smartcampus.model.enums.PriorityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PriorityService Unit & Boundary Tests")
class PriorityServiceTest {
    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    @DisplayName("Minimum possible inputs should yield LOW priority score of 14.0")
    void testMinimumScoreLowPriority() {
        // S=1, R=0, A=1 (from 1 person), I=1
        // P = 20 * (0.35*1 + 0.30*0 + 0.20*1 + 0.15*1) = 20 * 0.70 = 14.0
        PriorityService.PriorityResult res = priorityService.calculatePriority(1, 0, 1, 1, "Other");
        assertEquals(14.0, res.score(), 0.01);
        assertEquals(PriorityLevel.LOW, res.priorityLevel());
        assertFalse(res.isSafetyCritical());
    }

    @Test
    @DisplayName("Maximum possible inputs should yield CRITICAL priority score of 100.0")
    void testMaximumScoreCriticalPriority() {
        // S=5, R=5, A=5 (from 500 people), I=5
        // P = 20 * (0.35*5 + 0.30*5 + 0.20*5 + 0.15*5) = 20 * 5.0 = 100.0
        PriorityService.PriorityResult res = priorityService.calculatePriority(5, 5, 500, 5, "Electrical");
        assertEquals(100.0, res.score(), 0.01);
        assertEquals(PriorityLevel.CRITICAL, res.priorityLevel());
        assertTrue(res.isSafetyCritical());
    }

    @Test
    @DisplayName("MEDIUM priority classification test")
    void testMediumPriority() {
        // S=2, R=1, A=2 (from 10 people), I=2
        // P = 20 * (0.35*2 + 0.30*1 + 0.20*2 + 0.15*2) = 20 * (0.70 + 0.30 + 0.40 + 0.30) = 20 * 1.70 = 34.0
        PriorityService.PriorityResult res = priorityService.calculatePriority(2, 1, 10, 2, "Plumbing and Water Supply");
        assertEquals(34.0, res.score(), 0.01);
        assertEquals(PriorityLevel.MEDIUM, res.priorityLevel());
        assertFalse(res.isSafetyCritical());
    }

    @Test
    @DisplayName("HIGH priority classification test")
    void testHighPriority() {
        // S=4, R=2, A=4 (from 100 people), I=3
        // P = 20 * (0.35*4 + 0.30*2 + 0.20*4 + 0.15*3) = 20 * (1.40 + 0.60 + 0.80 + 0.45) = 20 * 3.25 = 65.0
        PriorityService.PriorityResult res = priorityService.calculatePriority(4, 2, 100, 3, "Internet and Wi-Fi");
        assertEquals(65.0, res.score(), 0.01);
        assertEquals(PriorityLevel.HIGH, res.priorityLevel());
        assertFalse(res.isSafetyCritical());
    }

    @ParameterizedTest(name = "People {0} should normalize to scale {1}")
    @CsvSource({
            "1, 1",
            "5, 1",
            "6, 2",
            "20, 2",
            "21, 3",
            "50, 3",
            "51, 4",
            "200, 4",
            "201, 5",
            "1000, 5"
    })
    @DisplayName("Affected population scale normalization")
    void testNormalizePeopleAffected(int people, int expectedScale) {
        assertEquals(expectedScale, priorityService.normalizePeopleAffected(people));
    }

    @Test
    @DisplayName("Safety risk override: Safety risk >= 4 must flag safety critical even if isolated to 1 person")
    void testSafetyRiskOverrideHighRisk() {
        // Low people count, low operational impact, but safety risk is 4
        PriorityService.PriorityResult res = priorityService.calculatePriority(2, 4, 1, 1, "Electrical");
        assertTrue(res.isSafetyCritical(), "Safety risk >= 4 must be flagged safety critical");
    }

    @Test
    @DisplayName("Safety risk override: Severity 5 with safety risk >= 2 must be flagged")
    void testSafetyOverrideSeverity5WithRisk() {
        PriorityService.PriorityResult res = priorityService.calculatePriority(5, 2, 2, 1, "Classroom Equipment");
        assertTrue(res.isSafetyCritical());
    }

    @Test
    @DisplayName("Category 'Safety and Security' with safety risk >= 3 must be flagged")
    void testSafetyCategoryOverride() {
        PriorityService.PriorityResult res = priorityService.calculatePriority(2, 3, 5, 2, "Safety and Security");
        assertTrue(res.isSafetyCritical());
    }

    @ParameterizedTest(name = "Severity {0} should be invalid")
    @ValueSource(ints = {-1, 0, 6, 10})
    void testInvalidSeverity(int severity) {
        assertThrows(IllegalArgumentException.class, () ->
                priorityService.calculatePriority(severity, 2, 10, 2, "Other"));
    }

    @ParameterizedTest(name = "Safety risk {0} should be invalid")
    @ValueSource(ints = {-2, -1, 6, 9})
    void testInvalidSafetyRisk(int safety) {
        assertThrows(IllegalArgumentException.class, () ->
                priorityService.calculatePriority(3, safety, 10, 2, "Other"));
    }

    @ParameterizedTest(name = "People affected {0} should be invalid")
    @ValueSource(ints = {-10, -1, 0})
    void testInvalidPeopleAffected(int people) {
        assertThrows(IllegalArgumentException.class, () ->
                priorityService.calculatePriority(3, 1, people, 2, "Other"));
    }

    @ParameterizedTest(name = "Operational impact {0} should be invalid")
    @ValueSource(ints = {-1, 0, 6, 12})
    void testInvalidOperationalImpact(int impact) {
        assertThrows(IllegalArgumentException.class, () ->
                priorityService.calculatePriority(3, 1, 10, impact, "Other"));
    }
}
