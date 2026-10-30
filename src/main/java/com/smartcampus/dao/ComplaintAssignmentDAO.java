package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.ComplaintAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComplaintAssignmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintAssignmentDAO.class);

    public ComplaintAssignment assign(int complaintId, int staffId, int assignedBy,
                                      String notes, Connection conn) throws SQLException {
        boolean isLocalConn = (conn == null);
        Connection actualConn = isLocalConn ? DBConnection.getConnection() : conn;

        try {
            // First deactivate previous active assignments for this complaint
            String deactivateSql = "UPDATE complaint_assignments SET is_active = FALSE WHERE complaint_id = ?";
            try (PreparedStatement ps = actualConn.prepareStatement(deactivateSql)) {
                ps.setInt(1, complaintId);
                ps.executeUpdate();
            }

            // Insert new assignment
            String insertSql = "INSERT INTO complaint_assignments (complaint_id, staff_id, assigned_by, notes, is_active) " +
                               "VALUES (?, ?, ?, ?, TRUE)";
            ComplaintAssignment assignment = new ComplaintAssignment();
            assignment.setComplaintId(complaintId);
            assignment.setStaffId(staffId);
            assignment.setAssignedBy(assignedBy);
            assignment.setNotes(notes);
            assignment.setActive(true);

            try (PreparedStatement ps = actualConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, complaintId);
                ps.setInt(2, staffId);
                ps.setInt(3, assignedBy);
                ps.setString(4, notes);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        assignment.setAssignmentId(rs.getInt(1));
                    }
                }
            }
            return assignment;
        } finally {
            if (isLocalConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public Optional<ComplaintAssignment> findActiveAssignment(int complaintId) throws SQLException {
        String sql = "SELECT ca.*, s.full_name as staff_name, s.department as staff_department, " +
                     "a.full_name as assigned_by_name " +
                     "FROM complaint_assignments ca " +
                     "JOIN users s ON ca.staff_id = s.user_id " +
                     "JOIN users a ON ca.assigned_by = a.user_id " +
                     "WHERE ca.complaint_id = ? AND ca.is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ComplaintAssignment ca = new ComplaintAssignment();
                    ca.setAssignmentId(rs.getInt("assignment_id"));
                    ca.setComplaintId(rs.getInt("complaint_id"));
                    ca.setStaffId(rs.getInt("staff_id"));
                    ca.setStaffName(rs.getString("staff_name"));
                    ca.setStaffDepartment(rs.getString("staff_department"));
                    ca.setAssignedBy(rs.getInt("assigned_by"));
                    ca.setAssignedByName(rs.getString("assigned_by_name"));
                    ca.setAssignedAt(rs.getTimestamp("assigned_at"));
                    ca.setNotes(rs.getString("notes"));
                    ca.setActive(rs.getBoolean("is_active"));
                    return Optional.of(ca);
                }
            }
        }
        return Optional.empty();
    }

    public List<ComplaintAssignment> findAssignmentsByStaff(int staffId) throws SQLException {
        String sql = "SELECT ca.*, s.full_name as staff_name, s.department as staff_department, " +
                     "a.full_name as assigned_by_name " +
                     "FROM complaint_assignments ca " +
                     "JOIN users s ON ca.staff_id = s.user_id " +
                     "JOIN users a ON ca.assigned_by = a.user_id " +
                     "WHERE ca.staff_id = ? ORDER BY ca.assigned_at DESC";

        List<ComplaintAssignment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComplaintAssignment ca = new ComplaintAssignment();
                    ca.setAssignmentId(rs.getInt("assignment_id"));
                    ca.setComplaintId(rs.getInt("complaint_id"));
                    ca.setStaffId(rs.getInt("staff_id"));
                    ca.setStaffName(rs.getString("staff_name"));
                    ca.setStaffDepartment(rs.getString("staff_department"));
                    ca.setAssignedBy(rs.getInt("assigned_by"));
                    ca.setAssignedByName(rs.getString("assigned_by_name"));
                    ca.setAssignedAt(rs.getTimestamp("assigned_at"));
                    ca.setNotes(rs.getString("notes"));
                    ca.setActive(rs.getBoolean("is_active"));
                    list.add(ca);
                }
            }
        }
        return list;
    }
}
