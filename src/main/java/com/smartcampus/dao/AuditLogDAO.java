package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogDAO.class);

    public void log(AuditLog auditLog) {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auditLog.getActorUserId());
            ps.setString(2, auditLog.getAction());
            ps.setString(3, auditLog.getEntityType());
            if (auditLog.getEntityId() != null) {
                ps.setInt(4, auditLog.getEntityId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, auditLog.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    public List<AuditLog> findAll(String actionFilter, String entityTypeFilter, String searchQuery, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT a.audit_id, a.actor_user_id, u.full_name as actor_name, u.role as actor_role, " +
                "a.action, a.entity_type, a.entity_id, a.details, a.created_at " +
                "FROM audit_logs a " +
                "LEFT JOIN users u ON a.actor_user_id = u.user_id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (actionFilter != null && !actionFilter.isBlank() && !actionFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND a.action = ?");
            params.add(actionFilter);
        }
        if (entityTypeFilter != null && !entityTypeFilter.isBlank() && !entityTypeFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND a.entity_type = ?");
            params.add(entityTypeFilter);
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (LOWER(a.details) LIKE ? OR LOWER(u.full_name) LIKE ? OR LOWER(a.action) LIKE ?)");
            String q = "%" + searchQuery.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append(" ORDER BY a.audit_id DESC LIMIT ?");
        params.add(limit > 0 ? limit : 200);

        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setAuditId(rs.getInt("audit_id"));
                    log.setActorUserId(rs.getInt("actor_user_id"));
                    log.setActorName(rs.getString("actor_name"));
                    log.setActorRole(rs.getString("actor_role"));
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    int entityId = rs.getInt("entity_id");
                    if (!rs.wasNull()) {
                        log.setEntityId(entityId);
                    }
                    log.setDetails(rs.getString("details"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }
}
