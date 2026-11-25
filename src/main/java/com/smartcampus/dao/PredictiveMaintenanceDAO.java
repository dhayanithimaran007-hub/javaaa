package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.MaintenanceRiskAssessment;
import com.smartcampus.model.PredictiveAnalysis;
import com.smartcampus.model.PreventiveRecommendation;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.RiskLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PredictiveMaintenanceDAO {

    public void saveAnalysisRun(PredictiveAnalysis analysis,
                                List<MaintenanceRiskAssessment> assessments,
                                List<PreventiveRecommendation> recommendations,
                                Connection conn) throws SQLException {
        boolean localConn = (conn == null);
        Connection actualConn = localConn ? DBConnection.getConnection() : conn;
        try {
            if (localConn) {
                actualConn.setAutoCommit(false);
            }

            String insertAnalysisSql = "INSERT INTO predictive_analysis " +
                    "(analysis_period_start, analysis_period_end, total_complaints_analyzed, analysis_summary, generated_by) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = actualConn.prepareStatement(insertAnalysisSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setTimestamp(1, analysis.getAnalysisPeriodStart());
                ps.setTimestamp(2, analysis.getAnalysisPeriodEnd());
                ps.setInt(3, analysis.getTotalComplaintsAnalyzed());
                ps.setString(4, analysis.getAnalysisSummary());
                if (analysis.getGeneratedBy() != null) {
                    ps.setInt(5, analysis.getGeneratedBy());
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        analysis.setId(keys.getInt(1));
                    }
                }
            }

            Map<String, Integer> assessmentIdByKey = new LinkedHashMap<>();
            String insertRiskSql = "INSERT INTO maintenance_risk_assessments " +
                    "(analysis_id, building, location, category, incident_count, risk_score, risk_level, contributing_factors, recommendation) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = actualConn.prepareStatement(insertRiskSql, Statement.RETURN_GENERATED_KEYS)) {
                for (MaintenanceRiskAssessment risk : assessments) {
                    risk.setAnalysisId(analysis.getId());
                    ps.setInt(1, analysis.getId());
                    ps.setString(2, risk.getBuilding());
                    ps.setString(3, risk.getLocation());
                    ps.setString(4, risk.getCategory());
                    ps.setInt(5, risk.getIncidentCount());
                    ps.setDouble(6, risk.getRiskScore());
                    ps.setString(7, risk.getRiskLevel().name());
                    ps.setString(8, risk.getContributingFactors());
                    ps.setString(9, risk.getRecommendation());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            risk.setId(keys.getInt(1));
                            String key = buildPatternKey(risk.getBuilding(), risk.getLocation(), risk.getCategory());
                            assessmentIdByKey.put(key, risk.getId());
                        }
                    }
                }
            }

            String findActiveSql = "SELECT id FROM preventive_maintenance_recommendations " +
                    "WHERE LOWER(building) = LOWER(?) AND LOWER(location) = LOWER(?) AND LOWER(category) = LOWER(?) " +
                    "  AND status IN ('NEW', 'UNDER_REVIEW', 'APPROVED', 'SCHEDULED') " +
                    "ORDER BY id DESC LIMIT 1";
            String updateRecSql = "UPDATE preventive_maintenance_recommendations " +
                    "SET risk_assessment_id = ?, risk_score = ?, supporting_complaint_count = ?, " +
                    "    analysis_period_days = ?, urgency = ?, recommendation = ? " +
                    "WHERE id = ?";
            String insertRecSql = "INSERT INTO preventive_maintenance_recommendations " +
                    "(risk_assessment_id, building, location, category, risk_score, supporting_complaint_count, " +
                    " analysis_period_days, urgency, recommendation, status, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            for (PreventiveRecommendation rec : recommendations) {
                String key = buildPatternKey(rec.getBuilding(), rec.getLocation(), rec.getCategory());
                int assessmentId = assessmentIdByKey.getOrDefault(key, rec.getRiskAssessmentId());
                rec.setRiskAssessmentId(assessmentId);

                Integer existingId = null;
                try (PreparedStatement findPs = actualConn.prepareStatement(findActiveSql)) {
                    findPs.setString(1, rec.getBuilding());
                    findPs.setString(2, rec.getLocation());
                    findPs.setString(3, rec.getCategory());
                    try (ResultSet rs = findPs.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("id");
                        }
                    }
                }

                if (existingId != null) {
                    try (PreparedStatement upPs = actualConn.prepareStatement(updateRecSql)) {
                        upPs.setInt(1, assessmentId);
                        upPs.setDouble(2, rec.getRiskScore());
                        upPs.setInt(3, rec.getSupportingComplaintCount());
                        upPs.setInt(4, rec.getAnalysisPeriodDays());
                        upPs.setString(5, rec.getUrgency());
                        upPs.setString(6, rec.getRecommendation());
                        upPs.setInt(7, existingId);
                        upPs.executeUpdate();
                        rec.setId(existingId);
                    }
                } else {
                    try (PreparedStatement inPs = actualConn.prepareStatement(insertRecSql, Statement.RETURN_GENERATED_KEYS)) {
                        inPs.setInt(1, assessmentId);
                        inPs.setString(2, rec.getBuilding());
                        inPs.setString(3, rec.getLocation());
                        inPs.setString(4, rec.getCategory());
                        inPs.setDouble(5, rec.getRiskScore());
                        inPs.setInt(6, rec.getSupportingComplaintCount());
                        inPs.setInt(7, rec.getAnalysisPeriodDays());
                        inPs.setString(8, rec.getUrgency());
                        inPs.setString(9, rec.getRecommendation());
                        inPs.setString(10, rec.getStatus() != null ? rec.getStatus().name() : RecommendationStatus.NEW.name());
                        if (rec.getCreatedBy() != null) {
                            inPs.setInt(11, rec.getCreatedBy());
                        } else {
                            inPs.setNull(11, java.sql.Types.INTEGER);
                        }
                        inPs.executeUpdate();
                        try (ResultSet keys = inPs.getGeneratedKeys()) {
                            if (keys.next()) {
                                rec.setId(keys.getInt(1));
                            }
                        }
                    }
                }
            }

            if (localConn) {
                actualConn.commit();
            }
        } catch (SQLException e) {
            if (localConn) {
                actualConn.rollback();
            }
            throw e;
        } finally {
            if (localConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public Optional<PredictiveAnalysis> findLatestAnalysis() throws SQLException {
        String sql = "SELECT p.*, u.full_name AS generated_by_name " +
                "FROM predictive_analysis p " +
                "LEFT JOIN users u ON p.generated_by = u.user_id " +
                "ORDER BY p.id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                PredictiveAnalysis p = new PredictiveAnalysis();
                p.setId(rs.getInt("id"));
                p.setAnalysisPeriodStart(rs.getTimestamp("analysis_period_start"));
                p.setAnalysisPeriodEnd(rs.getTimestamp("analysis_period_end"));
                p.setAnalysisTimestamp(rs.getTimestamp("analysis_timestamp"));
                p.setTotalComplaintsAnalyzed(rs.getInt("total_complaints_analyzed"));
                p.setAnalysisSummary(rs.getString("analysis_summary"));
                int genBy = rs.getInt("generated_by");
                if (!rs.wasNull()) p.setGeneratedBy(genBy);
                p.setGeneratedByName(rs.getString("generated_by_name"));
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public List<MaintenanceRiskAssessment> findAssessments(Integer analysisId, String buildingFilter,
                                                           String categoryFilter, String riskLevelFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM maintenance_risk_assessments WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (analysisId != null) {
            sql.append(" AND analysis_id = ?");
            params.add(analysisId);
        }
        if (buildingFilter != null && !buildingFilter.isBlank() && !buildingFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND building = ?");
            params.add(buildingFilter);
        }
        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND category = ?");
            params.add(categoryFilter);
        }
        if (riskLevelFilter != null && !riskLevelFilter.isBlank() && !riskLevelFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND risk_level = ?");
            params.add(riskLevelFilter.toUpperCase());
        }
        sql.append(" ORDER BY risk_score DESC, incident_count DESC");

        List<MaintenanceRiskAssessment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MaintenanceRiskAssessment r = new MaintenanceRiskAssessment();
                    r.setId(rs.getInt("id"));
                    r.setAnalysisId(rs.getInt("analysis_id"));
                    r.setBuilding(rs.getString("building"));
                    r.setLocation(rs.getString("location"));
                    r.setCategory(rs.getString("category"));
                    r.setIncidentCount(rs.getInt("incident_count"));
                    r.setRiskScore(rs.getDouble("risk_score"));
                    r.setRiskLevel(RiskLevel.fromString(rs.getString("risk_level")));
                    r.setContributingFactors(rs.getString("contributing_factors"));
                    r.setRecommendation(rs.getString("recommendation"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public List<PreventiveRecommendation> findRecommendations(String buildingFilter, String categoryFilter,
                                                              String statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM preventive_maintenance_recommendations WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (buildingFilter != null && !buildingFilter.isBlank() && !buildingFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND building = ?");
            params.add(buildingFilter);
        }
        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND category = ?");
            params.add(categoryFilter);
        }
        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND status = ?");
            params.add(statusFilter.toUpperCase());
        }
        sql.append(" ORDER BY FIELD(status, 'NEW', 'UNDER_REVIEW', 'APPROVED', 'SCHEDULED', 'COMPLETED', 'DISMISSED'), risk_score DESC, id DESC");

        List<PreventiveRecommendation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecommendation(rs));
                }
            }
        }
        return list;
    }

    public Optional<PreventiveRecommendation> findRecommendationById(int recId) throws SQLException {
        String sql = "SELECT * FROM preventive_maintenance_recommendations WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRecommendation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateRecommendationStatus(int recId, RecommendationStatus newStatus,
                                              Timestamp scheduledDate, Timestamp completedAt,
                                              Connection conn) throws SQLException {
        boolean localConn = (conn == null);
        Connection actualConn = localConn ? DBConnection.getConnection() : conn;
        String sql = "UPDATE preventive_maintenance_recommendations " +
                "SET status = ?, scheduled_date = COALESCE(?, scheduled_date), completed_at = COALESCE(?, completed_at) " +
                "WHERE id = ?";
        try (PreparedStatement ps = actualConn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setTimestamp(2, scheduledDate);
            ps.setTimestamp(3, completedAt);
            ps.setInt(4, recId);
            return ps.executeUpdate() > 0;
        } finally {
            if (localConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public Map<String, Integer> getMonthlyMaintenanceTrends(int months) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        int m = months > 0 ? months : 6;
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, COUNT(*) AS cnt " +
                "FROM complaints " +
                "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                "GROUP BY ym ORDER BY ym ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("ym"), rs.getInt("cnt"));
                }
            }
        }
        return map;
    }

    private PreventiveRecommendation mapRecommendation(ResultSet rs) throws SQLException {
        PreventiveRecommendation r = new PreventiveRecommendation();
        r.setId(rs.getInt("id"));
        r.setRiskAssessmentId(rs.getInt("risk_assessment_id"));
        r.setBuilding(rs.getString("building"));
        r.setLocation(rs.getString("location"));
        r.setCategory(rs.getString("category"));
        r.setRiskScore(rs.getDouble("risk_score"));
        r.setSupportingComplaintCount(rs.getInt("supporting_complaint_count"));
        r.setAnalysisPeriodDays(rs.getInt("analysis_period_days"));
        r.setUrgency(rs.getString("urgency"));
        r.setRecommendation(rs.getString("recommendation"));
        r.setStatus(RecommendationStatus.fromString(rs.getString("status")));
        r.setScheduledDate(rs.getTimestamp("scheduled_date"));
        r.setCompletedAt(rs.getTimestamp("completed_at"));
        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) r.setCreatedBy(createdBy);
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        return r;
    }

    private static String buildPatternKey(String building, String location, String category) {
        return (building != null ? building.trim().toLowerCase() : "") + "|"
                + (location != null ? location.trim().toLowerCase() : "") + "|"
                + (category != null ? category.trim().toLowerCase() : "");
    }
}
