package com.smartcampus.model;

public record PredictiveDashboardMetrics(
        int buildingsAnalyzed,
        int recurringFailureLocations,
        int highRiskBuildings,
        int criticalMaintenanceRisks,
        int preventiveActionsPending,
        int recommendationsCompleted
) {}
