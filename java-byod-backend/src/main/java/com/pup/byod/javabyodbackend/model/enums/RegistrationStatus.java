package com.pup.byod.javabyodbackend.model.enums;

public enum RegistrationStatus {
    pending,
    approved,
    rejected;

    public static RegistrationStatus fromString(String value) {
        if (value == null) {
            return null;
        }

        try {
            return RegistrationStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown registration status: " + value);
        }
    }
}
