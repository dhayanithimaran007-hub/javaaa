package com.smartcampus.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtil Input Validation & CSV Sanitization Tests")
class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "student@campus.edu",
            "alice.sharma@department.campus.ac.in",
            "admin-office_123@sub.domain.org"
    })
    @DisplayName("Valid email formats should pass")
    void testValidEmails(String email) {
        assertTrue(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-an-email",
            "@missinguser.com",
            "user@.com",
            "user@domain",
            "user with spaces@campus.edu",
            ""
    })
    @DisplayName("Invalid email formats should fail")
    void testInvalidEmails(String email) {
        assertFalse(ValidationUtil.isValidEmail(email));
    }

    @Test
    @DisplayName("Password complexity tests")
    void testPasswordComplexity() {
        assertTrue(ValidationUtil.isStrongPassword("Student@123"));
        assertTrue(ValidationUtil.isStrongPassword("AdminPass99"));

        // Too short (< 8 chars)
        assertFalse(ValidationUtil.isStrongPassword("Pass1"));
        // No digits
        assertFalse(ValidationUtil.isStrongPassword("OnlyLettersPassword"));
        // No letters
        assertFalse(ValidationUtil.isStrongPassword("1234567890"));
    }

    @Test
    @DisplayName("CSV Formula Injection Escaping: Cells starting with =, +, -, @ must be neutralized with a leading single quote")
    void testCsvFormulaEscaping() {
        // Attack payload starting with =cmd
        String escapedFormula = ValidationUtil.escapeCsvCell("=cmd|'/C calc'!A0");
        assertTrue(escapedFormula.startsWith("'="), "Formula should be neutralized with leading quote: " + escapedFormula);

        // Payload with @SUM
        String escapedAt = ValidationUtil.escapeCsvCell("@SUM(1,2)");
        assertTrue(escapedAt.contains("'@SUM"), "Escaped cell should contain neutralized formula: " + escapedAt);

        // Payload with + or -
        String escapedPlus = ValidationUtil.escapeCsvCell("+12345");
        assertTrue(escapedPlus.startsWith("'+"));

        // Regular safe text
        String safeText = ValidationUtil.escapeCsvCell("Standard Campus Title");
        assertEquals("Standard Campus Title", safeText);
    }

    @Test
    @DisplayName("CSV Quote and Comma Escaping (RFC 4180)")
    void testCsvRFC4180Escaping() {
        // String with comma
        String withComma = ValidationUtil.escapeCsvCell("Science Block A, Room 101");
        assertEquals("\"Science Block A, Room 101\"", withComma);

        // String with double quotes
        String withQuote = ValidationUtil.escapeCsvCell("Lab \"Super\" PC");
        assertEquals("\"Lab \"\"Super\"\" PC\"", withQuote);
    }
}
