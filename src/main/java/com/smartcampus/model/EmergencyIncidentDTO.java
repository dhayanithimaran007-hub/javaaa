package com.smartcampus.model;

import com.smartcampus.model.enums.EscalationLevel;

import java.sql.Timestamp;

public record EmergencyIncidentDTO(
        Complaint complaint,
        SlaSnapshot slaSnapshot,
        EscalationLevel currentEscalationLevel,
        String escalationReason,
        boolean acknowledged,
        Timestamp acknowledgedAt,
        String acknowledgedByName,
        String interventionNote,
        String safetyReason
) {}
