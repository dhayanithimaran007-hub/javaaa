package com.smartcampus.model.enums;

import java.awt.Color;

public enum PriorityLevel {
    LOW("LOW", 0.0, 24.99, new Color(220, 235, 252), new Color(20, 80, 160)),
    MEDIUM("MEDIUM", 25.0, 49.99, new Color(254, 243, 199), new Color(146, 64, 14)),
    HIGH("HIGH", 50.0, 74.99, new Color(254, 226, 226), new Color(185, 28, 28)),
    CRITICAL("CRITICAL", 75.0, 100.0, new Color(239, 68, 68), Color.WHITE);

    private final String label;
    private final double minScore;
    private final double maxScore;
    private final Color badgeBackgroundColor;
    private final Color badgeTextColor;

    PriorityLevel(String label, double minScore, double maxScore, Color badgeBackgroundColor, Color badgeTextColor) {
        this.label = label;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.badgeBackgroundColor = badgeBackgroundColor;
        this.badgeTextColor = badgeTextColor;
    }

    public String getLabel() {
        return label;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public Color getBadgeBackgroundColor() {
        return badgeBackgroundColor;
    }

    public Color getBadgeTextColor() {
        return badgeTextColor;
    }

    public static PriorityLevel fromScore(double score) {
        if (score < 25.0) return LOW;
        if (score < 50.0) return MEDIUM;
        if (score < 75.0) return HIGH;
        return CRITICAL;
    }

    public static PriorityLevel fromString(String priorityStr) {
        if (priorityStr == null) return null;
        for (PriorityLevel p : values()) {
            if (p.name().equalsIgnoreCase(priorityStr.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown priority level: " + priorityStr);
    }
}
