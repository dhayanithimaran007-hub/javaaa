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

        PriorityService.PriorityResult res = priorityService.calculatePriority(1, 0, 1, 1, "Other");
        assertEquals(14.0, res.score(), 0.01);
        assertEquals(PriorityLevel.LOW, res.priorityLevel());
        assertFalse(res.isSafetyCritical());
    }

    @Test
    @DisplayName("Maximum possible inputs should yield CRITICAL priority score of 100.0")
    void testMaximumScoreCriticalPriority() {

        PriorityService.PriorityResult res = priorityService.calculatePriority(5, 5, 500, 5, "Electrical");
        assertEquals(100.0, res.score(), 0.01);
        assertEquals(PriorityLevel.CRITICAL, res.priorityLevel());
        assertTrue(res.isSafetyCritical());
    }

    @Test
    @DisplayName("MEDIUM priority classification test")
    void testMediumPriority() {

        PriorityService.PriorityResult res = priorityService.calculatePriority(2, 1, 10, 2, "Plumbing and Water Supply");
        assertEquals(34.0, res.score(), 0.01);
        assertEquals(PriorityLevel.MEDIUM, res.priorityLevel());
        assertFalse(res.isSafetyCritical());
    }

    @Test
    @DisplayName("HIGH priority classification test")
    void testHighPriority() {

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

    @Test
    @DisplayName("Intelligent Lighting Classification: classroom vs corridor/staircase vs emergency exit vs short circuit")
    void testIntelligentLightingAndElectricalClassification() {

        PriorityService.PriorityResult classroom = priorityService.calculatePriority(
                2, 1, 50, 3, "Electrical",
                "Not working of Lights",
                "Tubelight which was located near the C section table was not working.",
                "ANEW 202");
        assertEquals(41.0, classroom.score(), 0.01);
        assertEquals(PriorityLevel.MEDIUM, classroom.priorityLevel());
        assertFalse(classroom.isSafetyCritical(), "Routine classroom tubelight failure must NOT be falsely marked safety-critical");

        PriorityService.PriorityResult corridor = priorityService.calculatePriority(
                2, 1, 20, 2, "Electrical",
                "Corridor and Staircase Lights Out",
                "Lights on 2nd floor corridor staircase are not working, creating dark walkway",
                "2nd Floor Staircase");
        assertEquals(PriorityLevel.HIGH, corridor.priorityLevel());
        assertTrue(corridor.score() >= 50.0);

        PriorityService.PriorityResult exitLight = priorityService.calculatePriority(
                2, 1, 15, 2, "Electrical",
                "Emergency exit lighting failure",
                "Emergency exit sign light is dead near north stairwell",
                "North Exit");
        assertEquals(PriorityLevel.CRITICAL, exitLight.priorityLevel());
        assertTrue(exitLight.score() >= 75.0);
        assertTrue(exitLight.isSafetyCritical());

        PriorityService.PriorityResult shortCircuit = priorityService.calculatePriority(
                3, 2, 10, 2, "Electrical",
                "Electrical short circuit in DB panel",
                "Sparking wires and burning smell near corridor switchboard",
                "Lab 105");
        assertEquals(PriorityLevel.CRITICAL, shortCircuit.priorityLevel());
        assertTrue(shortCircuit.score() >= 80.0);
        assertTrue(shortCircuit.isSafetyCritical());
    }
}
