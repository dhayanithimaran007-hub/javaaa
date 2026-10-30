package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.DashboardStats;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(ComplaintDAO.class);

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

        // Sorting: default is high priority first (CRITICAL, HIGH, MEDIUM, LOW) and newest
        String orderCol = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "id" -> "c.complaint_id";
            case "date" -> "c.created_at";
            case "status" -> "c.status";
            case "title" -> "c.title";
            default -> "c.priority_score";
        };

        String direction = sortAsc ? "ASC" : "DESC";
        // Secondary sort by created_at DESC
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

        // 1. Overall counts by status & priority
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

        // 2. Critical count
        String critSql = "SELECT COUNT(*) FROM complaints c " + baseWhere + " AND (c.is_safety_critical = TRUE OR c.priority = 'CRITICAL')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(critSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setCriticalComplaints(rs.getInt(1));
            }
        }

        // 3. Category distribution
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

        // 4. Priority distribution
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

        // 5. Overdue complaints: unresolved complaints older than 72 hours
        String overdueSql = "SELECT COUNT(*) FROM complaints c " + baseWhere +
                            " AND c.status NOT IN ('RESOLVED', 'REJECTED') " +
                            " AND c.created_at < DATE_SUB(NOW(), INTERVAL 72 HOUR)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(overdueSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setOverdueComplaints(rs.getInt(1));
            }
        }

        // 6. Average resolution time in hours
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

        // Join columns
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
