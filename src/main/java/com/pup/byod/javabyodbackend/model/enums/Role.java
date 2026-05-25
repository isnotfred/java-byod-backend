package com.pup.byod.javabyodbackend.model.enums;

/**
 * Maps the users.role CHECK constraint in PostgreSQL.
 * Values must match the DB strings exactly (lowercase).
 */
public enum Role {
    admin,
    guard;

    /**
     * Safe lookup that throws a clear message on unknown values
     * (used when mapping ResultSet strings).
     */
    public static Role fromString(String value) {
        if (value == null) return null;
        try {
            return Role.valueOf(value.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role value: '" + value + "'");
        }
    }
}
