package com.pup.byod.javabyodbackend.util;

import com.pup.byod.javabyodbackend.exception.BusinessRuleException;

public final class ValidationUtil {

    private ValidationUtil() {}

    // ── Required ────────────────────────────────────────────────────

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " is required.");
        }
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessRuleException(fieldName + " is required.");
        }
    }

    // ── Length ──────────────────────────────────────────────────────

    public static void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new BusinessRuleException(fieldName + " must not exceed " + max + " characters.");
        }
    }

    public static void requireMinLength(String value, int min, String fieldName) {
        if (value == null || value.length() < min) {
            throw new BusinessRuleException(fieldName + " must be at least " + min + " characters.");
        }
    }

    public static void requireLengthBetween(String value, int min, int max, String fieldName) {
        requireMinLength(value, min, fieldName);
        requireMaxLength(value, max, fieldName);
    }

    // ── Format ──────────────────────────────────────────────────────

    /**
     * Validates a student ID: alphanumeric, hyphens, up to 50 chars.
     * Adjust the regex to match your institution's actual format.
     */
    public static void requireValidStudentId(String studentId) {
        requireNonBlank(studentId, "Student ID");
        if (!studentId.matches("^[A-Za-z0-9\\-]{1,50}$")) {
            throw new BusinessRuleException("Student ID format is invalid.");
        }
    }

    /**
     * Basic username rule: 3–100 chars, alphanumeric, underscores, dots, hyphens, and @.
     */
    public static void requireValidUsername(String username) {
        requireNonBlank(username, "Username");
        if (!username.matches("^[A-Za-z0-9_@\\.\\-]{3,100}$")) {
            throw new BusinessRuleException(
                    "Username must be 3–100 characters and contain only letters, digits, underscores, dots, hyphens, and @.");
        }
    }

    /**
     * Passwords must be at least 8 characters.
     */
    public static void requireValidPassword(String password) {
        requireNonBlank(password, "Password");
        requireMinLength(password, 8, "Password");
    }

    /**
     * Serial number: printable non-whitespace characters, 1–100 chars.
     */
    public static void requireValidSerialNumber(String serial) {
        requireNonBlank(serial, "Serial number");
        requireMaxLength(serial, 100, "Serial number");
        if (serial.matches(".*\\s.*")) {
            throw new BusinessRuleException("Serial number must not contain whitespace.");
        }
    }
}
