package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;

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

public class ComplaintDAO {

    public Complaint create(Complaint complaint, Connection conn) throws SQLException {
        String sql = "INSERT INTO complaints (student_id, title, description, category, building, room_number, " +
                     "severity, safety_risk, people_affected, operational_impact, priority_score, priority, " +
                     "is_safety_critical, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean isLocalConn = (conn == null);
        Connection actualConn = isLocalConn ? DBConnection.getConnection() : conn;

        try (PreparedStatement ps = actualConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, complaint.getStudentId());
            ps.setString(2, complaint.getTitle());
            ps.setString(3, complaint.getDescription());
            ps.setString(4, complaint.getCategory());
            ps.setString(5, complaint.getBuilding());
            ps.setString(6, complaint.getRoomNumber());
            ps.setInt(7, complaint.getSeverity());
            ps.setInt(8, complaint.getSafetyRisk());
            ps.setInt(9, complaint.getPeopleAffected());
            ps.setInt(10, complaint.getOperationalImpact());
            ps.setDouble(11, complaint.getPriorityScore());
            ps.setString(12, complaint.getPriority().name());
            ps.setBoolean(13, complaint.isSafetyCritical());
            ps.setString(14, complaint.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating complaint failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    complaint.setComplaintId(rs.getInt(1));
                }
            }
        } finally {
            if (isLocalConn && actualConn != null) {
                actualConn.close();
            }
        }
        return complaint;
    }

    public boolean updateStatusAndResolvedAt(int complaintId, ComplaintStatus newStatus,
                                             Timestamp resolvedAt, Connection conn) throws SQLException {
        String sql = "UPDATE complaints SET status = ?, resolved_at = ? WHERE complaint_id = ?";
        boolean isLocalConn = (conn == null);
        Connection actualConn = isLocalConn ? DBConnection.getConnection() : conn;

        try (PreparedStatement ps = actualConn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setTimestamp(2, resolvedAt);
            ps.setInt(3, complaintId);
            return ps.executeUpdate() > 0;
        } finally {
            if (isLocalConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public Optional<Complaint> findById(int complaintId) throws SQLException {
        String sql = "SELECT c.*, u.full_name as student_name, u.email as student_email, " +
                     "s.user_id as assigned_staff_id, s.full_name as assigned_staff_name " +
                     "FROM complaints c " +
                     "JOIN users u ON c.student_id = u.user_id " +
                     "LEFT JOIN complaint_assignments ca ON c.complaint_id = ca.complaint_id AND ca.is_active = TRUE " +
                     "LEFT JOIN users s ON ca.staff_id = s.user_id " +
                     "WHERE c.complaint_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToComplaint(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Complaint> findComplaints(Integer studentId, Integer assignedStaffId,
                                         String statusFilter, String priorityFilter,
                                         String categoryFilter, String buildingFilter,
                                         Boolean safetyCriticalOnly, String searchQuery,
                                         String sortBy, boolean sortAsc) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT c.*, u.full_name as student_name, u.email as student_email, " +
                "s.user_id as assigned_staff_id, s.full_name as assigned_staff_name " +
                "FROM complaints c " +
                "JOIN users u ON c.student_id = u.user_id " +
                "LEFT JOIN complaint_assignments ca ON c.complaint_id = ca.complaint_id AND ca.is_active = TRUE " +
                "LEFT JOIN users s ON ca.staff_id = s.user_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (studentId != null) {
            sql.append(" AND c.student_id = ?");
            params.add(studentId);
        }

        if (assignedStaffId != null) {
            sql.append(" AND ca.staff_id = ?");
            params.add(assignedStaffId);
        }

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.status = ?");
            params.add(statusFilter);
        }

        if (priorityFilter != null && !priorityFilter.isBlank() && !priorityFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.priority = ?");
            params.add(priorityFilter);
        }

        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.category = ?");
            params.add(categoryFilter);
        }

        if (buildingFilter != null && !buildingFilter.isBlank() && !buildingFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.building = ?");
            params.add(buildingFilter);
        }

        if (Boolean.TRUE.equals(safetyCriticalOnly)) {
            sql.append(" AND (c.is_safety_critical = TRUE OR c.priority = 'CRITICAL')");
        }

        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (LOWER(c.title) LIKE ? OR LOWER(c.description) LIKE ? OR LOWER(c.building) LIKE ? " +
                       "OR LOWER(c.room_number) LIKE ? OR CAST(c.complaint_id AS CHAR) LIKE ? OR LOWER(u.full_name) LIKE ?)");
            String q = "%" + searchQuery.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        String orderCol = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "id" -> "c.complaint_id";
            case "date" -> "c.created_at";
            case "status" -> "c.status";
            case "title" -> "c.title";
            default -> "c.priority_score";
        };

        String direction = sortAsc ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(orderCol).append(" ").append(direction).append(", c.created_at DESC");

        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToComplaint(rs));
                }
            }
        }
        return list;
    }

    public DashboardStats calculateDashboardStats(Integer studentId, Integer staffId) throws SQLException {
        DashboardStats stats = new DashboardStats();

        StringBuilder baseWhere = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (studentId != null) {
            baseWhere.append(" AND c.student_id = ? ");
            params.add(studentId);
        }
        if (staffId != null) {
            baseWhere.append(" AND c.complaint_id IN (SELECT complaint_id FROM complaint_assignments WHERE staff_id = ? AND is_active = TRUE) ");
            params.add(staffId);
        }

        String statusSql = "SELECT c.status, COUNT(*) as cnt FROM complaints c " + baseWhere + " GROUP BY c.status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(statusSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("cnt");
                    total += count;
                    stats.getComplaintsByStatus().put(status, count);
                    switch (status) {
                        case "SUBMITTED" -> stats.setSubmittedComplaints(count);
                        case "ACKNOWLEDGED" -> stats.setAcknowledgedComplaints(count);
                        case "IN_PROGRESS" -> stats.setInProgressComplaints(count);
                        case "RESOLVED" -> stats.setResolvedComplaints(count);
                        case "REJECTED" -> stats.setRejectedComplaints(count);
                    }
                }
                stats.setTotalComplaints(total);
            }
        }

        String critSql = "SELECT COUNT(*) FROM complaints c " + baseWhere + " AND (c.is_safety_critical = TRUE OR c.priority = 'CRITICAL')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(critSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setCriticalComplaints(rs.getInt(1));
            }
        }

        String catSql = "SELECT c.category, COUNT(*) as cnt FROM complaints c " + baseWhere + " GROUP BY c.category ORDER BY cnt DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(catSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.getComplaintsByCategory().put(rs.getString("category"), rs.getInt("cnt"));
                }
            }
        }

        String prioSql = "SELECT c.priority, COUNT(*) as cnt FROM complaints c " + baseWhere + " GROUP BY c.priority";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(prioSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.getComplaintsByPriority().put(rs.getString("priority"), rs.getInt("cnt"));
                }
            }
        }

        int critSla = com.smartcampus.config.AppConfig.getIntProperty("sla.critical.hours", 4);
        int highSla = com.smartcampus.config.AppConfig.getIntProperty("sla.high.hours", 24);
        int medSla = com.smartcampus.config.AppConfig.getIntProperty("sla.medium.hours", 72);
        int lowSla = com.smartcampus.config.AppConfig.getIntProperty("sla.low.hours", 168);

        String overdueSql = "SELECT COUNT(*) FROM complaints c " + baseWhere +
                            " AND c.status NOT IN ('RESOLVED', 'REJECTED') " +
                            " AND TIMESTAMPDIFF(MINUTE, c.created_at, NOW()) > (" +
                            "   CASE c.priority " +
                            "     WHEN 'CRITICAL' THEN ? * 60 " +
                            "     WHEN 'HIGH' THEN ? * 60 " +
                            "     WHEN 'MEDIUM' THEN ? * 60 " +
                            "     ELSE ? * 60 " +
                            "   END)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(overdueSql)) {
            int idx = 1;
            for (Object param : params) ps.setObject(idx++, param);
            ps.setInt(idx++, critSla);
            ps.setInt(idx++, highSla);
            ps.setInt(idx++, medSla);
            ps.setInt(idx, lowSla);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setOverdueComplaints(rs.getInt(1));
            }
        }

        String avgResSql = "SELECT AVG(TIMESTAMPDIFF(HOUR, c.created_at, c.resolved_at)) FROM complaints c " +
                           baseWhere + " AND c.status = 'RESOLVED' AND c.resolved_at IS NOT NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(avgResSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setAvgResolutionHours(rs.getDouble(1));
                }
            }
        }

        return stats;
    }

    public Map<String, Integer> getComplaintTrends(int days) throws SQLException {
        Map<String, Integer> trends = new LinkedHashMap<>();
        int d = (days > 0) ? days : 14;
        String sql = "SELECT DATE_FORMAT(c.created_at, '%m-%d') as day_label, COUNT(*) as cnt " +
                     "FROM complaints c " +
                     "WHERE c.created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                     "GROUP BY DATE(c.created_at), DATE_FORMAT(c.created_at, '%m-%d') " +
                     "ORDER BY DATE(c.created_at) ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trends.put(rs.getString("day_label"), rs.getInt("cnt"));
                }
            }
        }
        return trends;
    }

    public Map<String, Integer> getComplaintsByDepartment() throws SQLException {
        Map<String, Integer> deptMap = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(u.department, 'General / Other') as dept, COUNT(*) as cnt " +
                     "FROM complaints c " +
                     "JOIN users u ON c.student_id = u.user_id " +
                     "GROUP BY u.department " +
                     "ORDER BY cnt DESC LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                deptMap.put(rs.getString("dept"), rs.getInt("cnt"));
            }
        }
        return deptMap;
    }

    public List<String> getDistinctBuildings() throws SQLException {
        List<String> buildings = new ArrayList<>();
        String sql = "SELECT DISTINCT building FROM complaints WHERE building IS NOT NULL AND TRIM(building) != '' ORDER BY building ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                buildings.add(rs.getString("building"));
            }
        }
        return buildings;
    }

    public int countUnassignedSafetyIssues() throws SQLException {
        String sql = "SELECT COUNT(*) FROM complaints c " +
                     "WHERE (c.is_safety_critical = TRUE OR c.priority = 'CRITICAL') " +
                     "AND c.status IN ('SUBMITTED', 'ACKNOWLEDGED') " +
                     "AND c.complaint_id NOT IN (SELECT complaint_id FROM complaint_assignments WHERE is_active = TRUE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<com.smartcampus.model.StaffPerformanceDTO> getStaffPerformanceMetrics() throws SQLException {
        List<com.smartcampus.model.StaffPerformanceDTO> metrics = new ArrayList<>();
        String sql = "SELECT u.user_id, u.full_name, u.department, " +
                     "COUNT(DISTINCT ca.complaint_id) as assigned_count, " +
                     "COUNT(DISTINCT CASE WHEN c.status = 'RESOLVED' THEN c.complaint_id END) as resolved_count, " +
                     "COALESCE(AVG(CASE WHEN c.status = 'RESOLVED' AND c.resolved_at IS NOT NULL " +
                     "THEN TIMESTAMPDIFF(HOUR, ca.assigned_at, c.resolved_at) END), 0.0) as avg_turnaround, " +
                     "COALESCE(AVG(f.rating), 0.0) as avg_rating, " +
                     "COUNT(DISTINCT CASE WHEN ca.is_active = TRUE AND c.status IN ('ACKNOWLEDGED', 'IN_PROGRESS') THEN ca.complaint_id END) as active_tasks " +
                     "FROM users u " +
                     "LEFT JOIN complaint_assignments ca ON u.user_id = ca.staff_id " +
                     "LEFT JOIN complaints c ON ca.complaint_id = c.complaint_id " +
                     "LEFT JOIN complaint_feedback f ON c.complaint_id = f.complaint_id " +
                     "WHERE u.role = 'STAFF' " +
                     "GROUP BY u.user_id, u.full_name, u.department " +
                     "ORDER BY resolved_count DESC, avg_rating DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                metrics.add(new com.smartcampus.model.StaffPerformanceDTO(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("department"),
                        rs.getInt("assigned_count"),
                        rs.getInt("resolved_count"),
                        rs.getDouble("avg_turnaround"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("active_tasks")
                ));
            }
        }
        return metrics;
    }

    public List<com.smartcampus.model.ActivityFeedItem> getRecentActivities(int limit) throws SQLException {
        List<com.smartcampus.model.ActivityFeedItem> list = new ArrayList<>();
        int lim = (limit > 0) ? limit : 20;

        String sql = "SELECT a.audit_id, a.actor_user_id, u.full_name as actor_name, u.role as actor_role, " +
                     "a.action, a.entity_type, a.entity_id, a.details, a.created_at " +
                     "FROM audit_logs a " +
                     "LEFT JOIN users u ON a.actor_user_id = u.user_id " +
                     "ORDER BY a.audit_id DESC LIMIT ?";

        long now = System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int auditId = rs.getInt("audit_id");
                    String actor = rs.getString("actor_name");
                    String role = rs.getString("actor_role");
                    String action = rs.getString("action");
                    String entityType = rs.getString("entity_type");
                    int entityIdVal = rs.getInt("entity_id");
                    Integer entityId = rs.wasNull() ? null : entityIdVal;
                    String details = rs.getString("details");
                    Timestamp ts = rs.getTimestamp("created_at");

                    String relTime = "Just now";
                    if (ts != null) {
                        long diffMs = now - ts.getTime();
                        long mins = diffMs / (60 * 1000);
                        long hours = diffMs / (60 * 60 * 1000);
                        long days = diffMs / (24 * 60 * 60 * 1000);
                        if (mins < 2) relTime = "Just now";
                        else if (mins < 60) relTime = mins + "m ago";
                        else if (hours < 24) relTime = hours + "h ago";
                        else relTime = days + "d ago";
                    }

                    boolean isCrit = (details != null && details.toLowerCase().contains("critical"))
                            || "CRITICAL".equalsIgnoreCase(action);

                    list.add(new com.smartcampus.model.ActivityFeedItem(
                            auditId,
                            actor != null ? actor : "System",
                            role != null ? role : "SYSTEM",
                            action,
                            entityType,
                            entityId,
                            details,
                            ts,
                            relTime,
                            isCrit
                    ));
                }
            }
        }
        return list;
    }

    public List<Complaint> findActiveComplaintsForEmergency(Integer assignedStaffId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, u.full_name as student_name, u.email as student_email, " +
                "s.user_id as assigned_staff_id, s.full_name as assigned_staff_name " +
                "FROM complaints c " +
                "JOIN users u ON c.student_id = u.user_id " +
                "LEFT JOIN complaint_assignments ca ON c.complaint_id = ca.complaint_id AND ca.is_active = TRUE " +
                "LEFT JOIN users s ON ca.staff_id = s.user_id " +
                "WHERE c.status NOT IN ('RESOLVED', 'REJECTED') "
        );
        if (assignedStaffId != null) {
            sql.append(" AND ca.staff_id = ? ");
        }
        sql.append(" ORDER BY c.is_safety_critical DESC, c.priority_score DESC, c.created_at ASC");

        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (assignedStaffId != null) {
                ps.setInt(1, assignedStaffId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToComplaint(rs));
                }
            }
        }
        return list;
    }

    public List<Complaint> findComplaintsInDateRange(Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT c.*, u.full_name as student_name, u.email as student_email, " +
                "s.user_id as assigned_staff_id, s.full_name as assigned_staff_name " +
                "FROM complaints c " +
                "JOIN users u ON c.student_id = u.user_id " +
                "LEFT JOIN complaint_assignments ca ON c.complaint_id = ca.complaint_id AND ca.is_active = TRUE " +
                "LEFT JOIN users s ON ca.staff_id = s.user_id " +
                "WHERE c.created_at >= ? AND c.created_at <= ? " +
                "ORDER BY c.created_at ASC";
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToComplaint(rs));
                }
            }
        }
        return list;
    }

    private Complaint mapRowToComplaint(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setComplaintId(rs.getInt("complaint_id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setCategory(rs.getString("category"));
        c.setBuilding(rs.getString("building"));
        c.setRoomNumber(rs.getString("room_number"));
        c.setSeverity(rs.getInt("severity"));
        c.setSafetyRisk(rs.getInt("safety_risk"));
        c.setPeopleAffected(rs.getInt("people_affected"));
        c.setOperationalImpact(rs.getInt("operational_impact"));
        c.setPriorityScore(rs.getDouble("priority_score"));
        c.setPriority(PriorityLevel.fromString(rs.getString("priority")));
        c.setSafetyCritical(rs.getBoolean("is_safety_critical"));
        c.setStatus(ComplaintStatus.fromString(rs.getString("status")));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        c.setResolvedAt(rs.getTimestamp("resolved_at"));

        try {
            c.setStudentName(rs.getString("student_name"));
            c.setStudentEmail(rs.getString("student_email"));
        } catch (SQLException ignored) {}

        try {
            int staffId = rs.getInt("assigned_staff_id");
            if (!rs.wasNull()) {
                c.setAssignedStaffId(staffId);
                c.setAssignedStaffName(rs.getString("assigned_staff_name"));
            }
        } catch (SQLException ignored) {}

        return c;
    }
}
