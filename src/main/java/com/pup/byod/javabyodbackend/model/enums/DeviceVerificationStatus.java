package com.pup.byod.javabyodbackend.model.enums;

/**
 * Maps the request_devices.device_status CHECK constraint in PostgreSQL.
 * Values: 'pending', 'approved', 'rejected'.
 */
public enum DeviceVerificationStatus {
    pending,
    approved,
    rejected;

    public static DeviceVerificationStatus fromString(String value) {
        if (value == null) return null;
        try {
            return DeviceVerificationStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown device verification status: '" + value + "'");
        }
    }
}
