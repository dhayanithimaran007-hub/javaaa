package com.smartcampus.model.enums;

import java.awt.Color;

public enum SlaStatus {
    ON_TRACK("ON TRACK", new Color(220, 252, 231), new Color(21, 128, 61)),
    APPROACHING_DEADLINE("APPROACHING", new Color(254, 243, 199), new Color(180, 83, 9)),
    BREACHED("BREACHED", new Color(255, 228, 230), new Color(190, 18, 60)),
    RESOLVED("RESOLVED", new Color(226, 232, 240), new Color(71, 85, 105));

    private final String displayName;
    private final Color badgeBg;
    private final Color badgeText;

    SlaStatus(String displayName, Color badgeBg, Color badgeText) {
        this.displayName = displayName;
        this.badgeBg = badgeBg;
        this.badgeText = badgeText;
    }

    public String getDisplayName() { return displayName; }
    public Color getBadgeBg() { return badgeBg; }
    public Color getBadgeText() { return badgeText; }

    public static SlaStatus fromString(String value) {
        if (value == null) return ON_TRACK;
        for (SlaStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim()) || s.displayName.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return ON_TRACK;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
