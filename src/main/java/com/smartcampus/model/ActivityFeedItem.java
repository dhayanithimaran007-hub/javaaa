package com.smartcampus.model;

import java.sql.Timestamp;

public record ActivityFeedItem(
        int auditId,
        String actorName,
        String actorRole,
        String action,
        String entityType,
        Integer entityId,
        String details,
        Timestamp createdAt,
        String relativeTime,
        boolean isCritical
) {}
