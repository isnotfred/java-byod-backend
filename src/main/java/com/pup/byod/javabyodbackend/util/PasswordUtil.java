package com.pup.byod.javabyodbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {}

    /**
     * Hash a plain-text password with BCrypt.
     * Store the returned hash in the DB — never store the plain-text.
     */
    public static String hash(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        return ENCODER.encode(plainText);
    }

    /**
     * Verify a plain-text password against a BCrypt hash.
     * Returns true if they match, false otherwise.
     */
    public static boolean verify(String plainText, String hashedPassword) {
        if (plainText == null || hashedPassword == null) {
            return false;
        }
        return ENCODER.matches(plainText, hashedPassword);
    }
}