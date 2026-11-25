package com.smartcampus.model;

public record StaffPerformanceDTO(
        int staffId,
        String fullName,
        String department,
        int assignedCount,
        int resolvedCount,
        double avgResolutionHours,
        double avgRating,
        int activeTasks
) {
    public double getResolutionRate() {
        return assignedCount > 0 ? (double) resolvedCount / assignedCount * 100.0 : 0.0;
    }
}
