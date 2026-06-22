package com.pup.byod.javabyodbackend.model.enums;

/**
 * Maps the requests.request_type CHECK constraint in PostgreSQL.
 * Values: 'normal', 'event'.
 */
public enum RequestType {
    normal,
    event;

    public static RequestType fromString(String value) {
        if (value == null) return null;
        try {
            return RequestType.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown request type: '" + value + "'");
        }
    }
}
