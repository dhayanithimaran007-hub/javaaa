package com.smartcampus.security;

import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSession {
    private static final Logger logger = LoggerFactory.getLogger(UserSession.class);
    private static User currentUser = null;

    private UserSession() {}

    public static synchronized void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        currentUser = user;
        logger.info("User logged in: {} (ID: {}, Role: {})", user.getEmail(), user.getUserId(), user.getRole());
    }

    public static synchronized void logout() {
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getEmail());
            currentUser = null;
        }
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null && currentUser.isActive();
    }

    public static synchronized boolean hasRole(Role... roles) {
        if (!isLoggedIn() || roles == null) return false;
        for (Role r : roles) {
            if (currentUser.getRole() == r) return true;
        }
        return false;
    }

    public static synchronized void requireLoggedIn() {
        if (!isLoggedIn()) {
            throw new SecurityException("Authentication required. Please log in to continue.");
        }
    }

    public static synchronized void requireRole(Role... allowedRoles) {
        requireLoggedIn();
        if (!hasRole(allowedRoles)) {
            throw new SecurityException("Access denied. Required role: " + formatRoles(allowedRoles)
                    + ", but user has: " + currentUser.getRole());
        }
    }

    public static synchronized void checkStudentOwnership(int studentId) {
        requireLoggedIn();
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admins can view any student records
        }
        if (currentUser.getRole() == Role.STUDENT && currentUser.getUserId() != studentId) {
            throw new SecurityException("Access denied. You cannot access another student's complaints.");
        }
    }

    private static String formatRoles(Role[] roles) {
        if (roles == null) return "None";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(roles[i].name());
        }
        return sb.toString();
    }
}
