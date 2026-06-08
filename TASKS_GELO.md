# Backend Tasks — Gelo
**Modules:** Student & Device Registration · Ingress/Egress Monitoring  
**Project:** `pup.edu.ph.it.javabyodsystem` | Spring Boot + PostgreSQL

---

## Files at a Glance

| File | New / Modify |
|---|---|
| `model/enums/DeviceType.java` | Modify |
| `model/enums/DevicePurpose.java` | Modify |
| `dao/DeviceDAO.java` | Modify |
| `dao/AuditLogDAO.java` | Modify (audit write only) |
| `service/SuperAdminService.java` | **New** |
| `controller/SuperAdminController.java` | **New** |

> **Dependency note:** Do the enums first. `DeviceDAO` and `SuperAdminService` both depend on them being correct before you touch anything else.

---

## 1. Enums

### 1.1 `DeviceType.java`

**File:** `com/pup/byod/javabyodbackend/model/enums/DeviceType.java`

Replace all existing subtype values with the five category constants. The `dbValue` string must match the database CHECK constraint exactly — spacing, ampersands, and parentheses included.

```java
public enum DeviceType {

    PERSONAL_COMPUTERS         ("Personal Computers"),
    COMPONENTS_AND_PERIPHERALS ("Components & Peripherals"),
    DISPLAY_AND_PROJECTION     ("Display & Projection"),
    PROJECT_PROTOTYPES         ("Project Prototypes (Optional SN)"),
    APPLIANCES_TLE             ("Appliances (TLE)");

    private final String dbValue;

    DeviceType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /** Use when reading device_type back from the DB. */
    public static DeviceType fromDbValue(String value) {
        for (DeviceType t : values()) {
            if (t.dbValue.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown device_type: " + value);
    }
}
```

### 1.2 `DevicePurpose.java`

**File:** `com/pup/byod/javabyodbackend/model/enums/DevicePurpose.java`

Add two new values to the existing enum:

```java
PROTOTYPE ("PROTOTYPE"),  // ← add
APPLIANCE ("APPLIANCE");  // ← add
```

---

## 2. DAO — `DeviceDAO`

**File:** `com/pup/byod/javabyodbackend/dao/DeviceDAO.java`

Two changes needed.

### 2.1 Update RowMapper for `device_type`

Find every place that reads `device_type` from a `ResultSet` and maps it to a Java type. Replace old subtype string comparisons with `DeviceType.fromDbValue(...)`.

**Before:**
```java
String raw = rs.getString("device_type");
DeviceType type = switch (raw) {
    case "laptop" -> DeviceType.LAPTOP;
    case "tablet" -> DeviceType.TABLET;
    case "phone"  -> DeviceType.PHONE;
    default       -> throw new IllegalArgumentException("Unknown: " + raw);
};
```

**After:**
```java
DeviceType type = DeviceType.fromDbValue(rs.getString("device_type"));
```

Do the same for any INSERT or UPDATE that writes `device_type` — use `deviceType.getDbValue()` instead of `.name()` or a hardcoded string.

### 2.2 Confirm `UserDAO` has create/update/deactivate

`SuperAdminService` reuses `UserDAO` directly — no new DAO is needed. But before writing the service, confirm these methods exist in `UserDAO`. If any are missing, add them now:

- `createUser(User user)` — inserts a new row into `users`
- `updateUser(int userId, User user)` — updates `full_name`, `username`, etc.
- `setUserStatus(int userId, String status)` — sets `status = 'active'` or `'inactive'`
- `setUserRole(int userId, Role role)` — updates the `role` column

---

## 3. DAO — `AuditLogDAO` (Super Admin call sites only)

**File:** `com/pup/byod/javabyodbackend/dao/AuditLogDAO.java`

No new query methods are needed here for your tasks. The existing `write(...)` method handles all action types. What you do need to confirm before writing `SuperAdminService`:

Make sure the following action type strings are defined as constants somewhere accessible (a dedicated `AuditActionTypes` constants class, or inline in `AuditLogDAO`):

```
ADMIN_CREATED            ADMIN_UPDATED            ADMIN_DEACTIVATED
GUARD_CREATED            GUARD_UPDATED            GUARD_DEACTIVATED_BY_SUPER
USER_ROLE_CHANGED        SYSTEM_CONFIG_UPDATED
```

If no constants class exists yet, create one:

```java
// com/pup/byod/javabyodbackend/model/AuditActionTypes.java
public final class AuditActionTypes {
    private AuditActionTypes() {}

    public static final String ADMIN_CREATED             = "ADMIN_CREATED";
    public static final String ADMIN_UPDATED             = "ADMIN_UPDATED";
    public static final String ADMIN_DEACTIVATED         = "ADMIN_DEACTIVATED";
    public static final String GUARD_CREATED             = "GUARD_CREATED";
    public static final String GUARD_UPDATED             = "GUARD_UPDATED";
    public static final String GUARD_DEACTIVATED_BY_SUPER = "GUARD_DEACTIVATED_BY_SUPER";
    public static final String USER_ROLE_CHANGED         = "USER_ROLE_CHANGED";
    public static final String SYSTEM_CONFIG_UPDATED     = "SYSTEM_CONFIG_UPDATED";
    // Add any existing action type constants here too
}
```

---

## 4. Service — `SuperAdminService` (New)

**File:** `com/pup/byod/javabyodbackend/service/SuperAdminService.java`

