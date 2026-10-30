package com.smartcampus.model.enums;

public enum ComplaintCategory {
    ELECTRICAL("Electrical"),
    PLUMBING_WATER("Plumbing and Water Supply"),
    CLEANLINESS_SANITATION("Cleanliness and Sanitation"),
    INTERNET_WIFI("Internet and Wi-Fi"),
    CLASSROOM_EQUIPMENT("Classroom Equipment"),
    LABORATORY_EQUIPMENT("Laboratory Equipment"),
    FURNITURE_INFRASTRUCTURE("Furniture and Infrastructure"),
    SAFETY_SECURITY("Safety and Security"),
    OTHER("Other");

    private final String displayName;

    ComplaintCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ComplaintCategory fromDisplayName(String name) {
        if (name == null) return null;
        for (ComplaintCategory cat : values()) {
            if (cat.displayName.equalsIgnoreCase(name.trim()) || cat.name().equalsIgnoreCase(name.trim())) {
                return cat;
            }
        }
        return OTHER;
    }
}
