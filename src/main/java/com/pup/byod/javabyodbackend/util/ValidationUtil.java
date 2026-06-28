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
        if (!studentId.matches("(?i)^2\\d{3}-\\d{5,6}-SR-0$")) {
            throw new BusinessRuleException("Student ID format must be 2###-######-SR-0 (e.g., 2024-00482-SR-0).");
        }
    }

    public static void requireValidCourseYearLevel(String value) {
        requireNonBlank(value, "Course Year Level");
        if (!value.matches("^[A-Za-z\\s.-]+\\s+\\d-\\d$")) {
            throw new BusinessRuleException("Course Year Level format must be Text #-# (e.g., BSIT 2-2).");
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

    /**
     * Validates a name: allows only letters (Unicode), spaces, hyphens, periods, and apostrophes.
     */
    public static void requireValidName(String name, String fieldName) {
        requireNonBlank(name, fieldName);
        if (!name.matches("^[\\p{L}\\s\\-\\.\\']+$")) {
            throw new BusinessRuleException(fieldName + " must contain only letters and name-appropriate symbols (spaces, hyphens, periods, apostrophes).");
        }
    }

    /**
     * Validates email format.
     */
    public static void requireValidEmail(String email) {
        requireNonBlank(email, "Email");
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessRuleException("Email format is invalid.");
        }
    }

    public static void requireValidOrganization(String org) {
        if (org != null && !org.isBlank()) {
            requireMaxLength(org, 255, "Organization");
            if (!org.matches("^[\\p{L}0-9\\s\\-\\.\\'&()]+$")) {
                throw new BusinessRuleException("Organization format is invalid.");
            }
        }
    }

    public static void requireValidEventName(String eventName) {
        requireNonBlank(eventName, "Event name");
        requireMaxLength(eventName, 255, "Event name");
        if (!eventName.matches("^[\\p{L}0-9\\s\\-\\.\\'\\!\\:\\,\\(\\)]+$")) {
            throw new BusinessRuleException("Event name must contain only letters, numbers, spaces, and basic punctuation (hyphens, periods, apostrophes, colons, exclamation marks, commas, parentheses).");
        }
    }

    public static void requireValidEventPurpose(String purpose) {
        if (purpose != null && !purpose.isBlank()) {
            requireMaxLength(purpose, 255, "Event purpose");
            if (!purpose.matches("^[\\p{L}0-9\\s\\-\\.\\,\\'\\(\\)]+$")) {
                throw new BusinessRuleException("Event purpose format is invalid.");
            }
        }
    }

    public static void requireValidDocRef(String ref) {
        if (ref != null && !ref.isBlank()) {
            requireMaxLength(ref, 255, "Approval document reference");
            if (!ref.matches("^[A-Za-z0-9\\-\\_\\.\\/\\:]+$")) {
                throw new BusinessRuleException("Approval document reference format is invalid.");
            }
        }
    }
}