Handles all account management operations performed by a Super Admin: creating, updating, deactivating Admin and Guard accounts, and changing roles.

### Required methods

| Method | Action type logged |
|---|---|
| `createAdminAccount(CreateUserRequest req)` | `ADMIN_CREATED` |
| `createGuardAccount(CreateUserRequest req)` | `GUARD_CREATED` |
| `updateAccount(int userId, UpdateUserRequest req)` | `ADMIN_UPDATED` or `GUARD_UPDATED` — check current role first |
| `deactivateAdmin(int userId, int actingUserId)` | `ADMIN_DEACTIVATED` |
| `deactivateGuard(int userId, int actingUserId)` | `GUARD_DEACTIVATED_BY_SUPER` |
| `changeUserRole(int userId, Role newRole, int actingUserId)` | `USER_ROLE_CHANGED` — store old and new role in `old_values`/`new_values` |

### Rules

- Every method must wrap its `UserDAO` mutation and `AuditLogDAO.write(...)` call in a **single transaction**. Do not call the audit write outside the transaction boundary.
- Reject at the service layer if the acting user is not `SUPER_ADMIN` — do not rely solely on the controller for this check.
- Passwords must be stored as a bcrypt or argon2 hash — never plaintext. Delegate hashing to the same utility already used in the existing user creation flow.

### Skeleton

```java
@Service
public class SuperAdminService {

    private final UserDAO     userDAO;
    private final AuditLogDAO auditLogDAO;
    // inject password encoder from existing auth utilities

    @Transactional
    public void createAdminAccount(CreateUserRequest req) {
        // 1. Hash password
        // 2. userDAO.createUser(...) with role = ADMIN
        // 3. auditLogDAO.write(ADMIN_CREATED, "users", newUserId, null, newValues)
    }

    @Transactional
    public void createGuardAccount(CreateUserRequest req) { ... }

    @Transactional
    public void updateAccount(int userId, UpdateUserRequest req) {
        // 1. Fetch current user to determine role (ADMIN or GUARD)
        // 2. userDAO.updateUser(...)
        // 3. auditLogDAO.write(ADMIN_UPDATED or GUARD_UPDATED, ...)
    }

    @Transactional
    public void deactivateAdmin(int userId, int actingUserId) {
        // 1. userDAO.setUserStatus(userId, "inactive")
        // 2. auditLogDAO.write(ADMIN_DEACTIVATED, ...)
    }

    @Transactional
    public void deactivateGuard(int userId, int actingUserId) { ... }

    @Transactional
    public void changeUserRole(int userId, Role newRole, int actingUserId) {
        // 1. Fetch current role for old_values
        // 2. userDAO.setUserRole(userId, newRole)
        // 3. auditLogDAO.write(USER_ROLE_CHANGED, old_values={role:old}, new_values={role:new})
    }
}
```

---

## 5. Controller — `SuperAdminController` (New)

**File:** `com/pup/byod/javabyodbackend/controller/SuperAdminController.java`  
**Base path:** `/super-admin`  
**Access:** `SUPER_ADMIN` only — set `@PreAuthorize` at class level

### Endpoints

| Method | Endpoint | Delegates to |
|---|---|---|
| `POST` | `/super-admin/admins` | `SuperAdminService.createAdminAccount(...)` |
| `POST` | `/super-admin/guards` | `SuperAdminService.createGuardAccount(...)` |
| `PUT` | `/super-admin/users/{userId}` | `SuperAdminService.updateAccount(...)` |
| `PUT` | `/super-admin/users/{userId}/deactivate` | `SuperAdminService.deactivateAdmin/Guard(...)` |
| `PUT` | `/super-admin/users/{userId}/role` | `SuperAdminService.changeUserRole(...)` |

All endpoints must return `403 Forbidden` — not `404` — when called by an `ADMIN` or `GUARD` role.

```java
@RestController
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @PostMapping("/admins")
    public ResponseEntity<Void> createAdmin(@RequestBody CreateUserRequest req) {
        superAdminService.createAdminAccount(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/guards")
    public ResponseEntity<Void> createGuard(@RequestBody CreateUserRequest req) {
        superAdminService.createGuardAccount(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable int userId,
                                           @RequestBody UpdateUserRequest req) {
        superAdminService.updateAccount(userId, req);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable int userId,
                                               @RequestParam int actingUserId) {
        // Determine admin vs guard inside the service
        superAdminService.deactivateAdmin(userId, actingUserId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> changeRole(@PathVariable int userId,
                                           @RequestParam Role newRole,
                                           @RequestParam int actingUserId) {
        superAdminService.changeUserRole(userId, newRole, actingUserId);
        return ResponseEntity.ok().build();
    }
}
```

---

## Checklist

- [ ] `DeviceType.java` — replaced with five category constants + `getDbValue()` / `fromDbValue()`
- [ ] `DevicePurpose.java` — `PROTOTYPE` and `APPLIANCE` added
- [ ] `DeviceDAO` RowMapper updated to use `DeviceType.fromDbValue(...)`
- [ ] All `device_type` INSERT/UPDATE paths use `deviceType.getDbValue()`
- [ ] `UserDAO` has `createUser`, `updateUser`, `setUserStatus`, `setUserRole`
- [ ] Audit action type constants defined and accessible
- [ ] `SuperAdminService` — all six methods implemented with transactional audit writes
- [ ] `SuperAdminController` — all five endpoints wired, returns `403` for wrong roles