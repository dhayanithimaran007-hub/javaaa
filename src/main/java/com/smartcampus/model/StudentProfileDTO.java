package com.smartcampus.model;

import java.util.List;

public record StudentProfileDTO(
        User student,
        int totalComplaints,
        int resolvedComplaints,
        int inProgressComplaints,
        double averageRatingGiven,
        List<Complaint> complaints
) {}
