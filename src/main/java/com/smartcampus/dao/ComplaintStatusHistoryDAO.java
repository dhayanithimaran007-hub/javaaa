package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.enums.ComplaintStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ComplaintStatusHistoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintStatusHistoryDAO.class);

    public ComplaintStatusHistory record(ComplaintStatusHistory history, Connection conn) throws SQLException {
        String sql = "INSERT INTO complaint_status_history (complaint_id, changed_by, old_status, new_status, remarks) " +
                     "VALUES (?, ?, ?, ?, ?)";
        boolean isLocalConn = (conn == null);
        Connection actualConn = isLocalConn ? DBConnection.getConnection() : conn;

        try (PreparedStatement ps = actualConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getComplaintId());
            ps.setInt(2, history.getChangedBy());
            ps.setString(3, history.getOldStatus() != null ? history.getOldStatus().name() : null);
            ps.setString(4, history.getNewStatus().name());
            ps.setString(5, history.getRemarks());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    history.setHistoryId(rs.getInt(1));
                }
            }
            return history;
        } finally {
            if (isLocalConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public List<ComplaintStatusHistory> findByComplaintId(int complaintId) throws SQLException {
        String sql = "SELECT h.*, u.full_name as changed_by_name, u.role as changed_by_role " +
                     "FROM complaint_status_history h " +
                     "JOIN users u ON h.changed_by = u.user_id " +
                     "WHERE h.complaint_id = ? ORDER BY h.history_id ASC";

        List<ComplaintStatusHistory> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComplaintStatusHistory h = new ComplaintStatusHistory();
                    h.setHistoryId(rs.getInt("history_id"));
                    h.setComplaintId(rs.getInt("complaint_id"));
                    h.setChangedBy(rs.getInt("changed_by"));
                    h.setChangedByName(rs.getString("changed_by_name"));
                    h.setChangedByRole(rs.getString("changed_by_role"));
                    String oldStr = rs.getString("old_status");
                    h.setOldStatus(oldStr != null ? ComplaintStatus.fromString(oldStr) : null);
                    h.setNewStatus(ComplaintStatus.fromString(rs.getString("new_status")));
                    h.setRemarks(rs.getString("remarks"));
                    h.setChangedAt(rs.getTimestamp("changed_at"));
                    list.add(h);
                }
            }
        }
        return list;
    }
}
