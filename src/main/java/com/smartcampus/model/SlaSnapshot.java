package com.smartcampus.model;

import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.SlaStatus;

import java.sql.Timestamp;

public record SlaSnapshot(
        int complaintId,
        int slaTargetHours,
        Timestamp slaStart,
        Timestamp slaDeadline,
        long elapsedMinutes,
        long remainingMinutes,
        long breachMinutes,
        double elapsedPercent,
        boolean earlyWarning50Triggered,
        SlaStatus slaStatus,
        EscalationLevel targetEscalationLevel
) {
    public String formatCountdownDisplay() {
        if (slaStatus == SlaStatus.RESOLVED) {
            return "Closed";
        }
        if (breachMinutes > 0) {
            return "OVERDUE " + formatDuration(breachMinutes);
        }
        return formatDuration(remainingMinutes) + " left";
    }

    public String formatElapsedDisplay() {
        return formatDuration(elapsedMinutes);
    }

    public String formatBreachDisplay() {
        return breachMinutes > 0 ? formatDuration(breachMinutes) : "None";
    }

    public static String formatDuration(long totalMinutes) {
        long mins = Math.max(0, totalMinutes);
        long days = mins / (24 * 60);
        long hours = (mins % (24 * 60)) / 60;
        long remMins = mins % 60;
        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, remMins);
        }
        return String.format("%02dh %02dm", hours, remMins);
    }
}
