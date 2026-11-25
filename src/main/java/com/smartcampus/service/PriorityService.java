package com.smartcampus.service;

import com.smartcampus.config.AppConfig;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriorityService {

    public static final double WEIGHT_SEVERITY = 0.35;
    public static final double WEIGHT_SAFETY = 0.30;
    public static final double WEIGHT_AFFECTED = 0.20;
    public static final double WEIGHT_IMPACT = 0.15;
    public static final double SCALE_MULTIPLIER = 20.0;

    public record PriorityResult(
            double score,
            PriorityLevel priorityLevel,
            boolean isSafetyCritical,
            int normalizedAffectedScore,
            String breakdown
    ) {}

    public record FactorBreakdown(
            double severityPoints,
            double safetyPoints,
            double affectedPoints,
            double impactPoints,
            double totalScore,
            String recommendedAction,
            int slaHours,
            String primaryDriver
    ) {}

    public int normalizePeopleAffected(int people) {
        if (people <= 0) {
            throw new IllegalArgumentException("People affected must be greater than zero.");
        }
        if (people <= 5) return 1;
        if (people <= 20) return 2;
        if (people <= 50) return 3;
        if (people <= 200) return 4;
        return 5;
    }

    public PriorityResult calculatePriority(int severity, int safetyRisk, int peopleAffected,
                                            int operationalImpact, String category) {
        return calculatePriority(severity, safetyRisk, peopleAffected, operationalImpact, category, null, null, null);
    }

    public PriorityResult calculatePriority(int severity, int safetyRisk, int peopleAffected,
                                            int operationalImpact, String category,
                                            String title, String description, String roomNumber) {

        ValidationUtil.validateRange(severity, 1, 5, "Severity");
        ValidationUtil.validateRange(safetyRisk, 0, 5, "Safety Risk");
        ValidationUtil.validatePositive(peopleAffected, "People Affected");
        ValidationUtil.validateRange(operationalImpact, 1, 5, "Operational Impact");

        int a = normalizePeopleAffected(peopleAffected);

        double rawScore = SCALE_MULTIPLIER * (
                (WEIGHT_SEVERITY * severity) +
                (WEIGHT_SAFETY * safetyRisk) +
                (WEIGHT_AFFECTED * a) +
                (WEIGHT_IMPACT * operationalImpact)
        );

        double clampedScore = Math.max(0.0, Math.min(100.0, rawScore));
        double score = BigDecimal.valueOf(clampedScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        ContextHazardAssessment ctx = assessContextHazard(category, title, description, roomNumber);
        if (ctx.minScoreFloor() > score) {
            score = ctx.minScoreFloor();
        }

        PriorityLevel priorityLevel = PriorityLevel.fromScore(score);

        boolean isSafetyCritical = ctx.forceSafetyCritical()
                || isSafetyCriticalCheck(severity, safetyRisk, category, priorityLevel);

        String contextSuffix = ctx.reason() != null ? " [" + ctx.reason() + "]" : "";
        String breakdown = String.format(
                "S=%d (x%.2f), R=%d (x%.2f), A=%d (from %d people, x%.2f), I=%d (x%.2f) => Score: %.2f [%s]%s%s",
                severity, WEIGHT_SEVERITY, safetyRisk, WEIGHT_SAFETY,
                a, peopleAffected, WEIGHT_AFFECTED,
                operationalImpact, WEIGHT_IMPACT, score, priorityLevel.name(),
                isSafetyCritical ? " [SAFETY OVERRIDE]" : "",
                contextSuffix
        );

        return new PriorityResult(score, priorityLevel, isSafetyCritical, a, breakdown);
    }

    public record ContextHazardAssessment(double minScoreFloor, boolean forceSafetyCritical, String reason) {}

    public ContextHazardAssessment assessContextHazard(String category, String title, String description, String roomNumber) {
        String corpus = ((title != null ? title : "") + " " +
                (description != null ? description : "") + " " +
                (roomNumber != null ? roomNumber : "")).toLowerCase();

        if (corpus.isBlank()) {
            return new ContextHazardAssessment(0.0, false, null);
        }

        boolean mentionsExitLight = corpus.contains("emergency exit") || corpus.contains("exit light")
                || corpus.contains("exit lighting") || corpus.contains("exit sign")
                || corpus.contains("fire exit") || corpus.contains("evacuation light");
        if (mentionsExitLight) {
            return new ContextHazardAssessment(75.0, true, "Emergency Exit Lighting Failure");
        }

        boolean mentionsShortCircuit = corpus.contains("short circuit") || corpus.contains("short-circuit")
                || corpus.contains("sparking") || corpus.contains("sparks") || corpus.contains("arcing")
                || corpus.contains("exposed wire") || corpus.contains("exposed wiring")
                || corpus.contains("live wire") || corpus.contains("burning smell")
                || corpus.contains("electrical fire") || corpus.contains("electrocution");
        if (mentionsShortCircuit) {
            return new ContextHazardAssessment(80.0, true, "Critical Electrical Short-Circuit Hazard");
        }

        boolean mentionsLighting = corpus.contains("light") || corpus.contains("lamp")
                || corpus.contains("bulb") || corpus.contains("tubelight") || corpus.contains("tube light")
                || corpus.contains("dark") || corpus.contains("blackout")
                || (category != null && category.toLowerCase().contains("electric"));

        boolean mentionsCorridorOrStairs = corpus.contains("staircase") || corpus.contains("stairwell")
                || corpus.contains("stairs") || corpus.contains("corridor")
                || corpus.contains("hallway") || corpus.contains("walkway")
                || corpus.contains("passage") || corpus.contains("ramp");

        if (mentionsLighting && mentionsCorridorOrStairs) {
            return new ContextHazardAssessment(50.0, false, "Dark Staircase/Corridor Egress Hazard");
        }

        return new ContextHazardAssessment(0.0, false, null);
    }

    public FactorBreakdown getDetailedFactorBreakdown(int severity, int safetyRisk, int peopleAffected,
                                                     int operationalImpact, String category) {
        int a = normalizePeopleAffected(peopleAffected);

        double sevPts = BigDecimal.valueOf(SCALE_MULTIPLIER * WEIGHT_SEVERITY * severity)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        double safPts = BigDecimal.valueOf(SCALE_MULTIPLIER * WEIGHT_SAFETY * safetyRisk)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        double affPts = BigDecimal.valueOf(SCALE_MULTIPLIER * WEIGHT_AFFECTED * a)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        double impPts = BigDecimal.valueOf(SCALE_MULTIPLIER * WEIGHT_IMPACT * operationalImpact)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        double total = BigDecimal.valueOf(sevPts + safPts + affPts + impPts)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        PriorityLevel level = PriorityLevel.fromScore(total);
        boolean isSafety = isSafetyCriticalCheck(severity, safetyRisk, category, level);

        String action = getRecommendedAction(level, category, isSafety);
        int sla = getSlaHours(level);

        String driver = "General Facilities Impact";
        double maxPts = Math.max(sevPts, Math.max(safPts, Math.max(affPts, impPts)));
        if (maxPts == safPts && safetyRisk > 0) driver = "Life Safety & Physical Hazard (" + safPts + " pts)";
        else if (maxPts == sevPts) driver = "High Functional Severity (" + sevPts + " pts)";
        else if (maxPts == affPts) driver = "Broad Campus Population Affected (" + affPts + " pts)";
        else if (maxPts == impPts) driver = "Critical Academic Disruption (" + impPts + " pts)";

        return new FactorBreakdown(sevPts, safPts, affPts, impPts, total, action, sla, driver);
    }

    public boolean isSafetyCriticalCheck(int severity, int safetyRisk, String category, PriorityLevel level) {

        if (safetyRisk >= 4) return true;

        if (severity == 5 && safetyRisk >= 2) return true;

        if (level == PriorityLevel.CRITICAL) return true;

        if (category != null && category.equalsIgnoreCase("Safety and Security")) {
            if (safetyRisk >= 3 || severity >= 4) return true;
        }

        return false;
    }

    public int getSlaHours(PriorityLevel level) {
        if (level == null) return AppConfig.getIntProperty("sla.medium.hours", 72);
        return switch (level) {
            case CRITICAL -> AppConfig.getIntProperty("sla.critical.hours", 4);
            case HIGH -> AppConfig.getIntProperty("sla.high.hours", 24);
            case MEDIUM -> AppConfig.getIntProperty("sla.medium.hours", 72);
            case LOW -> AppConfig.getIntProperty("sla.low.hours", 168);
        };
    }

    public String getRecommendedAction(PriorityLevel level, String category, boolean isSafetyCritical) {
        int sla = getSlaHours(level);
        if (isSafetyCritical || level == PriorityLevel.CRITICAL) {
            if (category != null && category.toLowerCase().contains("electrical")) {
                return "CRITICAL DISPATCH: Immediately isolate power supply; dispatch licensed electrician within 2 hours (SLA: " + sla + "h). Cordon off area.";
            } else if (category != null && category.toLowerCase().contains("water")) {
                return "CRITICAL DISPATCH: Shut off main water line valve immediately. Dispatch plumbing emergency crew (SLA: " + sla + "h) to prevent flooding.";
            } else {
                return "EMERGENCY PROTOCOL: Immediate human review & dispatch required within " + sla + " hours. Escalate to Facilities Supervisor.";
            }
        }

        if (level == PriorityLevel.HIGH) {
            if (category != null && (category.toLowerCase().contains("wi-fi") || category.toLowerCase().contains("internet"))) {
                return "HIGH PRIORITY: Dispatch network operations technician within 6 hours (SLA: " + sla + "h). Check central switchgear & AP firmware.";
            }
            return "URGENT DISPATCH: Assign to senior technician for resolution within " + sla + " hours. Priority schedule.";
        }

        if (level == PriorityLevel.MEDIUM) {
            return "STANDARD DISPATCH: Queue for facilities team assignment. Target resolution within standard " + sla + "-hour SLA window.";
        }

        return "ROUTINE MAINTENANCE: Schedule during normal preventive maintenance cycle. Target turnaround within " + sla + " hours.";
    }
}
