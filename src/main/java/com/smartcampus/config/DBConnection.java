package com.smartcampus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("MySQL Connector/J driver registered successfully.");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL Driver not found on classpath!", e);
        }
    }

    private DBConnection() {}

    /**
     * Validates database connection parameters before attempting a JDBC connection.
     * Prevents silent fallbacks to empty passwords and produces actionable error messages.
     */
    public static void validateConfiguration(String url, String user, String password) throws SQLException {
        if (url == null || url.isBlank()) {
            throw new SQLException("Database connection URL is not configured. Please specify 'db.url' in 'config/application.properties'.");
        }
        if (user == null || user.isBlank()) {
            throw new SQLException("Database username is not configured. Please specify 'db.user' in 'config/application.properties'.");
        }
        if (password == null || password.isBlank() || "your_mysql_password_here".equals(password.trim())) {
            throw new SQLException("Database password is not configured. Please specify 'db.password' in 'config/application.properties' or set the DB_PASSWORD environment variable.");
        }
    }

    /**
     * Obtains a new JDBC connection using currently configured credentials.
     */
    public static Connection getConnection() throws SQLException {
        AppConfig.ensureInitialized();
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        String password = AppConfig.getDbPassword();

        validateConfiguration(url, user, password);

        try {
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // Mask password from log
            logger.error("Failed to establish database connection to {} as user '{}': {}",
                    maskUrl(url), user, e.getMessage());
            throw new SQLException("Unable to connect to MySQL database (" + maskUrl(url) + "). " +
                    "Please ensure MySQL Server is running and credentials are valid. Details: " + e.getMessage(), e);
        }
    }

    /**
     * Executes 'SELECT 1' to verify connection health.
     * @return true if connection succeeds, false otherwise.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            logger.warn("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Tests a specific set of connection parameters without saving them.
     */
    public static boolean testSpecificConnection(String url, String user, String password) throws SQLException {
        validateConfiguration(url, user, password);
        DriverManager.setLoginTimeout(5);
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        }
    }

    public static String maskUrl(String url) {
        if (url == null) return "null";
        // Remove password parameter if present in query string or authority
        return url.replaceAll("(?i)password=[^&]*", "password=***")
                  .replaceAll("://([^:]+):([^@]+)@", "://$1:***@");
    }

    /**
     * CLI utility to verify database connectivity from the terminal without starting the GUI.
     */
    public static void main(String[] args) {
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        System.out.println("=======================================================================");
        System.out.println("Smart Campus - MySQL Database Connectivity Test");
        System.out.println("=======================================================================");
        System.out.println("Target URL : " + maskUrl(url));
        System.out.println("User       : " + user);
        System.out.println("Testing connection...");

        try (Connection conn = getConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL database: " + conn.getCatalog());
            DatabaseInitializer.initializeDatabase();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    System.out.println("[SUCCESS] Database tables verified. Registered users count: " + rs.getInt(1));
                }
            }
            System.out.println("Database is ready for Smart Campus Application.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Connection failed: " + e.getMessage());
            System.err.println("\nTroubleshooting tips:");
            System.err.println("1. Verify MySQL Server is running on localhost:3306.");
            System.err.println("2. Check credentials in 'config/application.properties' or set env var DB_PASSWORD.");
            System.err.println("3. Ensure database schema is imported: mysql -u root -p < database/schema.sql");
            System.exit(1);
        }
    }
}
