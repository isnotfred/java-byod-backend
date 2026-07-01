package com.pup.byod.javabyodbackend.model.enums;

/**
 * Maps the requests.status CHECK constraint in PostgreSQL.
 * Values: 'pending', 'approved', 'rejected', 'returned'.
 */
public enum RequestStatus {
    pending,
    approved,
    rejected,
    returned,
    cancelled;

    public static RequestStatus fromString(String value) {
        if (value == null) return null;
        try {
            return RequestStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown request status: '" + value + "'");
        }
    }
}
