package com.smartcampus.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PasswordUtil {
    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {}

    /**
     * Hashes a plaintext password using BCrypt with cost factor 12.
     */
    public static String hashPassword(String plaintextPassword) {
        if (plaintextPassword == null || plaintextPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plaintextPassword.toCharArray());
    }

    /**
     * Hashes a char array password and zeroes out the input buffer for security.
     */
    public static String hashPassword(char[] passwordChars) {
        if (passwordChars == null || passwordChars.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        try {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, passwordChars);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    /**
     * Verifies a candidate password against an existing BCrypt hash.
     */
    public static boolean verifyPassword(String candidatePassword, String bcryptHash) {
        if (candidatePassword == null || bcryptHash == null || bcryptHash.isBlank()) {
            return false;
        }
        return verifyPassword(candidatePassword.toCharArray(), bcryptHash);
    }

    /**
     * Verifies a char array candidate password against an existing BCrypt hash.
     */
    public static boolean verifyPassword(char[] candidateChars, String bcryptHash) {
        if (candidateChars == null || candidateChars.length == 0 || bcryptHash == null || bcryptHash.isBlank()) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(candidateChars, bcryptHash);
            return result.verified;
        } catch (Exception e) {
            return false;
        } finally {
            Arrays.fill(candidateChars, '\0');
        }
    }
}
