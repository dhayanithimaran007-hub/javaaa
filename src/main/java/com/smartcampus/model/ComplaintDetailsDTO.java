package com.smartcampus.model;

import java.util.List;
import java.util.Optional;

public record ComplaintDetailsDTO(
        Complaint complaint,
        List<ComplaintStatusHistory> historyList,
        Optional<ComplaintAssignment> activeAssignment,
        Optional<ComplaintFeedback> feedback
) {}
