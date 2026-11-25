package com.smartcampus.model.enums;

import java.awt.Color;

public enum RecommendationStatus {
    NEW("NEW", new Color(219, 234, 254), new Color(29, 78, 216)),
    UNDER_REVIEW("UNDER REVIEW", new Color(243, 232, 255), new Color(126, 34, 206)),
    APPROVED("APPROVED", new Color(224, 242, 254), new Color(3, 105, 161)),
    SCHEDULED("SCHEDULED", new Color(254, 243, 199), new Color(180, 83, 9)),
    COMPLETED("COMPLETED", new Color(220, 252, 231), new Color(21, 128, 61)),
    DISMISSED("DISMISSED", new Color(241, 245, 249), new Color(100, 116, 139));

    private final String displayName;
    private final Color badgeBg;
    private final Color badgeText;

    RecommendationStatus(String displayName, Color badgeBg, Color badgeText) {
        this.displayName = displayName;
        this.badgeBg = badgeBg;
        this.badgeText = badgeText;
    }

    public String getDisplayName() { return displayName; }
    public Color getBadgeBg() { return badgeBg; }
    public Color getBadgeText() { return badgeText; }

    public static RecommendationStatus fromString(String value) {
        if (value == null) return NEW;
        for (RecommendationStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim()) || s.displayName.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return NEW;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
