package com.smartcampus.model;

public record StaffRecommendationDTO(
        User staff,
        int activeWorkload,
        boolean specializationMatch,
        double matchScore,
        String reason
) implements Comparable<StaffRecommendationDTO> {
    @Override
    public int compareTo(StaffRecommendationDTO o) {

        return Double.compare(o.matchScore, this.matchScore);
    }
}
