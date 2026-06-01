package com.pup.byod.javabyodbackend.model.enums;

public enum DeviceType {
    laptop,
    tablet,
    phone;

    public static DeviceType fromString(String value) {
        if (value == null) {
            return null;
        }

        try {
            return DeviceType.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown device type: " + value);
        }
    }
}
