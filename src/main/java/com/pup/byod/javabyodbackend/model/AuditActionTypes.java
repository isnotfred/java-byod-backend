package com.pup.byod.javabyodbackend.model;

public final class AuditActionTypes {
    private AuditActionTypes() {}

    public static final String ADMIN_CREATED = "ADMIN_CREATED";
    public static final String ADMIN_UPDATED = "ADMIN_UPDATED";
    public static final String ADMIN_DEACTIVATED = "ADMIN_DEACTIVATED";
    public static final String GUARD_CREATED = "GUARD_CREATED";
    public static final String GUARD_UPDATED = "GUARD_UPDATED";
    public static final String GUARD_DEACTIVATED_BY_SUPER = "GUARD_DEACTIVATED_BY_SUPER";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String SYSTEM_CONFIG_UPDATED = "SYSTEM_CONFIG_UPDATED";
}
