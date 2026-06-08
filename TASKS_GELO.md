# Backend Tasks - Gelo
**Modules:** Device Type Alignment and Super Admin Account Management  
**Project:** `java-byod-backend` | Spring Boot + PostgreSQL

---

## Read This First

This task file has been aligned with the current codebase. Follow these notes to avoid breaking compile:

- Current Java package root is `com/pup/byod/javabyodbackend`, not `com/pup/byod/backend`.
- `Role.java` currently uses lowercase enum constants because `UserDAO` writes `role.name()` directly to PostgreSQL. Use `Role.admin`, `Role.guard`, and `Role.super_admin`.
- `Role.super_admin` and AuthService helper methods were already added by the Reports/RBAC task.
- `DevicePurpose.java` does not currently exist. `devices.device_purpose` is currently modeled as `String`. Do not convert `Device.devicePurpose` to an enum unless you update every caller.
- Spring Security currently permits all requests in `SecurityConfig`, and method security is not enabled. `@PreAuthorize` annotations will not enforce 403 until method security/authentication is wired.

---

## Files At A Glance

| File | New / Modify |
|---|---|
| `model/enums/DeviceType.java` | Modify |
| `dao/DeviceDAO.java` | Modify |
| `controller/DeviceController.java` | Modify only if needed for `DeviceType` parsing |
| `service/DeviceService.java` | Modify only if needed for `DeviceType` parsing |
| `dao/UserDAO.java` | Modify - add small wrapper methods only if service needs them |
| `model/AuditActionTypes.java` | New, optional but preferred |
| `service/SuperAdminService.java` | New |
| `controller/SuperAdminController.java` | New |

> Dependency note: Do `DeviceType.java` first, then update all current `DeviceType.fromString(...)` and `deviceType.name()` call sites. Do not start Super Admin work until user role constants and DAO methods are confirmed.

---

## 1. Enum - `DeviceType.java`

**File:** `src/main/java/com/pup/byod/javabyodbackend/model/enums/DeviceType.java`

Replace the old values:

```java
laptop,
tablet,
phone
```

with the five category constants required by `src/main/resources/db/schema.sql`.

Use this shape, but keep `fromString(...)` as a compatibility alias because existing code already calls it:

```java
public enum DeviceType {
    PERSONAL_COMPUTERS("Personal Computers"),
    COMPONENTS_AND_PERIPHERALS("Components & Peripherals"),
    DISPLAY_AND_PROJECTION("Display & Projection"),
    PROJECT_PROTOTYPES("Project Prototypes (Optional SN)"),
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
```

Do not create or modify `DevicePurpose.java` for now. The current model stores purpose as `String`, and the database already accepts `PROTOTYPE` and `APPLIANCE`.

---

## 2. DAO - `DeviceDAO`

**File:** `src/main/java/com/pup/byod/javabyodbackend/dao/DeviceDAO.java`

### 2.1 Read `device_type` With DB Values

Update every `DeviceType.fromString(rs.getString("device_type"))` mapper to use:

```java
DeviceType.fromDbValue(rs.getString("device_type"))
```

Current places to check:

- main `Device` row mapper
- `PendingDevice` row mapper
- `DeviceCampusStatus` row mapper

Report DTO row mappers can stay as plain `String` because those report models expose `deviceType` as `String`.

### 2.2 Write `device_type` With DB Values

Any insert or update that writes `device_type` must use:

```java
device.getDeviceType().getDbValue()
```

not:

```java
device.getDeviceType().name()
```

Current place to check:

- `DeviceDAO.insert(Device device)`

---

## 3. Caller Updates For `DeviceType`

Search for:

```text
DeviceType.fromString
deviceType.name()
DeviceType.
```

Current known callers:

- `controller/DeviceController.java`
- `service/DeviceService.java`
- `dao/DeviceDAO.java`

These should still compile if `fromString(...)` remains as an alias. Prefer changing new code to `fromDbValue(...)`, but keeping the alias avoids a wide refactor.

---

## 4. DAO - `UserDAO`

**File:** `src/main/java/com/pup/byod/javabyodbackend/dao/UserDAO.java`

`SuperAdminService` may reuse the existing methods:

- `insert(User user)`
- `update(User user)`
- `setStatus(int userId, String status)`
- `findById(int userId)`
- `findByUsername(String username)`

If you want method names that match the service intent, add thin wrappers instead of duplicating SQL:

```java
public int createUser(User user) {
    return insert(user);
}

public int updateUser(User user) {
    return update(user);
}

public int setUserStatus(int userId, String status) {
    return setStatus(userId, status);
}

public int setUserRole(int userId, Role role) {
    String sql = "UPDATE users SET role = :role WHERE user_id = :userId";
    var params = new MapSqlParameterSource()
            .addValue("role", role.name())
            .addValue("userId", userId);
    return jdbc.update(sql, params);
}
```

