package com.smartcampus.service;

import com.smartcampus.config.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DatabaseHealthService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "users",
            "complaints",
            "complaint_assignments",
            "complaint_status_history",
            "complaint_feedback",
            "audit_logs",
            "complaint_escalations",
            "predictive_analysis",
            "maintenance_risk_assessments",
            "preventive_maintenance_recommendations"
    );

    public record HealthReport(
            boolean connected,
            long latencyMs,
            String databaseName,
            int tableCount,
            boolean schemaReady,
            String message
    ) {}

    public HealthReport checkHealth() {
        long start = System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            long latency = System.currentTimeMillis() - start;
            String catalog = conn.getCatalog();

            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> existingTables = new HashSet<>();
            try (ResultSet tablesRs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tablesRs.next()) {
                    existingTables.add(tablesRs.getString("TABLE_NAME").toLowerCase());
                }
            }

            boolean schemaReady = existingTables.containsAll(REQUIRED_TABLES);
            String message = schemaReady
                    ? "Database is healthy and schema is verified (" + existingTables.size() + " tables)."
                    : "Database connected, but schema is missing or incomplete (" + existingTables.size() + " tables found).";

            return new HealthReport(true, latency, catalog, existingTables.size(), schemaReady, message);
        } catch (SQLException e) {
            long latency = System.currentTimeMillis() - start;
            logger.warn("Health check failed: {}", e.getMessage());
            return new HealthReport(false, latency, "unknown", 0, false, e.getMessage());
        }
    }

    public java.util.Map<String, Integer> getTableRecordCounts() {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String table : REQUIRED_TABLES) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        counts.put(table, rs.getInt(1));
                    }
                } catch (SQLException e) {
                    counts.put(table, -1);
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to get table record counts: {}", e.getMessage());
        }
        return counts;
    }
}
