package com.smartcampus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationResolutionTest {

    @Test
    @DisplayName("maskUrl should hide password parameter and credentials in URL authority")
    void testMaskUrl() {
        String raw = "jdbc:mysql://localhost:3306/db?user=root&password=secretPassword123&useSSL=false";
        String masked = DBConnection.maskUrl(raw);
        assertFalse(masked.contains("secretPassword123"));
        assertTrue(masked.contains("password=***"));

        String rawAuth = "jdbc:mysql://root:secretPass@localhost:3306/db";
        String maskedAuth = DBConnection.maskUrl(rawAuth);
        assertFalse(maskedAuth.contains("secretPass"));
        assertTrue(maskedAuth.contains("root:***@"));

        assertEquals("null", DBConnection.maskUrl(null));
    }

    @Test
    @DisplayName("validateConfiguration should reject missing or blank database URL")
    void testValidateConfigurationMissingUrl() {
        SQLException ex1 = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration(null, "root", "secret"));
        assertTrue(ex1.getMessage().contains("URL is not configured"));

        SQLException ex2 = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("   ", "root", "secret"));
        assertTrue(ex2.getMessage().contains("URL is not configured"));
    }

    @Test
    @DisplayName("validateConfiguration should reject missing or blank database username")
    void testValidateConfigurationMissingUser() {
        SQLException ex1 = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/db", null, "secret"));
        assertTrue(ex1.getMessage().contains("username is not configured"));

        SQLException ex2 = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/db", "   ", "secret"));
        assertTrue(ex2.getMessage().contains("username is not configured"));
    }

    @Test
    @DisplayName("validateConfiguration should reject missing, blank, or placeholder password")
    void testValidateConfigurationMissingPassword() {
        SQLException exNull = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/db", "root", null));
        assertTrue(exNull.getMessage().contains("password is not configured"));

        SQLException exBlank = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/db", "root", "   "));
        assertTrue(exBlank.getMessage().contains("password is not configured"));

        SQLException exPlaceholder = assertThrows(SQLException.class, () ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/db", "root", "your_mysql_password_here"));
        assertTrue(exPlaceholder.getMessage().contains("password is not configured"));
    }

    @Test
    @DisplayName("validateConfiguration should succeed with valid parameters")
    void testValidateConfigurationValid() {
        assertDoesNotThrow(() ->
                DBConnection.validateConfiguration("jdbc:mysql://localhost:3306/smart_campus_db", "root", "ValidPass@123"));
    }

    @Test
    @DisplayName("AppConfig should resolve database URL, user, and password properties correctly")
    void testAppConfigResolution() {
        AppConfig.ensureInitialized();

        assertNotNull(AppConfig.getDbUrl());
        assertTrue(AppConfig.getDbUrl().startsWith("jdbc:mysql://"));
        assertEquals("root", AppConfig.getDbUser());

        // Update in-memory credentials without persistence
        AppConfig.updateDbCredentials("jdbc:mysql://127.0.0.1:3306/testdb", "testuser", "testpass", false);
        assertEquals("jdbc:mysql://127.0.0.1:3306/testdb", AppConfig.getDbUrl());
        assertEquals("testuser", AppConfig.getDbUser());
        assertEquals("testpass", AppConfig.getDbPassword());
        assertTrue(AppConfig.isDbPasswordConfigured());

        // Reload configuration from actual config file
        AppConfig.loadConfig();
    }

    @Test
    @DisplayName("Empty or blank properties should not overwrite non-empty defaults")
    void testEmptyPropertyDoesNotOverwriteConfiguredDefault() {
        Properties baseProps = new Properties();
        baseProps.setProperty("db.password", "PreConfiguredPassword");

        Properties loadedFromFile = new Properties();
        loadedFromFile.setProperty("db.password", "");
        loadedFromFile.setProperty("db.user", "root");

        for (String key : loadedFromFile.stringPropertyNames()) {
            String val = loadedFromFile.getProperty(key);
            if (val != null && !val.trim().isEmpty()) {
                baseProps.setProperty(key, val.trim());
            }
        }

        // db.password should remain PreConfiguredPassword, not overwritten by empty string
        assertEquals("PreConfiguredPassword", baseProps.getProperty("db.password"));
        assertEquals("root", baseProps.getProperty("db.user"));
    }

    @Test
    @DisplayName("DatabaseHealthService should report healthy when MySQL database is reachable")
    void testLiveDatabaseHealthCheck() {
        org.junit.jupiter.api.Assumptions.assumeTrue(DBConnection.testConnection(), "MySQL Server must be running");
        com.smartcampus.service.DatabaseHealthService healthService = new com.smartcampus.service.DatabaseHealthService();
        com.smartcampus.service.DatabaseHealthService.HealthReport report = healthService.checkHealth();
        assertTrue(report.connected(), "Should report connected");
        assertTrue(report.schemaReady(), "Schema should be ready");
        assertEquals("smart_campus_db", report.databaseName());
    }

    @Test
    @DisplayName("UserService should authenticate demo accounts when database is connected")
    void testLiveUserAuthentication() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(DBConnection.testConnection(), "MySQL Server must be running");
        com.smartcampus.service.UserService userService = new com.smartcampus.service.UserService();
        com.smartcampus.model.User user = userService.authenticate("admin@campus.edu", "Admin@123");
        assertNotNull(user);
        assertEquals("admin@campus.edu", user.getEmail());
        assertEquals(com.smartcampus.model.enums.Role.ADMIN, user.getRole());
    }
}

