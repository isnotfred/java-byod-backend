package com.pup.byod.javabyodbackend.model;

public final class AuditActionTypes {
    private AuditActionTypes() {}

    // User management
    public static final String ADMIN_CREATED = "ADMIN_CREATED";
    public static final String ADMIN_UPDATED = "ADMIN_UPDATED";
    public static final String ADMIN_DEACTIVATED = "ADMIN_DEACTIVATED";
    public static final String GUARD_CREATED = "GUARD_CREATED";
    public static final String GUARD_UPDATED = "GUARD_UPDATED";
    public static final String GUARD_DEACTIVATED_BY_SUPER = "GUARD_DEACTIVATED_BY_SUPER";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String SYSTEM_CONFIG_UPDATED = "SYSTEM_CONFIG_UPDATED";

    // Request lifecycle
    public static final String REQUEST_CREATED = "REQUEST_CREATED";
    public static final String REQUEST_APPROVED = "REQUEST_APPROVED";
    public static final String REQUEST_REJECTED = "REQUEST_REJECTED";
    public static final String REQUEST_RETURNED = "REQUEST_RETURNED";

    // Device verification
    public static final String DEVICE_VERIFIED = "DEVICE_VERIFIED";

    // Gate scan / transactions
    public static final String DEVICE_CHECK_IN = "DEVICE_CHECK_IN";
    public static final String DEVICE_CHECK_OUT = "DEVICE_CHECK_OUT";
    public static final String MISSED_EGRESS_BATCH = "MISSED_EGRESS_BATCH";
}
