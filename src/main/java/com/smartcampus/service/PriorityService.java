package com.smartcampus.service;

import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic Priority Scoring Engine.
 * 
 * Formula:
 *   P = 20 * (0.35 * S + 0.30 * R + 0.20 * A + 0.15 * I)
 * 
 * Where:
 *   - S = Severity on scale 1 to 5
 *   - R = Safety risk on scale 0 to 5 (0 denotes no immediate physical hazard)
 *   - A = Affected population normalized to scale 1 to 5
 *   - I = Operational impact on scale 1 to 5
 * 
 * Thresholds:
 *   - 0.0 to 24.99: LOW
 *   - 25.0 to 49.99: MEDIUM
 *   - 50.0 to 74.99: HIGH
 *   - 75.0 to 100.0: CRITICAL
 * 
 * Note: This is an explicit, deterministic rule-based prioritization engine,
 * fully auditable and distinct from opaque machine-learning models.
 */
public class PriorityService {
    // Component weights
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

    /**
     * Normalizes raw people count into a documented 1–5 scale:
     * - 1 to 5 people -> 1 (Individual / small team)
     * - 6 to 20 people -> 2 (Room / small group)
     * - 21 to 50 people -> 3 (Classroom / lab)
     * - 51 to 200 people -> 4 (Floor / major wing / lecture hall)
     * - > 200 people -> 5 (Entire building / campus-wide)
     */
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

    /**
     * Calculates deterministic priority score, classification, and safety flags.
     */
    public PriorityResult calculatePriority(int severity, int safetyRisk, int peopleAffected,
                                            int operationalImpact, String category) {
        // 1. Validate inputs
        ValidationUtil.validateRange(severity, 1, 5, "Severity");
        ValidationUtil.validateRange(safetyRisk, 0, 5, "Safety Risk");
        ValidationUtil.validatePositive(peopleAffected, "People Affected");
        ValidationUtil.validateRange(operationalImpact, 1, 5, "Operational Impact");

        // 2. Normalize affected population
        int a = normalizePeopleAffected(peopleAffected);

        // 3. Compute weighted formula: P = 20 * (0.35*S + 0.30*R + 0.20*A + 0.15*I)
        double rawScore = SCALE_MULTIPLIER * (
                (WEIGHT_SEVERITY * severity) +
                (WEIGHT_SAFETY * safetyRisk) +
                (WEIGHT_AFFECTED * a) +
                (WEIGHT_IMPACT * operationalImpact)
        );

        // Clamp between 0.0 and 100.0 and round to 2 decimal places
        double clampedScore = Math.max(0.0, Math.min(100.0, rawScore));
        double score = BigDecimal.valueOf(clampedScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        // 4. Determine priority level from score
        PriorityLevel priorityLevel = PriorityLevel.fromScore(score);

        // 5. Check safety-critical conditions for immediate human review override
        boolean isSafetyCritical = isSafetyCriticalCheck(severity, safetyRisk, category, priorityLevel);

        // 6. Format breakdown explanation
        String breakdown = String.format(
                "S=%d (x%.2f), R=%d (x%.2f), A=%d (from %d people, x%.2f), I=%d (x%.2f) => Score: %.2f [%s]%s",
                severity, WEIGHT_SEVERITY, safetyRisk, WEIGHT_SAFETY,
                a, peopleAffected, WEIGHT_AFFECTED,
                operationalImpact, WEIGHT_IMPACT, score, priorityLevel.name(),
                isSafetyCritical ? " [SAFETY OVERRIDE]" : ""
        );

        return new PriorityResult(score, priorityLevel, isSafetyCritical, a, breakdown);
    }

    /**
     * Determines whether the complaint presents an immediate safety risk requiring
     * human intervention regardless of other numerical scoring factors.
     */
    public boolean isSafetyCriticalCheck(int severity, int safetyRisk, String category, PriorityLevel level) {
        // Explicit safety risk 4 or 5 is always safety-critical
        if (safetyRisk >= 4) return true;

        // High severity combined with moderate safety hazard
        if (severity == 5 && safetyRisk >= 2) return true;

        // Any complaint classified as CRITICAL level
        if (level == PriorityLevel.CRITICAL) return true;

        // Safety and Security category with significant risk or severity
        if (category != null && category.equalsIgnoreCase("Safety and Security")) {
            if (safetyRisk >= 3 || severity >= 4) return true;
        }

        return false;
    }
}