Remember: use lowercase role constants in service code:

```java
Role.admin
Role.guard
Role.super_admin
```

---

## 5. Audit Action Type Constants

**Preferred new file:** `src/main/java/com/pup/byod/javabyodbackend/model/AuditActionTypes.java`

The database already allows these values in `schema.sql`; this task is only to avoid typo-prone inline strings.

```java
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
```

Do not remove or rewrite existing report query methods in `AuditLogDAO`.

---

## 6. Service - `SuperAdminService`

**New file:** `src/main/java/com/pup/byod/javabyodbackend/service/SuperAdminService.java`

Handles Super Admin account management for Admin and Guard accounts.

### Required Methods

| Method | Action Type |
|---|---|
| `createAdminAccount(...)` | `ADMIN_CREATED` |
| `createGuardAccount(...)` | `GUARD_CREATED` |
| `updateAccount(...)` | `ADMIN_UPDATED` or `GUARD_UPDATED` based on current role |
| `deactivateAdmin(...)` | `ADMIN_DEACTIVATED` |
| `deactivateGuard(...)` | `GUARD_DEACTIVATED_BY_SUPER` |
| `changeUserRole(...)` | `USER_ROLE_CHANGED` |

### Service Rules

- Mark mutating methods with `@Transactional`.
- Use `PasswordUtil.hash(...)`; never store plaintext passwords.
- Use `ValidationUtil` consistently with `UserService`.
- Check `actingUserId` by loading that user and requiring `Role.super_admin`.
- Do not rely only on controller annotations for authorization.
- Use `AuditLogDAO.writeAuditLog(...)` or `AuditLogService.writeAuditLog(...)` inside the same transaction as the user mutation.
- Store useful JSON strings in `old_values` and `new_values`, especially for role changes.
- Do not allow deactivation or role changes for a missing user; throw the existing `ResourceNotFoundException`.

Suggested role checks:

```java
private void requireSuperAdmin(int actingUserId) {
    User actor = userDAO.findById(actingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Acting user not found."));
    if (actor.getRole() != Role.super_admin) {
        throw new BusinessRuleException("Super admin access required.");
    }
}
```

---

## 7. Controller - `SuperAdminController`

**New file:** `src/main/java/com/pup/byod/javabyodbackend/controller/SuperAdminController.java`  
**Base path:** `/super-admin`

Create endpoints:

| Method | Endpoint | Delegates To |
|---|---|---|
| `POST` | `/super-admin/admins` | `createAdminAccount(...)` |
| `POST` | `/super-admin/guards` | `createGuardAccount(...)` |
| `PUT` | `/super-admin/users/{userId}` | `updateAccount(...)` |
| `PUT` | `/super-admin/users/{userId}/deactivate` | service determines current role, then deactivates |
| `PUT` | `/super-admin/users/{userId}/role` | `changeUserRole(...)` |

You may add:

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

but this will not work until method security is enabled and authentication creates `ROLE_SUPER_ADMIN`. Keep the service-level `actingUserId` check no matter what.

For now, include `actingUserId` in the request body or request parameter, matching the existing project style. Do not claim 403 behavior is complete unless `SecurityConfig` is updated with real authentication and method security.

---

## 8. What Is Already Done

- `Role.super_admin` exists.
- `AuthService.isSuperAdmin(...)`, `isAdminOrAbove(...)`, and `isAnyStaff(...)` exist.
- `schema.sql` already allows `super_admin`.
- `schema.sql` already allows the Super Admin audit action type strings.
- Report DAO methods in `DeviceDAO`, `DeviceLogDAO`, and `AuditLogDAO` already exist. Do not rewrite them for this task.

---

## Checklist

- [ ] `DeviceType.java` replaced with five database category constants
- [ ] `DeviceType.getDbValue()` added
- [ ] `DeviceType.fromDbValue(...)` added
- [ ] `DeviceType.fromString(...)` kept as a compatibility alias
- [ ] `DeviceDAO` row mappers use `DeviceType.fromDbValue(...)`
- [ ] `DeviceDAO.insert(...)` writes `deviceType.getDbValue()`
- [ ] No `DevicePurpose.java` migration done unless all callers are updated
- [ ] `UserDAO` wrappers added only if needed by `SuperAdminService`
- [ ] `UserDAO.setUserRole(...)` added if service needs direct role changes
- [ ] `AuditActionTypes` constants added
- [ ] `SuperAdminService` implemented with `@Transactional` methods
- [ ] `SuperAdminService` checks `actingUserId` is `Role.super_admin`
- [ ] `SuperAdminController` endpoints wired
- [ ] Security limitation documented or fixed before claiming 403 behavior
- [ ] `mvnw compile` passes with a Java 21 JDK
