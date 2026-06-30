package com.pup.byod.javabyodbackend.model.enums;

public enum DeviceType {
    PERSONAL_COMPUTERS("Personal Computers"),
    COMPONENTS_AND_PERIPHERALS("Components & Peripherals"),
    DISPLAY_AND_PROJECTION("Display & Projection"),
    PROJECT_PROTOTYPES("Project Prototypes"),
    APPLIANCES_TLE("Appliances (TLE)");

    private final String dbValue;

    DeviceType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static DeviceType fromDbValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        for (DeviceType type : values()) {
            if (type.dbValue.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown device_type: " + value);
    }

    public static DeviceType fromString(String value) {
        return fromDbValue(value);
    }
}
