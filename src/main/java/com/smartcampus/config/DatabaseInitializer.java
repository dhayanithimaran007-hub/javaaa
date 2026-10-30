package com.smartcampus.config;

import com.smartcampus.dao.ComplaintAssignmentDAO;
import com.smartcampus.dao.ComplaintDAO;
import com.smartcampus.dao.ComplaintStatusHistoryDAO;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.Complaint;
import com.smartcampus.model.ComplaintStatusHistory;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.PasswordUtil;
import com.smartcampus.service.PriorityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initializeDatabase() {
        ensureDatabaseExists();
        try (Connection conn = DBConnection.getConnection()) {
            logger.info("Checking database schema initialization...");
            executeSchemaScript(conn);
            seedDefaultAccountsAndData(conn);
            logger.info("Database schema and seed verification complete.");
        } catch (SQLException e) {
            logger.warn("Database initialization deferred (MySQL connection issue): {}", e.getMessage());
        }
    }

    private static void ensureDatabaseExists() {
        AppConfig.ensureInitialized();
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        String password = AppConfig.getDbPassword();
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            return;
        }
        if (password == null || password.isBlank() || "your_mysql_password_here".equals(password.trim())) {
            logger.warn("Skipping automatic database existence check: Database password is not configured.");
            return;
        }

        try {
            int slashIndex = url.indexOf('/', "jdbc:mysql://".length());
            if (slashIndex == -1) return;
            int qIndex = url.indexOf('?', slashIndex);
            String dbName = (qIndex != -1) ? url.substring(slashIndex + 1, qIndex) : url.substring(slashIndex + 1);
            if (dbName.isBlank()) return;

            String queryParams = (qIndex != -1) ? url.substring(qIndex) : "";
            String serverUrl = url.substring(0, slashIndex + 1) + queryParams;

            DriverManager.setLoginTimeout(3);
            try (Connection serverConn = java.sql.DriverManager.getConnection(serverUrl, user, password);
                 Statement stmt = serverConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                logger.info("Database '{}' verified / ensured on MySQL server.", dbName);
            }
        } catch (SQLException e) {
            logger.debug("Database existence check note: {}", e.getMessage());
        }
    }

    private static void executeSchemaScript(Connection conn) {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream("db/schema.sql")) {
            if (in == null) {
                logger.warn("schema.sql not found on classpath.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 Statement stmt = conn.createStatement()) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String sql = sb.toString().trim();
                        // Strip trailing semicolon for execution
                        sql = sql.substring(0, sql.length() - 1);
                        if (!sql.toLowerCase().startsWith("create database") && !sql.toLowerCase().startsWith("use ")) {
                            try {
                                stmt.execute(sql);
                            } catch (SQLException ex) {
                                // Table may already exist
                                logger.debug("Schema statement notice: {}", ex.getMessage());
                            }
                        }
                        sb.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error executing schema script: {}", e.getMessage(), e);
        }
    }

    private static void seedDefaultAccountsAndData(Connection conn) {
        UserDAO userDAO = new UserDAO();
        ComplaintDAO complaintDAO = new ComplaintDAO();
        ComplaintAssignmentDAO assignmentDAO = new ComplaintAssignmentDAO();
        ComplaintStatusHistoryDAO historyDAO = new ComplaintStatusHistoryDAO();
        PriorityService priorityService = new PriorityService();

        try {
            // Check if users exist
            boolean hasUsers = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    hasUsers = true;
                }
            }

            if (!hasUsers) {
                logger.info("Seeding initial demo accounts...");

                User admin = new User(0, "Campus Administrator", "admin@campus.edu",
                        PasswordUtil.hashPassword("Admin@123"), Role.ADMIN, "Administration", "9876543210", true, null, null);
                userDAO.create(admin);

                User staff1 = new User(0, "Rajesh Kumar (Electrician)", "electrician@campus.edu",
                        PasswordUtil.hashPassword("Staff@123"), Role.STAFF, "Electrical Maintenance", "9876543211", true, null, null);
                userDAO.create(staff1);

                User staff2 = new User(0, "Suresh Patel (Plumber)", "plumber@campus.edu",
                        PasswordUtil.hashPassword("Staff@123"), Role.STAFF, "Plumbing & Sanitation", "9876543212", true, null, null);
                userDAO.create(staff2);

                User student1 = new User(0, "Alice Sharma", "alice@student.campus.edu",
                        PasswordUtil.hashPassword("Student@123"), Role.STUDENT, "Computer Science", "9876543213", true, null, null);
                userDAO.create(student1);

                User student2 = new User(0, "Bob Verghese", "bob@student.campus.edu",
                        PasswordUtil.hashPassword("Student@123"), Role.STUDENT, "Mechanical Engineering", "9876543214", true, null, null);
                userDAO.create(student2);

                logger.info("Default users seeded: admin@campus.edu, electrician@campus.edu, alice@student.campus.edu");

                // Seed initial complaints for demonstration
                seedSampleComplaints(conn, userDAO, complaintDAO, assignmentDAO, historyDAO, priorityService);
            }
        } catch (SQLException e) {
            logger.warn("Seeding notice: {}", e.getMessage());
        }
    }

    private static void seedSampleComplaints(Connection conn, UserDAO userDAO, ComplaintDAO complaintDAO,
                                            ComplaintAssignmentDAO assignmentDAO, ComplaintStatusHistoryDAO historyDAO,
                                            PriorityService priorityService) throws SQLException {

        Optional<User> studentOpt = userDAO.findByEmail("alice@student.campus.edu");
        Optional<User> adminOpt = userDAO.findByEmail("admin@campus.edu");
        Optional<User> staffOpt = userDAO.findByEmail("electrician@campus.edu");

        if (studentOpt.isEmpty() || adminOpt.isEmpty() || staffOpt.isEmpty()) return;

        User student = studentOpt.get();
        User admin = adminOpt.get();
        User staff = staffOpt.get();

        // 1. Critical safety complaint
        Complaint c1 = new Complaint();
        c1.setStudentId(student.getUserId());
        c1.setTitle("Exposed High-Voltage Wiring in Physics Lab");
        c1.setDescription("Sparking wires hanging near workstation 4 in Physics Lab 204. High risk of electrical shock.");
        c1.setCategory("Electrical");
        c1.setBuilding("Science Block A");
        c1.setRoomNumber("Lab 204");
        c1.setSeverity(5);
        c1.setSafetyRisk(5);
        c1.setPeopleAffected(45);
        c1.setOperationalImpact(4);

        PriorityService.PriorityResult pr1 = priorityService.calculatePriority(5, 5, 45, 4, c1.getCategory());
        c1.setPriorityScore(pr1.score());
        c1.setPriority(pr1.priorityLevel());
        c1.setSafetyCritical(pr1.isSafetyCritical());
        c1.setStatus(ComplaintStatus.ACKNOWLEDGED);

        complaintDAO.create(c1, conn);
        assignmentDAO.assign(c1.getComplaintId(), staff.getUserId(), admin.getUserId(), "Urgent safety hazard - inspect immediately", conn);
        historyDAO.record(new ComplaintStatusHistory(c1.getComplaintId(), student.getUserId(), null, ComplaintStatus.SUBMITTED, "Initial submission"), conn);
        historyDAO.record(new ComplaintStatusHistory(c1.getComplaintId(), admin.getUserId(), ComplaintStatus.SUBMITTED, ComplaintStatus.ACKNOWLEDGED, "Assigned to electrician"), conn);

        // 2. High priority complaint
        Complaint c2 = new Complaint();
        c2.setStudentId(student.getUserId());
        c2.setTitle("Main Campus Wi-Fi Router Down in Library");
        c2.setDescription("The central Wi-Fi access point in the 2nd-floor study hall has failed. Over 150 students cannot access digital resources.");
        c2.setCategory("Internet and Wi-Fi");
        c2.setBuilding("Central Library");
        c2.setRoomNumber("2nd Floor Hall");
        c2.setSeverity(4);
        c2.setSafetyRisk(0);
        c2.setPeopleAffected(150);
        c2.setOperationalImpact(5);

        PriorityService.PriorityResult pr2 = priorityService.calculatePriority(4, 0, 150, 5, c2.getCategory());
        c2.setPriorityScore(pr2.score());
        c2.setPriority(pr2.priorityLevel());
        c2.setSafetyCritical(pr2.isSafetyCritical());
        c2.setStatus(ComplaintStatus.IN_PROGRESS);

        complaintDAO.create(c2, conn);
        historyDAO.record(new ComplaintStatusHistory(c2.getComplaintId(), student.getUserId(), null, ComplaintStatus.SUBMITTED, "Initial submission"), conn);
        historyDAO.record(new ComplaintStatusHistory(c2.getComplaintId(), admin.getUserId(), ComplaintStatus.SUBMITTED, ComplaintStatus.IN_PROGRESS, "Network technician dispatched"), conn);

        // 3. Resolved complaint
        Complaint c3 = new Complaint();
        c3.setStudentId(student.getUserId());
        c3.setTitle("Water Leakage in Restroom Basin");
        c3.setDescription("Water valve under sink #2 in Ground Floor Restroom is dripping constantly.");
        c3.setCategory("Plumbing and Water Supply");
        c3.setBuilding("Main Academic Block");
        c3.setRoomNumber("GF-Washroom-B");
        c3.setSeverity(2);
        c3.setSafetyRisk(1);
        c3.setPeopleAffected(15);
        c3.setOperationalImpact(2);

        PriorityService.PriorityResult pr3 = priorityService.calculatePriority(2, 1, 15, 2, c3.getCategory());
        c3.setPriorityScore(pr3.score());
        c3.setPriority(pr3.priorityLevel());
        c3.setSafetyCritical(pr3.isSafetyCritical());
        c3.setStatus(ComplaintStatus.RESOLVED);

        complaintDAO.create(c3, conn);
        complaintDAO.updateStatusAndResolvedAt(c3.getComplaintId(), ComplaintStatus.RESOLVED, new java.sql.Timestamp(System.currentTimeMillis()), conn);
        historyDAO.record(new ComplaintStatusHistory(c3.getComplaintId(), student.getUserId(), null, ComplaintStatus.SUBMITTED, "Initial submission"), conn);
        historyDAO.record(new ComplaintStatusHistory(c3.getComplaintId(), staff.getUserId(), ComplaintStatus.SUBMITTED, ComplaintStatus.RESOLVED, "Replaced Teflon tape and washers. Fixed."), conn);

        logger.info("Sample complaints seeded successfully.");
    }
}
