package com.smartcampus.model;

import com.smartcampus.model.enums.ComplaintStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComplaintStatus Lifecycle Transition Tests")
class ComplaintStatusTransitionTest {

    @Test
    @DisplayName("SUBMITTED can transition to ACKNOWLEDGED or REJECTED")
    void testSubmittedTransitions() {
        assertTrue(ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.ACKNOWLEDGED));
        assertTrue(ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.REJECTED));
        assertFalse(ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.RESOLVED));
        assertFalse(ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("ACKNOWLEDGED can transition to IN_PROGRESS or REJECTED")
    void testAcknowledgedTransitions() {
        assertTrue(ComplaintStatus.ACKNOWLEDGED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertTrue(ComplaintStatus.ACKNOWLEDGED.canTransitionTo(ComplaintStatus.REJECTED));
        assertFalse(ComplaintStatus.ACKNOWLEDGED.canTransitionTo(ComplaintStatus.RESOLVED));
        assertFalse(ComplaintStatus.ACKNOWLEDGED.canTransitionTo(ComplaintStatus.SUBMITTED));
    }

    @Test
    @DisplayName("IN_PROGRESS can transition to RESOLVED or REJECTED")
    void testInProgressTransitions() {
        assertTrue(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.RESOLVED));
        assertTrue(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.REJECTED));
        assertFalse(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.ACKNOWLEDGED));
        assertFalse(ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.SUBMITTED));
    }

    @Test
    @DisplayName("RESOLVED can only transition to IN_PROGRESS via reopen")
    void testResolvedTransitions() {
        assertTrue(ComplaintStatus.RESOLVED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
        assertFalse(ComplaintStatus.RESOLVED.canTransitionTo(ComplaintStatus.SUBMITTED));
        assertFalse(ComplaintStatus.RESOLVED.canTransitionTo(ComplaintStatus.REJECTED));
    }

    @Test
    @DisplayName("Cannot transition to null or self")
    void testSelfAndNullTransitions() {
        for (ComplaintStatus status : ComplaintStatus.values()) {
            assertFalse(status.canTransitionTo(null));
            assertFalse(status.canTransitionTo(status));
        }
    }
}
