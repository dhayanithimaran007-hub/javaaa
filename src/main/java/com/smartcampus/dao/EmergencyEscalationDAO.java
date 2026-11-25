package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.ComplaintEscalation;
import com.smartcampus.model.enums.SlaStatus;

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

public class EmergencyEscalationDAO {

    public boolean insertEscalationIfAbsent(ComplaintEscalation esc, Connection conn) throws SQLException {
        boolean localConn = (conn == null);
        Connection actualConn = localConn ? DBConnection.getConnection() : conn;
        try {
            String checkSql = "SELECT id FROM complaint_escalations WHERE complaint_id = ? AND escalation_level = ?";
            try (PreparedStatement checkPs = actualConn.prepareStatement(checkSql)) {
                checkPs.setInt(1, esc.getComplaintId());
                checkPs.setInt(2, esc.getEscalationLevel());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        esc.setId(rs.getInt("id"));
                        return false;
                    }
                }
            }

            String insertSql = "INSERT IGNORE INTO complaint_escalations " +
                    "(complaint_id, escalation_level, previous_level, sla_status, escalation_reason, breach_at, " +
                    "acknowledged_at, acknowledged_by, intervention_note) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = actualConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, esc.getComplaintId());
                ps.setInt(2, esc.getEscalationLevel());
                ps.setInt(3, esc.getPreviousLevel());
                ps.setString(4, esc.getSlaStatus() != null ? esc.getSlaStatus().name() : SlaStatus.ON_TRACK.name());
                ps.setString(5, esc.getEscalationReason());
                ps.setTimestamp(6, esc.getBreachAt());
                ps.setTimestamp(7, esc.getAcknowledgedAt());
                if (esc.getAcknowledgedBy() != null) {
                    ps.setInt(8, esc.getAcknowledgedBy());
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }
                ps.setString(9, esc.getInterventionNote());

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            esc.setId(keys.getInt(1));
                        }
                    }
                    return true;
                }
                return false;
            }
        } finally {
            if (localConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public void acknowledgeEscalation(int complaintId, int adminUserId, String note, Connection conn) throws SQLException {
        boolean localConn = (conn == null);
        Connection actualConn = localConn ? DBConnection.getConnection() : conn;
        try {
            String updateSql = "UPDATE complaint_escalations " +
                    "SET acknowledged_at = COALESCE(acknowledged_at, CURRENT_TIMESTAMP), " +
                    "    acknowledged_by = COALESCE(acknowledged_by, ?), " +
                    "    intervention_note = CASE WHEN ? IS NOT NULL AND ? <> '' THEN ? ELSE intervention_note END " +
                    "WHERE complaint_id = ?";
            String trimmedNote = note != null ? note.trim() : "";
            int updated;
            try (PreparedStatement ps = actualConn.prepareStatement(updateSql)) {
                ps.setInt(1, adminUserId);
                ps.setString(2, trimmedNote);
                ps.setString(3, trimmedNote);
                ps.setString(4, trimmedNote);
                ps.setInt(5, complaintId);
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                ComplaintEscalation ackEntry = new ComplaintEscalation();
                ackEntry.setComplaintId(complaintId);
                ackEntry.setEscalationLevel(0);
                ackEntry.setPreviousLevel(0);
                ackEntry.setSlaStatus(SlaStatus.ON_TRACK);
                ackEntry.setEscalationReason("Safety-critical alert acknowledged by administrator.");
                ackEntry.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
                ackEntry.setAcknowledgedBy(adminUserId);
                ackEntry.setInterventionNote(trimmedNote.isEmpty() ? "Alert acknowledged." : trimmedNote);
                insertEscalationIfAbsent(ackEntry, actualConn);
            }
        } finally {
            if (localConn && actualConn != null) {
                actualConn.close();
            }
        }
    }

    public List<ComplaintEscalation> findByComplaintId(int complaintId) throws SQLException {
        String sql = "SELECT e.*, u.full_name AS ack_user_name " +
                "FROM complaint_escalations e " +
                "LEFT JOIN users u ON e.acknowledged_by = u.user_id " +
                "WHERE e.complaint_id = ? " +
                "ORDER BY e.escalation_level ASC, e.created_at ASC";
        List<ComplaintEscalation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Map<Integer, ComplaintEscalation> findLatestByComplaintMap() throws SQLException {
        String sql = "SELECT e.*, u.full_name AS ack_user_name " +
                "FROM complaint_escalations e " +
                "LEFT JOIN users u ON e.acknowledged_by = u.user_id " +
                "ORDER BY e.complaint_id ASC, e.escalation_level ASC";
        Map<Integer, ComplaintEscalation> map = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ComplaintEscalation esc = mapRow(rs);
                map.put(esc.getComplaintId(), esc);
            }
        }
        return map;
    }

    public double[] getEmergencyResponseStats() throws SQLException {
        String sql = "SELECT " +
                "  COUNT(*) AS total_emergencies, " +
                "  SUM(CASE WHEN c.status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_emergencies, " +
                "  AVG(CASE " +
                "        WHEN first_ack.first_assigned IS NOT NULL " +
                "          THEN TIMESTAMPDIFF(MINUTE, c.created_at, first_ack.first_assigned) / 60.0 " +
                "        WHEN c.resolved_at IS NOT NULL " +
                "          THEN TIMESTAMPDIFF(MINUTE, c.created_at, c.resolved_at) / 60.0 " +
                "        ELSE NULL " +
                "      END) AS avg_response_hours " +
                "FROM complaints c " +
                "LEFT JOIN (" +
                "  SELECT complaint_id, MIN(assigned_at) AS first_assigned " +
                "  FROM complaint_assignments GROUP BY complaint_id" +
                ") first_ack ON c.complaint_id = first_ack.complaint_id " +
                "WHERE c.is_safety_critical = TRUE OR c.priority = 'CRITICAL'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int total = rs.getInt("total_emergencies");
                int resolved = rs.getInt("resolved_emergencies");
                double avgHrs = rs.getDouble("avg_response_hours");
                if (rs.wasNull()) avgHrs = 0.0;
                double rate = total > 0 ? ((double) resolved / total) * 100.0 : 0.0;
                return new double[]{avgHrs, rate};
            }
        }
        return new double[]{0.0, 0.0};
    }

    private ComplaintEscalation mapRow(ResultSet rs) throws SQLException {
        ComplaintEscalation e = new ComplaintEscalation();
        e.setId(rs.getInt("id"));
        e.setComplaintId(rs.getInt("complaint_id"));
        e.setEscalationLevel(rs.getInt("escalation_level"));
        e.setPreviousLevel(rs.getInt("previous_level"));
        e.setSlaStatus(SlaStatus.fromString(rs.getString("sla_status")));
        e.setEscalationReason(rs.getString("escalation_reason"));
        e.setBreachAt(rs.getTimestamp("breach_at"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        e.setAcknowledgedAt(rs.getTimestamp("acknowledged_at"));
        int ackBy = rs.getInt("acknowledged_by");
        if (!rs.wasNull()) {
            e.setAcknowledgedBy(ackBy);
        }
        e.setAcknowledgedByName(rs.getString("ack_user_name"));
        e.setInterventionNote(rs.getString("intervention_note"));
        return e;
    }
}
