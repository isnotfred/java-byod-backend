package com.pup.byod.javabyodbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {}

    /**
     * Generate a secure random password.
     */
    public static String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

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