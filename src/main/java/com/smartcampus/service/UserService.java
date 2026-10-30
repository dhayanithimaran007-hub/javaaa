package com.smartcampus.service;

import com.smartcampus.dao.AuditLogDAO;
import com.smartcampus.dao.UserDAO;
import com.smartcampus.model.AuditLog;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.PasswordUtil;
import com.smartcampus.security.UserSession;
import com.smartcampus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;

    public UserService() {
        this(new UserDAO(), new AuditLogDAO());
    }

    public UserService(UserDAO userDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public User authenticate(String email, String password) throws SQLException {
        ValidationUtil.requireNonBlank(email, "Email");
        ValidationUtil.requireNonBlank(password, "Password");

        Optional<User> optionalUser = userDAO.findByEmail(email.trim());
        if (optionalUser.isEmpty()) {
            logger.warn("Authentication failed: User not found for email '{}'", email);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = optionalUser.get();
        if (!user.isActive()) {
            logger.warn("Authentication blocked: Inactive account for '{}'", email);
            throw new IllegalStateException("Your account has been deactivated. Please contact the campus administrator.");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            logger.warn("Authentication failed: Password mismatch for email '{}'", email);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        logger.info("User authenticated successfully: {} [{}]", user.getEmail(), user.getRole());
        auditLogDAO.log(new AuditLog(user.getUserId(), "LOGIN", "USER", user.getUserId(), "User logged in successfully."));
        return user;
    }

    public User registerStudent(String fullName, String email, String password, String department, String phone) throws SQLException {
        String cleanName = ValidationUtil.requireNonBlank(fullName, "Full Name");
        String cleanEmail = ValidationUtil.requireNonBlank(email, "Email");

        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        if (!ValidationUtil.isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain both letters and numbers.");
        }

        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        User student = new User();
        student.setFullName(cleanName);
        student.setEmail(cleanEmail.toLowerCase().trim());
        student.setPasswordHash(PasswordUtil.hashPassword(password));
        // Enforce STUDENT role strictly for public self-registration
        student.setRole(Role.STUDENT);
        student.setDepartment(department != null ? department.trim() : "");
        student.setPhone(phone != null ? phone.trim() : "");
        student.setActive(true);

        User created = userDAO.create(student);
        logger.info("Student registered: {} (ID: {})", created.getEmail(), created.getUserId());
        auditLogDAO.log(new AuditLog(created.getUserId(), "REGISTER_STUDENT", "USER", created.getUserId(),
                "Student self-registration completed for " + created.getEmail()));
        return created;
    }

    public User createStaffAccount(String fullName, String email, String password, String department, String phone) throws SQLException {
        // Enforce administrative authorization
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();

        String cleanName = ValidationUtil.requireNonBlank(fullName, "Staff Name");
        String cleanEmail = ValidationUtil.requireNonBlank(email, "Staff Email");

        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        if (!ValidationUtil.isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain both letters and numbers.");
        }

        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        User staff = new User();
        staff.setFullName(cleanName);
        staff.setEmail(cleanEmail.toLowerCase().trim());
        staff.setPasswordHash(PasswordUtil.hashPassword(password));
        staff.setRole(Role.STAFF);
        staff.setDepartment(department != null ? department.trim() : "Maintenance");
        staff.setPhone(phone != null ? phone.trim() : "");
        staff.setActive(true);

        User created = userDAO.create(staff);
        logger.info("Staff account created by admin {}: {} (ID: {})", currentAdmin.getEmail(), created.getEmail(), created.getUserId());
        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), "CREATE_STAFF", "USER", created.getUserId(),
                "Admin created staff account for " + created.getEmail()));
        return created;
    }

    public void changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();

        // Only allow changing own password unless administrator
        if (currentUser.getUserId() != userId && currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Unauthorized: You can only change your own password.");
        }

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // If not admin override, verify current password
        if (currentUser.getRole() != Role.ADMIN || currentUser.getUserId() == userId) {
            if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
        }

        if (!ValidationUtil.isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 8 characters long and contain both letters and numbers.");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        userDAO.updatePassword(userId, newHash);
        logger.info("Password changed for user ID: {}", userId);
        auditLogDAO.log(new AuditLog(currentUser.getUserId(), "CHANGE_PASSWORD", "USER", userId, "Password updated successfully."));
    }

    public void updateProfile(int userId, String fullName, String department, String phone) throws SQLException {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();

        if (currentUser.getUserId() != userId && currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Unauthorized: You can only edit your own profile.");
        }

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setFullName(ValidationUtil.requireNonBlank(fullName, "Full Name"));
        user.setDepartment(department != null ? department.trim() : "");
        user.setPhone(phone != null ? phone.trim() : "");

        userDAO.update(user);
        if (currentUser.getUserId() == userId) {
            currentUser.setFullName(user.getFullName());
            currentUser.setDepartment(user.getDepartment());
            currentUser.setPhone(user.getPhone());
        }

        logger.info("Profile updated for user ID: {}", userId);
        auditLogDAO.log(new AuditLog(currentUser.getUserId(), "UPDATE_PROFILE", "USER", userId, "Profile details updated."));
    }

    public void toggleUserActive(int targetUserId, boolean active) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        User currentAdmin = UserSession.getCurrentUser();

        if (currentAdmin.getUserId() == targetUserId && !active) {
            throw new IllegalArgumentException("Administrators cannot deactivate their own account.");
        }

        userDAO.setActive(targetUserId, active);
        String actionStr = active ? "ACTIVATE_USER" : "DEACTIVATE_USER";
        logger.info("User ID {} active status set to {} by admin {}", targetUserId, active, currentAdmin.getEmail());
        auditLogDAO.log(new AuditLog(currentAdmin.getUserId(), actionStr, "USER", targetUserId,
                "User account " + (active ? "activated" : "deactivated")));
    }

    public List<User> listUsers(String roleFilter, Boolean activeFilter, String searchQuery) throws SQLException {
        UserSession.requireRole(Role.ADMIN);
        return userDAO.findAll(roleFilter, activeFilter, searchQuery);
    }

    public List<User> getActiveStaffList() throws SQLException {
        return userDAO.findActiveStaff();
    }
}

