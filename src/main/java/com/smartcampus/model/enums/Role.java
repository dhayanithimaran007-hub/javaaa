package com.smartcampus.model.enums;

public enum Role {
    STUDENT("Student"),
    ADMIN("Administrator"),
    STAFF("Maintenance Staff");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) return null;
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(roleStr.trim()) || r.displayName.equalsIgnoreCase(roleStr.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleStr);
    }
}
