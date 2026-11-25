package com.smartcampus.service;

import com.smartcampus.config.AppConfig;
import com.smartcampus.config.DBConnection;
import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.dao.ComplaintDAO;
import com.smartcampus.dao.PredictiveMaintenanceDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.MaintenanceRiskAssessment;
import com.smartcampus.model.PredictiveAnalysis;
import com.smartcampus.model.PredictiveDashboardMetrics;
import com.smartcampus.model.PreventiveRecommendation;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.RiskLevel;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PredictiveMaintenanceService {

    private static final List<String> ASSET_KEYWORDS = List.of(
            "wiring", "breaker", "panel", "fan", "light", "socket", "generator",
            "pipe", "valve", "sink", "tap", "drain", "tank", "pump",
            "router", "switch", "wifi", "wi-fi", "access point", "cable",
            "projector", "ac", "hvac", "chiller", "door", "window", "roof", "wall"
    );

    public record AnalysisExecutionResult(
            PredictiveAnalysis analysis,
            List<MaintenanceRiskAssessment> assessments,
            List<PreventiveRecommendation> recommendations
    ) {}

    public record PredictiveDashboardData(
            PredictiveDashboardMetrics metrics,
            Optional<PredictiveAnalysis> latestAnalysis,
            List<MaintenanceRiskAssessment> assessments,
            List<PreventiveRecommendation> recommendations,
            Map<String, Integer> complaintsByBuilding,
            Map<String, Integer> recurringFailuresByCategory,
            Map<String, Integer> monthlyTrends
    ) {}

    private final ComplaintDAO complaintDAO;
    private final PredictiveMaintenanceDAO predictiveDAO;
    private final AuditLogDAO auditLogDAO;

    public PredictiveMaintenanceService() {
        this(new ComplaintDAO(), new PredictiveMaintenanceDAO(), new AuditLogDAO());
    }

    public PredictiveMaintenanceService(ComplaintDAO complaintDAO, PredictiveMaintenanceDAO predictiveDAO, AuditLogDAO auditLogDAO) {
        this.complaintDAO = complaintDAO;
        this.predictiveDAO = predictiveDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public AnalysisExecutionResult runPredictiveAnalysis(int analysisPeriodDays) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User admin = UserSession.getCurrentUser();

        int days = analysisPeriodDays > 0 ? analysisPeriodDays : 30;
        Instant now = Instant.now();
        Instant periodStart = now.minus(days, ChronoUnit.DAYS);
        Instant prevStart = periodStart.minus(days, ChronoUnit.DAYS);

        Timestamp tsEnd = Timestamp.from(now);
        Timestamp tsStart = Timestamp.from(periodStart);
        Timestamp tsPrevStart = Timestamp.from(prevStart);

        List<Complaint> currentComplaints = complaintDAO.findComplaintsInDateRange(tsStart, tsEnd);
        List<Complaint> previousComplaints = complaintDAO.findComplaintsInDateRange(tsPrevStart, tsStart);

        AnalysisExecutionResult computed = analyzeHistoricalComplaints(
                currentComplaints, previousComplaints, days, tsStart, tsEnd, admin.getUserId());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                predictiveDAO.saveAnalysisRun(computed.analysis(), computed.assessments(), computed.recommendations(), conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        auditLogDAO.log(new AuditLog(admin.getUserId(), "PREDICTIVE_ANALYSIS_RUN", "PREDICTIVE_ANALYSIS",
                computed.analysis().getId(),
                String.format("Analyzed %d complaints over %d days: %d risk assessments, %d recommendations processed.",
                        currentComplaints.size(), days, computed.assessments().size(), computed.recommendations().size())));

        for (MaintenanceRiskAssessment r : computed.assessments()) {
            if (r.getRiskLevel() == RiskLevel.HIGH || r.getRiskLevel() == RiskLevel.CRITICAL) {
                auditLogDAO.log(new AuditLog(admin.getUserId(), "RISK_ASSESSMENT_CREATED", "MAINTENANCE_RISK",
                        r.getId(), String.format("%s (%s) - %s: Risk Score %.1f [%s] (%d incidents)",
                        r.getBuilding(), r.getLocation(), r.getCategory(), r.getRiskScore(), r.getRiskLevel().name(), r.getIncidentCount())));
            }
        }

        return computed;
    }

    public AnalysisExecutionResult analyzeHistoricalComplaints(List<Complaint> currentComplaints,
                                                                 List<Complaint> previousComplaints,
                                                                 int periodDays,
                                                                 Timestamp periodStart,
                                                                 Timestamp periodEnd,
                                                                 Integer generatedBy) {
        List<Complaint> cur = currentComplaints != null ? currentComplaints : List.of();
        List<Complaint> prev = previousComplaints != null ? previousComplaints : List.of();

        PredictiveAnalysis header = new PredictiveAnalysis();
        header.setAnalysisPeriodStart(periodStart);
        header.setAnalysisPeriodEnd(periodEnd);
        header.setTotalComplaintsAnalyzed(cur.size());
        header.setGeneratedBy(generatedBy);

        if (cur.isEmpty()) {
            header.setAnalysisSummary("No historical complaints found in the selected " + periodDays + "-day analysis window.");
            return new AnalysisExecutionResult(header, List.of(), List.of());
        }

        double wFreq = AppConfig.getDoubleProperty("predictive.weight.frequency", 0.25);
        double wRec = AppConfig.getDoubleProperty("predictive.weight.recurrence", 0.25);
        double wRecent = AppConfig.getDoubleProperty("predictive.weight.recency", 0.20);
        double wSev = AppConfig.getDoubleProperty("predictive.weight.severity", 0.15);
        double wTrend = AppConfig.getDoubleProperty("predictive.weight.trend", 0.15);
        double wSum = wFreq + wRec + wRecent + wSev + wTrend;
        if (wSum <= 0) {
            wFreq = 0.25; wRec = 0.25; wRecent = 0.20; wSev = 0.15; wTrend = 0.15; wSum = 1.0;
        }
        wFreq /= wSum; wRec /= wSum; wRecent /= wSum; wSev /= wSum; wTrend /= wSum;

        int thHighBldgFreq = AppConfig.getIntProperty("predictive.rule.high.building.freq", 4);
        int thRepeatedCat = AppConfig.getIntProperty("predictive.rule.repeated.category.count", 3);
        int thRepeatedLoc = AppConfig.getIntProperty("predictive.rule.repeated.location.count", 2);
        int thHighPrio = AppConfig.getIntProperty("predictive.rule.high.priority.count", 2);
        double thTrendRatio = AppConfig.getDoubleProperty("predictive.rule.trend.growth.ratio", 1.25);
        int thRepeatedAsset = AppConfig.getIntProperty("predictive.rule.repeated.asset.count", 2);

        Map<String, Integer> buildingTotals = new HashMap<>();
        Map<String, List<Complaint>> byBuildingCategory = new LinkedHashMap<>();
        for (Complaint c : cur) {
            String bldg = safeStr(c.getBuilding(), "General Campus");
            String cat = safeStr(c.getCategory(), "Other");
            buildingTotals.merge(bldg, 1, Integer::sum);
            byBuildingCategory.computeIfAbsent(bldg + "||" + cat, k -> new ArrayList<>()).add(c);
        }

        Map<String, Integer> prevByBldgCat = new HashMap<>();
        for (Complaint c : prev) {
            String key = safeStr(c.getBuilding(), "General Campus") + "||" + safeStr(c.getCategory(), "Other");
            prevByBldgCat.merge(key, 1, Integer::sum);
        }

        Instant recentCutoff = periodEnd.toInstant().minus(7, ChronoUnit.DAYS);

        List<MaintenanceRiskAssessment> assessments = new ArrayList<>();
        List<PreventiveRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<String, List<Complaint>> entry : byBuildingCategory.entrySet()) {
            List<Complaint> group = entry.getValue();
            String building = safeStr(group.get(0).getBuilding(), "General Campus");
            String category = safeStr(group.get(0).getCategory(), "Other");
            int bldgTotal = buildingTotals.getOrDefault(building, group.size());
            int catCount = group.size();
            int prevCatCount = prevByBldgCat.getOrDefault(entry.getKey(), 0);

            Map<String, Integer> roomCounts = new HashMap<>();
            Map<String, Integer> assetCounts = new HashMap<>();
            int highCritCount = 0;
            int recent7dCount = 0;
            double sumPrioScore = 0.0;

            for (Complaint c : group) {
                String room = safeStr(c.getRoomNumber(), "General Area");
                roomCounts.merge(room, 1, Integer::sum);

                if (c.getPriority() == PriorityLevel.HIGH || c.getPriority() == PriorityLevel.CRITICAL || c.isSafetyCritical()) {
                    highCritCount++;
                }
                sumPrioScore += c.getPriorityScore();
                if (c.getCreatedAt() != null && !c.getCreatedAt().toInstant().isBefore(recentCutoff)) {
                    recent7dCount++;
                }

                String corpus = (safeStr(c.getTitle(), "") + " " + safeStr(c.getDescription(), "") + " " + room).toLowerCase(Locale.ROOT);
                for (String asset : ASSET_KEYWORDS) {
                    if (corpus.contains(asset)) {
                        assetCounts.merge(asset, 1, Integer::sum);
                    }
                }
            }

            String primaryLocation = roomCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Building-Wide");
            int maxSameRoomCount = roomCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);

            String topAsset = assetCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .filter(e -> e.getValue() >= thRepeatedAsset)
                    .map(Map.Entry::getKey)
                    .orElse(null);

            List<String> triggeredRules = new ArrayList<>();
            if (bldgTotal >= thHighBldgFreq) {
                triggeredRules.add("RULE 1 (High Building Frequency: " + bldgTotal + " total complaints)");
            }
            if (catCount >= thRepeatedCat) {
                triggeredRules.add("RULE 2 (Repeated Category Failures: " + catCount + " " + category + " incidents)");
            }
            if (maxSameRoomCount >= thRepeatedLoc) {
                triggeredRules.add("RULE 3 (Repeated Location Failures: " + maxSameRoomCount + " incidents in " + primaryLocation + ")");
            }
            if (highCritCount >= thHighPrio) {
                triggeredRules.add("RULE 4 (Recurring High-Priority/Safety Issues: " + highCritCount + " high/critical incidents)");
            }
            boolean isIncreasingTrend = (prevCatCount == 0 && catCount >= 2) || (prevCatCount > 0 && ((double) catCount / prevCatCount) >= thTrendRatio);
            if (isIncreasingTrend) {
                triggeredRules.add(String.format("RULE 5 (Increasing Failure Trend: %d vs %d prior period)", catCount, prevCatCount));
            }
            if (topAsset != null) {
                triggeredRules.add("RULE 6 (Repeated Asset Failure: '" + topAsset + "' flagged in " + assetCounts.get(topAsset) + " incidents)");
            }

            double freqFactor = Math.min(100.0, (bldgTotal / 8.0) * 100.0);
            double recurrenceFactor = Math.min(100.0, ((catCount * 16.0) + (maxSameRoomCount >= 2 ? (maxSameRoomCount * 10.0) : 0.0)));
            double recencyFactor = Math.min(100.0, (recent7dCount / 3.0) * 100.0);
            double severityFactor = Math.min(100.0, sumPrioScore / catCount);
            double trendFactor;
            if (isIncreasingTrend) {
                trendFactor = prevCatCount == 0 ? 80.0 : Math.min(100.0, (((double) catCount / prevCatCount) - 1.0) * 100.0 + 45.0);
            } else if (catCount >= 2 && catCount == prevCatCount) {
                trendFactor = 40.0;
            } else {
                trendFactor = 10.0;
            }

            double rawRisk = (wFreq * freqFactor) + (wRec * recurrenceFactor) + (wRecent * recencyFactor)
                    + (wSev * severityFactor) + (wTrend * trendFactor);
            double riskScore = BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, rawRisk)))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            RiskLevel riskLevel = RiskLevel.fromScore(riskScore);

            String recAction = buildCategoryAwareRecommendation(building, primaryLocation, category, catCount, topAsset, triggeredRules);

            StringBuilder factors = new StringBuilder();
            factors.append(String.format(
                    "Weighted Score Breakdown -> Freq=%.1f (x%.0f%%), Recurrence=%.1f (x%.0f%%), Recency=%.1f (x%.0f%%), Severity=%.1f (x%.0f%%), Trend=%.1f (x%.0f%%). ",
                    freqFactor, wFreq * 100, recurrenceFactor, wRec * 100, recencyFactor, wRecent * 100,
                    severityFactor, wSev * 100, trendFactor, wTrend * 100));
            if (!triggeredRules.isEmpty()) {
                factors.append("Detected Patterns: ").append(String.join("; ", triggeredRules)).append(". ");
            }
            factors.append("Note: Historical-data-based risk indicator (not a guaranteed failure prediction).");

            MaintenanceRiskAssessment risk = new MaintenanceRiskAssessment();
            risk.setBuilding(building);
            risk.setLocation(maxSameRoomCount >= 2 ? primaryLocation : "Multiple Areas");
            risk.setCategory(category);
            risk.setIncidentCount(catCount);
            risk.setRiskScore(riskScore);
            risk.setRiskLevel(riskLevel);
            risk.setContributingFactors(factors.toString());
            risk.setRecommendation(recAction);
            assessments.add(risk);

            if (!triggeredRules.isEmpty() || riskScore >= 25.0) {
                PreventiveRecommendation rec = new PreventiveRecommendation();
                rec.setBuilding(building);
                rec.setLocation(risk.getLocation());
                rec.setCategory(category);
                rec.setRiskScore(riskScore);
                rec.setSupportingComplaintCount(catCount);
                rec.setAnalysisPeriodDays(periodDays);
                rec.setUrgency(riskLevel.getLabel());
                rec.setRecommendation(recAction);
                rec.setStatus(RecommendationStatus.NEW);
                rec.setCreatedBy(generatedBy);
                recommendations.add(rec);
            }
        }

        assessments.sort(Comparator.comparingDouble(MaintenanceRiskAssessment::getRiskScore).reversed()
                .thenComparing(Comparator.comparingInt(MaintenanceRiskAssessment::getIncidentCount).reversed()));

        long highRiskCount = assessments.stream().filter(a -> a.getRiskLevel() == RiskLevel.HIGH || a.getRiskLevel() == RiskLevel.CRITICAL).count();
        header.setAnalysisSummary(String.format(
                "Analyzed %d historical complaints (%d-day window) across %d building/category clusters: %d high/critical maintenance risks identified.",
                cur.size(), periodDays, assessments.size(), highRiskCount));

        return new AnalysisExecutionResult(header, assessments, recommendations);
    }

    public PredictiveDashboardData getPredictiveDashboardData(String buildingFilter, String categoryFilter,
                                                              String riskLevelFilter, String recStatusFilter,
                                                              int trendMonths) throws SQLException {
        UserSession.requireRole(Role.ADMIN);

        Optional<PredictiveAnalysis> latest = predictiveDAO.findLatestAnalysis();
        Integer analysisId = latest.map(PredictiveAnalysis::getId).orElse(null);

        List<MaintenanceRiskAssessment> assessments = (analysisId != null)
                ? predictiveDAO.findAssessments(analysisId, buildingFilter, categoryFilter, riskLevelFilter)
                : List.of();
        List<MaintenanceRiskAssessment> allLatestAssessments = (analysisId != null)
                ? predictiveDAO.findAssessments(analysisId, null, null, null)
                : List.of();

        List<PreventiveRecommendation> recommendations = predictiveDAO.findRecommendations(
                buildingFilter, categoryFilter, recStatusFilter);
        List<PreventiveRecommendation> allRecommendations = predictiveDAO.findRecommendations(null, null, null);

        Set<String> buildingsSet = new HashSet<>();
        Set<String> recurringLocations = new HashSet<>();
        Set<String> highRiskBuildings = new HashSet<>();
        int criticalRisks = 0;

        for (MaintenanceRiskAssessment r : allLatestAssessments) {
            buildingsSet.add(r.getBuilding());
            if (r.getIncidentCount() >= 2) {
                recurringLocations.add(r.getBuilding() + "|" + r.getLocation());
            }
            if (r.getRiskLevel() == RiskLevel.HIGH || r.getRiskLevel() == RiskLevel.CRITICAL) {
                highRiskBuildings.add(r.getBuilding());
            }
            if (r.getRiskLevel() == RiskLevel.CRITICAL) {
                criticalRisks++;
            }
        }

        int pendingActions = 0;
        int completedActions = 0;
        for (PreventiveRecommendation rec : allRecommendations) {
            if (rec.getStatus() == RecommendationStatus.COMPLETED) {
                completedActions++;
            } else if (rec.getStatus() != RecommendationStatus.DISMISSED) {
                pendingActions++;
            }
        }

        Map<String, Integer> complaintsByBuilding = new LinkedHashMap<>();
        List<Complaint> allComplaints = complaintDAO.findComplaints(null, null, null, null, null, null, null, null, "created", false);
        for (Complaint c : allComplaints) {
            String b = safeStr(c.getBuilding(), "Unknown");
            complaintsByBuilding.merge(b, 1, Integer::sum);
        }

        Map<String, Integer> recurringByCategory = new LinkedHashMap<>();
        for (MaintenanceRiskAssessment r : allLatestAssessments) {
            if (r.getIncidentCount() >= 2) {
                recurringByCategory.merge(r.getCategory(), r.getIncidentCount(), Integer::sum);
            }
        }

        Map<String, Integer> monthlyTrends = predictiveDAO.getMonthlyMaintenanceTrends(trendMonths);

        PredictiveDashboardMetrics metrics = new PredictiveDashboardMetrics(
                buildingsSet.size(),
                recurringLocations.size(),
                highRiskBuildings.size(),
                criticalRisks,
                pendingActions,
                completedActions
        );

        return new PredictiveDashboardData(
                metrics, latest, assessments, recommendations,
                complaintsByBuilding, recurringByCategory, monthlyTrends
        );
    }

    public void updateRecommendationLifecycle(int recId, RecommendationStatus targetStatus, Timestamp scheduledDate) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User admin = UserSession.getCurrentUser();

        PreventiveRecommendation existing = predictiveDAO.findRecommendationById(recId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation #" + recId + " not found."));

        Timestamp completedAt = (targetStatus == RecommendationStatus.COMPLETED)
                ? new Timestamp(System.currentTimeMillis()) : null;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                predictiveDAO.updateRecommendationStatus(recId, targetStatus, scheduledDate, completedAt, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        String auditAction = switch (targetStatus) {
            case APPROVED -> "RECOMMENDATION_APPROVED";
            case SCHEDULED -> "MAINTENANCE_SCHEDULED";
            case COMPLETED -> "MAINTENANCE_COMPLETED";
            case DISMISSED -> "RECOMMENDATION_DISMISSED";
            default -> "RECOMMENDATION_UNDER_REVIEW";
        };

        auditLogDAO.log(new AuditLog(admin.getUserId(), auditAction, "PREVENTIVE_RECOMMENDATION", recId,
                String.format("%s (%s) transitioned from %s to %s",
                        existing.getBuilding(), existing.getCategory(), existing.getStatus().name(), targetStatus.name())));
    }

    private String buildCategoryAwareRecommendation(String building, String location, String category,
                                                    int count, String topAsset, List<String> triggeredRules) {
        String lowerCat = category.toLowerCase(Locale.ROOT);
        String assetNote = topAsset != null ? " (repeatedly involving '" + topAsset + "')" : "";
        String locPhrase = (!"Building-Wide".equals(location) && !"Multiple Areas".equals(location))
                ? " centered around " + location : "";

        if (lowerCat.contains("electric")) {
            return String.format("Repeated electrical incidents (%d) detected in %s%s%s. Recommend a comprehensive electrical safety, circuit breaker, and wiring inspection.",
                    count, building, locPhrase, assetNote);
        }
        if (lowerCat.contains("plumb") || lowerCat.contains("water")) {
            return String.format("Recurring plumbing/water complaints (%d) detected in %s%s%s. Recommend pressurized pipeline, valve seal, and drainage inspection.",
                    count, building, locPhrase, assetNote);
        }
        if (lowerCat.contains("wi-fi") || lowerCat.contains("internet") || lowerCat.contains("network")) {
            return String.format("Repeated network outages (%d) detected in %s%s%s. Recommend core switch, access point, and uplink cable diagnostics.",
                    count, building, locPhrase, assetNote);
        }
        if (lowerCat.contains("safety") || lowerCat.contains("security")) {
            return String.format("Recurring safety/security complaints (%d) detected in %s%s%s. Recommend immediate hazard audit and preventive security controls.",
                    count, building, locPhrase, assetNote);
        }
        if (lowerCat.contains("furniture") || lowerCat.contains("infrastructure") || lowerCat.contains("structural")) {
            return String.format("Recurring structural/infrastructure complaints (%d) detected in %s%s%s. Recommend professional civil & structural facility inspection.",
                    count, building, locPhrase, assetNote);
        }
        if (lowerCat.contains("lab") || lowerCat.contains("classroom") || lowerCat.contains("equipment")) {
            return String.format("Repeated %s failures (%d) detected in %s%s%s. Recommend preventive calibration and hardware servicing cycle.",
                    category.toLowerCase(Locale.ROOT), count, building, locPhrase, assetNote);
        }
        return String.format("%s shows %d %s maintenance incidents%s%s (%d detection rule(s)). Schedule preventive facility inspection.",
                building, count, category.toLowerCase(Locale.ROOT), locPhrase, assetNote, triggeredRules.size());
    }

    private static String safeStr(String s, String fallback) {
        return (s != null && !s.trim().isEmpty()) ? s.trim() : fallback;
    }
}
