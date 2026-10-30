package com.smartcampus.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtil BCrypt Hashing & Verification Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Hashing should produce valid non-empty BCrypt hash starting with $2a$")
    void testHashPassword() {
        String hash = PasswordUtil.hashPassword("SecureStudent@2026");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    @Test
    @DisplayName("Candidate matching password should verify successfully")
    void testVerifyMatchingPassword() {
        String raw = "AdminSecret#99";
        String hash = PasswordUtil.hashPassword(raw);
        assertTrue(PasswordUtil.verifyPassword(raw, hash));
    }

    @Test
    @DisplayName("Candidate non-matching password should fail verification")
    void testVerifyNonMatchingPassword() {
        String hash = PasswordUtil.hashPassword("CorrectPassword123");
        assertFalse(PasswordUtil.verifyPassword("WrongPassword123", hash));
        assertFalse(PasswordUtil.verifyPassword("correctpassword123", hash)); // case sensitivity
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Empty or blank passwords should throw IllegalArgumentException")
    void testBlankPasswordThrows(String blank) {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(blank));
    }

    @Test
    @DisplayName("Null candidate or hash should safely return false without crashing")
    void testNullInputsVerify() {
        assertFalse(PasswordUtil.verifyPassword((String) null, "$2a$12$somehash"));
        assertFalse(PasswordUtil.verifyPassword("password", null));
        assertFalse(PasswordUtil.verifyPassword("", ""));
    }
}
