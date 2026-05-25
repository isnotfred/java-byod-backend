# BYOD Backend — Task Assignment

**Project:** `java-byod-backend`
**Stack:** Spring Boot + JDBC + PostgreSQL (Railway)
**Team:** Gelo, Me

---

## Work Order

Follow this order strictly. Each layer depends on the one before it.

```
Shared Setup → Enums → Models → DAOs → Services → Controllers
```

> **Me first:** Complete `DataSourceConfig.java` and `application.properties` before
> Gelo starts on Services — he needs the DB connection wired up to test anything.

---

## Me — JDBC Integration, Reports, Validation & Auth

Owns all DAOs across the entire project since all JDBC code lives there.

### Infrastructure (do these first)

| File | Package | Notes |
|---|---|---|
| `application.properties` | `resources/` | DB config, HikariCP, Jackson settings |
| `DataSourceConfig.java` | `config/` | HikariCP bean, reads Railway env vars |
| `CorsConfig.java` | `config/` | Allow requests from JavaFX client |

### Exception Classes

| File | Package |
|---|---|
| `ResourceNotFoundException.java` | `exception/` |
| `BusinessRuleException.java` | `exception/` |
| `GlobalExceptionHandler.java` | `exception/` |

### Utility Classes

| File | Package | Notes |
|---|---|---|
| `PasswordUtil.java` | `util/` | BCrypt hash and verify |
| `ValidationUtil.java` | `util/` | Required field checks, length, format |
| `DateUtil.java` | `util/` | Timestamp formatting for responses |

### Enums

| File | Package | DB Column |
|---|---|---|
| `Role.java` | `model/enums/` | `users.role` — `admin`, `guard` |

### Models

| File | Package | Maps To |
|---|---|---|
| `User.java` | `model/` | `users` table |
| `AuditLog.java` | `model/` | `audit_logs` table |

### DAOs (all of them — JDBC is your ownership)

| File | Package | DB Table / View / Function |
|---|---|---|
| `UserDAO.java` | `dao/` | `users` table |
| `AuditLogDAO.java` | `dao/` | Calls `fn_write_audit_log()` — never INSERT directly |
| `StudentDAO.java` | `dao/` | `students` table |
| `DeviceDAO.java` | `dao/` | `devices`, `v_pending_devices`, `v_device_campus_status` |
| `EventRequestDAO.java` | `dao/` | `event_requests`, `v_active_event_requests` |
| `EventRequestDeviceDAO.java` | `dao/` | `event_request_devices` |
| `DeviceLogDAO.java` | `dao/` | `device_logs` |

### Services

| File | Package | Logic |
|---|---|---|
| `AuthService.java` | `service/` | Login validation, BCrypt password verification |
| `UserService.java` | `service/` | User creation, status transitions, duplicate username checks |
| `AuditLogService.java` | `service/` | Orchestrates AuditLogDAO calls across all write operations |

### Controllers

| File | Package | Endpoints |
|---|---|---|
| `AuthController.java` | `controller/` | `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` |
| `UserController.java` | `controller/` | `GET /POST /PUT /api/v1/users` |
| `AuditLogController.java` | `controller/` | `GET /api/v1/audit-logs` |

---

## Gelo — Student & Device Registration + Ingress/Egress

Owns Enums, Models, Services, and Controllers for his features.
Does not write any DAO or JDBC code — coordinate with me when a new DAO method is needed.

### Enums

| File | Package | DB Column |
|---|---|---|
| `DeviceType.java` | `model/enums/` | `devices.device_type` — `laptop`, `tablet`, `phone` |
| `RegistrationStatus.java` | `model/enums/` | `devices.registration_status` — `pending`, `approved`, `rejected` |

### Models

| File | Package | Maps To |
|---|---|---|
| `Student.java` | `model/` | `students` table |
| `Device.java` | `model/` | `devices` table |
| `PendingDevice.java` | `model/` | `v_pending_devices` view |
| `DeviceCampusStatus.java` | `model/` | `v_device_campus_status` view |
| `DeviceLog.java` | `model/` | `device_logs` table |
| `EventRequest.java` | `model/` | `event_requests` table |
| `EventRequestDevice.java` | `model/` | `event_request_devices` table |
| `ActiveEventRequest.java` | `model/` | `v_active_event_requests` view |

### Services

| File | Package | Logic |
|---|---|---|
| `StudentService.java` | `service/` | Registration, soft-delete enforcement, search |
| `DeviceService.java` | `service/` | Registration, approval/rejection state machine, deactivation |
| `EventRequestService.java` | `service/` | Submission, approval workflow, date range validation |
| `DeviceLogService.java` | `service/` | Gate scan logic, consecutive-event prevention, auto-exit batch |

### Controllers

| File | Package | Endpoints |
|---|---|---|
| `StudentController.java` | `controller/` | `GET /POST /PUT /api/v1/students` |
| `DeviceController.java` | `controller/` | `GET /POST /PUT /api/v1/devices` |
| `EventRequestController.java` | `controller/` | `GET /POST /PUT /api/v1/event-requests` |
| `DeviceLogController.java` | `controller/` | `GET /POST /api/v1/device-logs` |

---

## Shared Files — Coordinate Before Touching

| File | Rule |
|---|---|
| `pom.xml` | Discuss before adding any dependency |
| `JavaByodBackendApplication.java` | Do not modify unless both agree |
| `schema.sql` | Already finalised — do not edit without discussion |

---

## Suggested Commit Convention

```
[layer] short description

Examples:
[config] add DataSourceConfig and CorsConfig
[model] add Student and Device POJOs
[dao] implement StudentDAO with RowMapper
[service] add DeviceService approval state machine
[controller] add DeviceController CRUD endpoints
[fix] handle null registration_status in DeviceDAO
```

---

## Full File Checklist

### Me
- [ ] `application.properties`
- [ ] `DataSourceConfig.java`
- [ ] `CorsConfig.java`
- [ ] `ResourceNotFoundException.java`
- [ ] `BusinessRuleException.java`
- [ ] `GlobalExceptionHandler.java`
- [ ] `PasswordUtil.java`
- [ ] `ValidationUtil.java`
- [ ] `DateUtil.java`
- [ ] `Role.java`
- [ ] `User.java`
- [ ] `AuditLog.java`
- [ ] `UserDAO.java`
- [ ] `AuditLogDAO.java`
- [ ] `StudentDAO.java`
- [ ] `DeviceDAO.java`
- [ ] `EventRequestDAO.java`
- [ ] `EventRequestDeviceDAO.java`
- [ ] `DeviceLogDAO.java`
- [ ] `AuthService.java`
- [ ] `UserService.java`
- [ ] `AuditLogService.java`
- [ ] `AuthController.java`
- [ ] `UserController.java`
- [ ] `AuditLogController.java`

### Gelo
- [ ] `DeviceType.java`
- [ ] `RegistrationStatus.java`
- [ ] `Student.java`
- [ ] `Device.java`
- [ ] `PendingDevice.java`
- [ ] `DeviceCampusStatus.java`
- [ ] `DeviceLog.java`
- [ ] `EventRequest.java`
- [ ] `EventRequestDevice.java`
- [ ] `ActiveEventRequest.java`
- [ ] `StudentService.java`
- [ ] `DeviceService.java`
- [ ] `EventRequestService.java`
- [ ] `DeviceLogService.java`
- [ ] `StudentController.java`
- [ ] `DeviceController.java`
- [ ] `EventRequestController.java`
- [ ] `DeviceLogController.java`
