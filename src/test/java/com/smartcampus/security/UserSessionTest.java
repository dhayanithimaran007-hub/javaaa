package com.smartcampus.security;

import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserSession Authentication Context & Authorization Tests")
class UserSessionTest {

    @BeforeEach
    @AfterEach
    void resetSession() {
        UserSession.logout();
    }

    @Test
    @DisplayName("Unauthenticated state should be correctly reflected")
    void testUnauthenticated() {
        assertFalse(UserSession.isLoggedIn());
        assertNull(UserSession.getCurrentUser());
        assertThrows(SecurityException.class, UserSession::requireLoggedIn);
    }

    @Test
    @DisplayName("Student login should establish session and enforce role checks")
    void testStudentLogin() {
        User student = new User(101, "Alice Sharma", "alice@student.campus.edu", "hash", Role.STUDENT, "CS", "123", true, null, null);
        UserSession.login(student);

        assertTrue(UserSession.isLoggedIn());
        assertEquals(student, UserSession.getCurrentUser());
        assertTrue(UserSession.hasRole(Role.STUDENT));
        assertFalse(UserSession.hasRole(Role.ADMIN));
        assertFalse(UserSession.hasRole(Role.STAFF));

        assertDoesNotThrow(() -> UserSession.requireRole(Role.STUDENT));
        assertThrows(SecurityException.class, () -> UserSession.requireRole(Role.ADMIN));
    }

    @Test
    @DisplayName("Student cannot access another student's complaint")
    void testStudentOwnershipRestriction() {
        User student = new User(101, "Alice Sharma", "alice@student.campus.edu", "hash", Role.STUDENT, "CS", "123", true, null, null);
        UserSession.login(student);

        // Own complaint (studentId = 101)
        assertDoesNotThrow(() -> UserSession.checkStudentOwnership(101));

        // Another student's complaint (studentId = 102) -> must throw SecurityException
        assertThrows(SecurityException.class, () -> UserSession.checkStudentOwnership(102));
    }

    @Test
    @DisplayName("Administrator can view any student's complaint")
    void testAdminOwnershipExemption() {
        User admin = new User(1, "Campus Admin", "admin@campus.edu", "hash", Role.ADMIN, "Admin", "123", true, null, null);
        UserSession.login(admin);

        assertDoesNotThrow(() -> UserSession.checkStudentOwnership(101));
        assertDoesNotThrow(() -> UserSession.checkStudentOwnership(102));
    }
}
