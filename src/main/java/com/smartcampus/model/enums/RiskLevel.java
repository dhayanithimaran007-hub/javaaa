package com.smartcampus.model.enums;

import java.awt.Color;

public enum RiskLevel {
    LOW("LOW", new Color(220, 252, 231), new Color(21, 128, 61)),
    MODERATE("MODERATE", new Color(254, 243, 199), new Color(180, 83, 9)),
    HIGH("HIGH", new Color(255, 237, 213), new Color(194, 65, 12)),
    CRITICAL("CRITICAL", new Color(255, 228, 230), new Color(190, 18, 60));

    private final String label;
    private final Color badgeBg;
    private final Color badgeText;

    RiskLevel(String label, Color badgeBg, Color badgeText) {
        this.label = label;
        this.badgeBg = badgeBg;
        this.badgeText = badgeText;
    }

    public String getLabel() { return label; }
    public Color getBadgeBg() { return badgeBg; }
    public Color getBadgeText() { return badgeText; }

    public static RiskLevel fromScore(double score) {
        if (score >= 75.0) return CRITICAL;
        if (score >= 50.0) return HIGH;
        if (score >= 25.0) return MODERATE;
        return LOW;
    }

    public static RiskLevel fromString(String value) {
        if (value == null) return LOW;
        for (RiskLevel r : values()) {
            if (r.name().equalsIgnoreCase(value.trim()) || r.label.equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return LOW;
    }
}
