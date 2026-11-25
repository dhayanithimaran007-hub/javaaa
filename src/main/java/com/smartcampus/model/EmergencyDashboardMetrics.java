package com.smartcampus.model;

public record EmergencyDashboardMetrics(
        int activeCriticalIncidents,
        int approachingDeadlines,
        int breachedSlaCount,
        int escalatedComplaintsCount,
        double avgEmergencyResponseHours,
        double emergencyResolutionRate
) {}
