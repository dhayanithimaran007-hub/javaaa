package com.smartcampus.model.enums;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ComplaintStatus {
    SUBMITTED("Submitted", new Color(224, 231, 255), new Color(55, 48, 163)),
    ACKNOWLEDGED("Acknowledged", new Color(237, 233, 254), new Color(109, 40, 217)),
    IN_PROGRESS("In Progress", new Color(254, 243, 199), new Color(180, 83, 9)),
    RESOLVED("Resolved", new Color(220, 252, 231), new Color(21, 128, 61)),
    REJECTED("Rejected", new Color(254, 226, 226), new Color(185, 28, 28));

    private final String displayName;
    private final Color badgeBackgroundColor;
    private final Color badgeTextColor;

    ComplaintStatus(String displayName, Color badgeBackgroundColor, Color badgeTextColor) {
        this.displayName = displayName;
        this.badgeBackgroundColor = badgeBackgroundColor;
        this.badgeTextColor = badgeTextColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getBadgeBackgroundColor() {
        return badgeBackgroundColor;
    }

    public Color getBadgeTextColor() {
        return badgeTextColor;
    }

    public Set<ComplaintStatus> getAllowedNextStatuses() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(ACKNOWLEDGED, REJECTED);
            case ACKNOWLEDGED -> EnumSet.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> EnumSet.of(RESOLVED, REJECTED);
            case RESOLVED -> EnumSet.of(IN_PROGRESS); // Allowed to reopen
            case REJECTED -> EnumSet.of(SUBMITTED);   // Allowed to resubmit / reopen
        };
    }

    public boolean canTransitionTo(ComplaintStatus target) {
        if (target == null || target == this) return false;
        return getAllowedNextStatuses().contains(target);
    }

    public static ComplaintStatus fromString(String statusStr) {
        if (statusStr == null) return null;
        for (ComplaintStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim()) || s.displayName.equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown complaint status: " + statusStr);
    }
}
