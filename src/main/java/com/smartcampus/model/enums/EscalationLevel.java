package com.smartcampus.model.enums;

import java.awt.Color;

public enum EscalationLevel {
    NORMAL(0, "LEVEL 0 - NORMAL", new Color(241, 245, 249), new Color(71, 85, 105)),
    EARLY_WARNING(1, "LEVEL 1 - WARNING", new Color(254, 243, 199), new Color(180, 83, 9)),
    SLA_BREACH(2, "LEVEL 2 - BREACH", new Color(255, 228, 230), new Color(190, 18, 60)),
    ADMIN_ESCALATION(3, "LEVEL 3 - CRITICAL ESC", new Color(190, 18, 60), Color.WHITE);

    private final int level;
    private final String displayName;
    private final Color badgeBg;
    private final Color badgeText;

    EscalationLevel(int level, String displayName, Color badgeBg, Color badgeText) {
        this.level = level;
        this.displayName = displayName;
        this.badgeBg = badgeBg;
        this.badgeText = badgeText;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    public Color getBadgeBg() { return badgeBg; }
    public Color getBadgeText() { return badgeText; }

    public static EscalationLevel fromLevel(int lvl) {
        for (EscalationLevel e : values()) {
            if (e.level == lvl) return e;
        }
        return lvl > 3 ? ADMIN_ESCALATION : NORMAL;
    }

    public static EscalationLevel fromString(String value) {
        if (value == null) return NORMAL;
        for (EscalationLevel e : values()) {
            if (e.name().equalsIgnoreCase(value.trim()) || e.displayName.equalsIgnoreCase(value.trim())) {
                return e;
            }
        }
        return NORMAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
