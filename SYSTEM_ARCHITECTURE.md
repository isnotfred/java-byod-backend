# BYOD Device Management System — System Architecture Document

`pup.edu.ph.it.javabyodsystem` | JavaFX + Spring Boot + PostgreSQL

| Layer | Technology |
|---|---|
| **Frontend** | JavaFX (IntelliJ IDEA, FXML, CSS) |
| **Backend** | Spring Boot + JDBC (IntelliJ IDEA) |
| **Database** | PostgreSQL (hosted on Railway) |
| **Hosting** | Railway — Backend API + PostgreSQL database |
| **Version Control** | Git + GitHub (two separate repositories) |

---

## 1. Overview

The BYOD Device Management System is a two-repository desktop application. The JavaFX frontend communicates with a Spring Boot REST API backend over HTTP/HTTPS. The backend talks exclusively to a PostgreSQL database hosted on Railway. All three tiers run independently and are deployed or run separately.

**Architecture type:** Client–Server (3-tier: JavaFX Client → Spring Boot API → PostgreSQL)

| Tier | Technology | Runs On | Role |
|---|---|---|---|
| **Frontend** | JavaFX | Developer machine / end-user PC | UI, user input, HTTP calls to backend |
| **Backend** | Spring Boot + JDBC | Railway (cloud) | REST API, business logic, DB access |
| **Database** | PostgreSQL | Railway (cloud) | Persistent storage, triggers, views, functions |

---

## 2. System Architecture Diagram

```
FRONTEND (JavaFX — local machine)
  Controller → HTTP Client (RestTemplate / HttpClient)
  FXML / CSS / Model (JavaFX ObservableList, POJO)
          ↕  HTTPS (JSON REST API)
BACKEND (Spring Boot — Railway)
  @RestController → @Service → DAO (JDBC / NamedParameterJdbcTemplate)
  Model (POJO / RowMapper)
          ↕  JDBC (PostgreSQL Driver 42.7.x)
DATABASE (PostgreSQL — Railway)
  Tables · Views · Triggers · Functions (fn_write_audit_log, etc.)
```

---

## 3. Frontend–Backend Communication

The JavaFX frontend and the Spring Boot backend are completely separate processes. They communicate exclusively through a JSON REST API over HTTPS. The frontend never connects directly to PostgreSQL.

| From | To | Protocol | Format |
|---|---|---|---|
| JavaFX Controller | Spring Boot @RestController | HTTPS (HTTP/1.1 or HTTP/2) | JSON request body / query params |
| Spring Boot @RestController | JavaFX Controller | HTTPS response | JSON response body (+ HTTP status code) |
| Spring Boot @Service / DAO | PostgreSQL (Railway) | JDBC over TLS | PreparedStatement / RowMapper |

**Base URL (Railway):** `https://<app-name>.railway.app/api/v1/`

---

## 4. Backend — Spring Boot Package Structure

All backend source code lives under the base package: `com.pup.byod.javabyodbackend/`

| Package / Layer | Full Package Path | Responsibility |
|---|---|---|
| **controller/** | com.pup.byod.javabyodbackend.controller | @RestController — receives HTTP requests, returns ResponseEntity |
| **service/** | com.pup.byod.javabyodbackend.service | Business logic, auth, validation, transaction orchestration |
| **dao/** | com.pup.byod.javabyodbackend.dao | All SQL via JDBC / NamedParameterJdbcTemplate; RowMapper |
| **model/** | com.pup.byod.javabyodbackend.model | POJOs, enums, constants, and report rows matching DB |
| **config/** | com.pup.byod.javabyodbackend.config | DataSource bean, CORS config, security config, DB connection pool |
| **util/** | com.pup.byod.javabyodbackend.util | Password hashing (BCrypt), validation helpers, date formatters |
| **exception/** | com.pup.byod.javabyodbackend.exception | Custom exception classes + @ControllerAdvice global error handler |

---

## 5. Backend — Layered Architecture

The backend follows a strict unidirectional call chain. No layer skips another. A Controller never calls a DAO directly, and a DAO never calls a Service.

### 5.1 Controller Layer (`@RestController`)

The Controller is the HTTP entry point. It:

- Receives HTTP requests and maps them with `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- Reads JSON request bodies (`@RequestBody`) and path/query parameters
- Calls the appropriate Service method
- Returns `ResponseEntity` with the correct HTTP status code and JSON body
- Catches exceptions thrown by Service and maps them to HTTP error responses

A Controller contains no SQL, business rules, or password hashing.

### 5.2 Service Layer (`@Service`)

The Service layer contains all business logic. It:

- Validates inputs — required fields, format checks, business rules
- Handles authentication — verifies password hash using BCrypt from `util/`
- Orchestrates multi-step operations with `@Transactional`
- Calls one or more DAO methods to read or write data
- Calls `AuditLogDAO` to write audit entries via `fn_write_audit_log()`
- Throws descriptive exceptions that the Controller maps to HTTP responses

A Service never references `HttpServletRequest`, `HttpServletResponse`, or any Spring MVC type.

### 5.3 DAO Layer (JDBC)

The DAO layer is the only part of the backend that talks to PostgreSQL. It:

- Uses `NamedParameterJdbcTemplate` or `JdbcTemplate` from Spring JDBC
- Writes all SQL as parameterised queries (no string concatenation)
- Maps each `ResultSet` row to a Model object via a `RowMapper`
- Calls the database function `fn_write_audit_log()` for audit entries
- Uses `@Transactional` on multi-step operations
- Throws `DataAccessException` upward — the Service layer decides what to do

Views used directly in DAO queries:

- `v_device_campus_status` — current inside/outside status per device
- `v_pending_devices` — pending device registrations for the approval queue
- `v_active_event_requests` — active event requests with device counts

### 5.4 Model Layer (POJOs)

Models are plain Java objects with fields that match the database table columns. One Model class per table. Models contain private fields with getters/setters, and enums for constrained fields (`DeviceType`, `RegistrationStatus`, `Role`, etc.). Models carry no logic. Additionally, this layer contains constants for audit actions (`AuditActionTypes`) and DTOs representing query result structures for reports (under `model/report/`).

---

## 6. Database — PostgreSQL on Railway

The PostgreSQL database is hosted on Railway and is the single source of truth. The backend connects via JDBC with credentials injected as Railway environment variables. The JavaFX frontend never connects directly to the database.

- **JDBC URL pattern:** `jdbc:postgresql://<railway-host>:<port>/byod_db`
- **Driver:** `org.postgresql.Driver` (PostgreSQL JDBC 42.7.x)
- **Connection pool:** HikariCP (Spring Boot default)

### 6.1 Tables

| Table | Primary Key | Purpose |
|---|---|---|
| **users** | user_id SERIAL | Admin, guard, and super admin accounts (no student logins) |
| **students** | student_id VARCHAR(50) | Student registry — never hard-delete; set status = inactive |
| **devices** | device_id SERIAL | Permanent BYOD device registrations |
| **event_requests** | event_request_id SERIAL | Header for a temporary device access request (school events, orgs) |
| **event_request_devices** | event_device_id SERIAL | Individual devices listed under an event request |
| **device_logs** | log_id SERIAL | Immutable gate entry/exit event log — never UPDATE or DELETE |
| **audit_logs** | audit_id SERIAL | Immutable system-wide audit trail — write via fn_write_audit_log() only |

### 6.2 Views

| View Name | Purpose |
|---|---|
| **v_device_campus_status** | Derives inside/outside status per approved active device from the latest device_log row |
| **v_pending_devices** | Pending device registrations for the admin approval queue, with student name joined |
| **v_active_event_requests** | Pending and approved event requests with device counts |

### 6.3 Key Functions and Triggers

| Name | Purpose |
|---|---|
| **fn_write_audit_log()** | Preferred writer for audit_logs. Called from DAO layer only. Prevents direct INSERT. |
| **fn_set_updated_at()** | Auto-refreshes updated_at on every UPDATE across all mutable tables |
| **fn_force_created_at()** | Forces server-side created_at timestamp on device_logs and audit_logs to prevent backdating |
| **fn_guard_registration_transition()** | Enforces device registration state machine: pending → approved \| pending → rejected \| rejected → pending |
| **fn_guard_device_log_approved_only()** | Blocks gate log inserts for unapproved or inactive devices |
| **fn_guard_consecutive_events()** | Blocks two consecutive same-type events (e.g. double-entry without exit). Auto-exit rows are exempt. |
| **fn_audit_log_immutable()** | Prevents UPDATE and DELETE on audit_logs rows |
| **fn_device_log_immutable()** | Prevents hard-delete on device_logs rows |
| **fn_protect_student/device/user_delete()** | Blocks hard-delete when referencing records exist; requires setting status = inactive instead |

---

## 7. Request Flow (End-to-End)

The table below traces a single user action — e.g. a guard approving a device — from the JavaFX UI to the database and back.

| # | Layer | What Happens | Passes To Next |
|---|---|---|---|
| **1** | JavaFX UI (FXML) | User clicks button / submits form | Raw input to Controller |
| **2** | JavaFX Controller | Reads nodes, serialises to JSON, sends HTTP request via HttpClient | HTTPS request to backend |
| **3** | Spring Boot @RestController | Deserialises JSON to DTO / model, calls Service method | Method call + parameters |
| **4** | Spring Boot @Service | Validates business rules, calls DAO, calls AuditLogDAO | Model objects or primitives |
| **5** | DAO (JDBC) | Executes PreparedStatement, calls fn_write_audit_log() | SQL + parameters to DB |
| **6** | PostgreSQL (Railway) | Executes SQL, fires triggers, returns ResultSet / confirmation | ResultSet rows |
| **7** | DAO → Service → Controller | ResultSet mapped to Model POJOs; returned up the chain | ResponseEntity (JSON) |
| **8** | JavaFX Controller | Parses JSON response, updates TableView / labels / alerts | UI update (ObservableList) |

---

## 8. Error Handling Strategy

Errors originate in three places: the Service layer (business rule violations), the DAO layer (SQL failures via `DataAccessException`), and the PostgreSQL database (trigger exceptions). All errors propagate as HTTP error responses to the JavaFX frontend.

| Trigger | Caught In | Shown to User As |
|---|---|---|
| DB trigger blocks INSERT/UPDATE | @ControllerAdvice (HTTP 400) | Friendly banner / alert dialog in JavaFX |
| Duplicate unique key | @ControllerAdvice (HTTP 409) | "Serial number already exists", "Username already exists", or "Student ID already exists" |
| Business rule violation | @ControllerAdvice (HTTP 422) | Custom message text surfaced in alert |
| Super admin access required | @ControllerAdvice (HTTP 403) | "Super admin access required" |
| Resource not found | @ControllerAdvice (HTTP 404) | "Acting user not found", "User to update not found", etc. |
| Auth failure | Service → HTTP 401 | "Invalid username or password" |
| Inactive account | Service → HTTP 403 | "Account is inactive" |
| DB connection lost | HikariCP / @ControllerAdvice 503 | "Database connection failed" |
| Field validation failure / invalid argument | @ControllerAdvice (HTTP 400) | Field-level error messages on form / validation error details |
| General internal error | @ControllerAdvice (HTTP 500) | "An unexpected error occurred." |

---

## 9. Audit Logging

Every significant action writes a row to `audit_logs` via the PostgreSQL function `fn_write_audit_log()`. This is always called from the DAO layer inside a transaction — never from the Controller or directly from the Service.

**Function signature:**

```sql
fn_write_audit_log(
    p_user_id      INT,
    p_action_type  VARCHAR,   -- e.g. 'DEVICE_APPROVED'
    p_target_table VARCHAR,   -- e.g. 'devices'
    p_target_id    VARCHAR,   -- e.g. '42'
    p_old_values   JSONB,     -- state before change
    p_new_values   JSONB,     -- state after change
    p_ip_address   VARCHAR
)
```

Standardised `action_type` values (enforced by CHECK constraint):

| Device Actions | Student/User Actions | Event / System Actions |
|---|---|---|
| DEVICE_REGISTERED | STUDENT_CREATED | EVENT_REQUEST_CREATED |
| DEVICE_APPROVED | STUDENT_UPDATED | EVENT_REQUEST_APPROVED |
| DEVICE_REJECTED | STUDENT_DEACTIVATED | EVENT_REQUEST_RETURNED |
| DEVICE_DEACTIVATED | USER_CREATED | EVENT_REQUEST_REJECTED |
| DEVICE_UPDATED | USER_UPDATED | SYSTEM_AUTO_EXIT_BATCH |
| DEVICE_ENTRY | USER_DEACTIVATED | SYSTEM_CONFIG_UPDATED |
| DEVICE_EXIT | USER_LOGIN | |
| DEVICE_AUTO_EXIT | USER_LOGOUT | |
| | USER_LOGIN_FAILED | |
| | USER_ROLE_CHANGED | |
| | ADMIN_CREATED | |
| | ADMIN_UPDATED | |
| | ADMIN_DEACTIVATED | |
| | GUARD_CREATED | |
| | GUARD_UPDATED | |
| | GUARD_DEACTIVATED_BY_SUPER | |

---

## 10. Project File Structure

The frontend and backend live in two separate Git repositories. Each follows the standard IntelliJ IDEA project layout for its respective framework.

### 10.1 Frontend Repository — JavaFX (IntelliJ IDEA)

The frontend has no `service/` or `util/` sub-packages in the current structure. All HTTP calls are initiated from controllers directly. Model classes include both table-backed POJOs and view-specific models (`PendingDevice`, `DeviceCampusStatus`, `ActiveEventRequest`) that mirror the data returned by the backend API.

**Controllers (`controller/`)**

| Controller Class | Screen / Role |
|---|---|
| LoginScreenController | Login screen — credential input, auth request to backend |
| AdminDashboardController | Admin home dashboard — navigation hub for admin role |
| SecurityGuardDashboardController | Guard home dashboard — navigation hub for guard role |
| DeviceManagementScreenController | Full device registry — browse, search, deactivate devices |
| PendingRegistrationApprovalScreenController | Admin approval queue — approve or reject pending devices |
| QuickPendingRegistrationScreenController | Fast-path device registration form for guards |
| StudentManagementScreenController | Student registry — add, search, deactivate students |
| UserManagementScreenController | User (admin/guard) management — create, deactivate accounts |
| IngressEgressMonitoringScreenController | Gate scan screen — log device entry/exit by serial number |
| ActiveDevicesInsideCampusScreenController | Live view of devices currently inside campus (guard view) |
| TemporaryEventDeviceScreenController | Event request management — submit, view, approve event device requests |
| LogsScreenController | Device logs and audit logs — browse historical records |
| ReportsScreenController | Reports screen — generate and display summaries |

**Models (`model/`)**

| Model Class | Maps To |
|---|---|
| User | users table — admin and guard account data |
| Student | students table — student registry |
| Device | devices table — registered BYOD devices |
| PendingDevice | v_pending_devices view — device + student name for approval queue |
| DeviceCampusStatus | v_device_campus_status view — inside/outside status per device |
| DeviceLog | device_logs table — gate entry/exit events |
| EventRequest | event_requests table — event device request headers |
| EventRequestDevice | event_request_devices table — individual devices per event request |
| ActiveEventRequest | v_active_event_requests view — pending/approved requests with device count |
| AuditLog | audit_logs table — system-wide audit trail |

**Enums (`model/enums/`)**

| Enum Class | Mirrors DB CHECK Constraint On |
|---|---|
| UserRole | users.role — admin, guard |
| EntityStatus | users.status, students.status, devices.device_status — active, inactive |
| RegistrationStatus | devices.registration_status — pending, approved, rejected |
| DeviceType | devices.device_type — laptop, tablet, phone |
| DevicePurpose | devices.device_purpose — Academic BYOD, School Event, etc. |
| GateEventType | device_logs.event_type — entry, exit |
| LogoutType | device_logs.logout_type — manual, automatic |
| EventRequestStatus | event_requests.status — pending, approved, returned, rejected |
| ApprovalDocType | event_requests.approval_doc_type — Paper Approval, Signed GPOA |
| EventDeviceType | event_request_devices.device_type — laptop, tablet, phone, camera, projector, other |
| EventDeviceStatus | event_request_devices.device_status — pending, approved, returned |
| AuditActionType | audit_logs.action_type — DEVICE_REGISTERED, USER_LOGIN, etc. |

**File Tree:**

```
byod-frontend/                                        ← GitHub repo root
├── .idea/                                            ← IntelliJ IDEA project files
├── src/
│   └── main/
│       ├── java/
│       │   │   module-info.java                      ← JavaFX module declaration
│       │   └── pup/edu/ph/it/javabyodsystem/
│       │       ├── Launcher.java                     ← Main entry point workaround
│       │       ├── Main.java                         ← JavaFX Application subclass
│       │       ├── controller/                       ← One controller per screen
│       │       │   ├── LoginScreenController.java
│       │       │   ├── AdminDashboardController.java
│       │       │   ├── SecurityGuardDashboardController.java
│       │       │   ├── DeviceManagementScreenController.java
│       │       │   ├── PendingRegistrationApprovalScreenController.java
│       │       │   ├── QuickPendingRegistrationScreenController.java
│       │       │   ├── StudentManagementScreenController.java
│       │       │   ├── UserManagementScreenController.java
│       │       │   ├── IngressEgressMonitoringScreenController.java
│       │       │   ├── ActiveDevicesInsideCampusScreenController.java
│       │       │   ├── TemporaryEventDeviceScreenController.java
│       │       │   ├── LogsScreenController.java
│       │       │   └── ReportsScreenController.java
│       │       └── model/                            ← POJOs matching backend DTOs / DB views
│       │           ├── ActiveEventRequest.java
│       │           ├── AuditLog.java
│       │           ├── Device.java
│       │           ├── DeviceCampusStatus.java
│       │           ├── DeviceLog.java
│       │           ├── EventRequest.java
│       │           ├── EventRequestDevice.java
│       │           ├── PendingDevice.java
│       │           ├── Student.java
│       │           ├── User.java
│       │           └── enums/                        ← Mirrors DB CHECK constraint vocabularies
│       │               ├── ApprovalDocType.java
│       │               ├── AuditActionType.java
│       │               ├── DevicePurpose.java
│       │               ├── DeviceType.java
│       │               ├── EntityStatus.java
│       │               ├── EventDeviceStatus.java
│       │               ├── EventDeviceType.java
│       │               ├── EventRequestStatus.java
│       │               ├── GateEventType.java
│       │               ├── LogoutType.java
│       │               ├── RegistrationStatus.java
│       │               └── UserRole.java
│       └── resources/
│           └── pup/edu/ph/it/javabyodsystem/
│               ├── fxml/                             ← One .fxml per screen
│               │   ├── LoginScreen.fxml
│               │   ├── AdminDashboard.fxml
│               │   ├── SecurityGuardDashboard.fxml
│               │   ├── DeviceManagementScreen.fxml
│               │   ├── PendingRegistrationApprovalScreen.fxml
│               │   ├── QuickPendingRegistrationScreen.fxml
│               │   ├── StudentManagementScreen.fxml
│               │   ├── UserManagementScreen.fxml
│               │   ├── IngressEgressMonitoringScreen.fxml
│               │   ├── ActiveDevicesInsideCampusScreen.fxml
│               │   ├── ActiveDevicesAdminScreen.fxml
│               │   ├── TemporaryEventDeviceScreen.fxml
│               │   ├── TemporaryEventDeviceGuardScreen.fxml
│               │   ├── LogsScreen.fxml
│               │   └── ReportsScreen.fxml
│               └── css/
│                   └── styles.css                    ← Global stylesheet
├── pom.xml                                           ← Maven dependencies
├── .gitignore
└── README.md
```

### 10.2 Backend Repository — Spring Boot (IntelliJ IDEA)

The backend follows the standard Spring Boot layered structure. Each layer has one clear responsibility and communicates only with the layer directly adjacent to it. Controllers never call DAOs, and DAOs never call Services.

**Controllers (`controller/`)**

| Controller Class | Endpoints / Role |
|---|---|
| AuthController | POST /auth/login — authenticate user, return session/token |
| UserController | GET/POST/PUT /users — user account CRUD |
| StudentController | GET/POST/PUT /students — student registry CRUD |
| DeviceController | GET/POST/PUT /devices — device registration, approval, deactivation |
| EventRequestController | GET/POST/PUT /event-requests — event request lifecycle |
| DeviceLogController | GET/POST /device-logs — gate entry/exit logging and history |
| AuditLogController | GET /audit-logs — read-only audit trail queries |
| ReportController | GET /reports/* — daily/monthly traffic, pending registrations, active devices, device frequency, incident reports |
| SuperAdminController | POST/PUT /super-admin/* — manage admins/guards (create, update, deactivate, change role) |

**Services (`service/`)**

| Service Class | Business Logic Handled |
|---|---|
| AuthService | Login validation, BCrypt password verification, session management |
| UserService | User creation, status transitions, duplicate username checks |
| StudentService | Student registration, soft-delete enforcement, search logic |
| DeviceService | Device registration, approval/rejection state machine, deactivation |
| EventRequestService | Event request submission, approval workflow, date range validation |
| DeviceLogService | Gate scan logic, consecutive-event prevention, auto-exit batch |
| AuditLogService | Orchestrates calls to AuditLogDAO / fn_write_audit_log() |
| ReportService | Produces all six report types required by the BYOD business analysis |
| SuperAdminService | Account CRUD, status updates, role changes, and super admin authorization checks |

**DAOs (`dao/`)**

| DAO Class | DB Table / View Accessed |
|---|---|
| UserDAO | users table |
| StudentDAO | students table |
| DeviceDAO | devices table, v_pending_devices, v_device_campus_status |
| EventRequestDAO | event_requests table, v_active_event_requests |
| EventRequestDeviceDAO | event_request_devices table |
| DeviceLogDAO | device_logs table |
| AuditLogDAO | Calls fn_write_audit_log() — never INSERTs into audit_logs directly |

**Models (`model/`)**

| Model Class | Maps To |
|---|---|
| User | users table |
| Student | students table |
| Device | devices table |
| EventRequest | event_requests table |
| EventRequestDevice | event_request_devices table |
| DeviceLog | device_logs table |
| AuditLog | audit_logs table |
| AuditActionTypes | Constant values for audit actions |

**Report Models (`model/report/`)**

| Model Class | Maps To |
|---|---|
| ActiveDeviceRow | Real-time snapshot of active devices on campus |
| DailyTrafficRow | Entry/exit events on a given day |
| DeviceFrequencyRow | Device entry/exit frequency over a date range |
| IncidentOverrideRow | Admin overrides, rejections, and dispute resolutions |
| MonthlyTrafficRow | Aggregated monthly traffic grouped by category & student |
| PendingRegistrationRow | All devices in 'pending' status joined with submitter info |

**Enums (`model/enums/`)**

| Enum Class | Used For |
|---|---|
| Role | users.role — admin, guard, super_admin |
| DeviceType | devices.device_type and event_request_devices.device_type — Personal Computers, Components & Peripherals, Display & Projection, Project Prototypes (Optional SN), Appliances (TLE) |
| RegistrationStatus | devices.registration_status — JDBC mapping and Service validation |

**Supporting Packages:**

| Package | Contents |
|---|---|
| config/ | DataSourceConfig (HikariCP + Railway env vars), CorsConfig (allow JavaFX host), SecurityConfig (stateless security configuration) |
| exception/ | ResourceNotFoundException, BusinessRuleException, ForbiddenException, GlobalExceptionHandler (@ControllerAdvice) |
| util/ | PasswordUtil (BCrypt hash/verify), ValidationUtil, DateUtil |

**File Tree:**

```
byod-backend/                           ← GitHub repo root
├── .idea/                              ← IntelliJ IDEA project files
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/pup/byod/javabyodbackend/
│   │   │       ├── JavaByodBackendApplication.java   ← Spring Boot entry point
│   │   │       ├── controller/                       ← @RestController — HTTP endpoints
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── StudentController.java
│   │   │       │   ├── DeviceController.java
│   │   │       │   ├── EventRequestController.java
│   │   │       │   ├── DeviceLogController.java
│   │   │       │   ├── AuditLogController.java
│   │   │       │   ├── ReportController.java
│   │   │       │   └── SuperAdminController.java
│   │   │       ├── service/                          ← @Service — business logic
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── UserService.java
│   │   │       │   ├── StudentService.java
│   │   │       │   ├── DeviceService.java
│   │   │       │   ├── EventRequestService.java
│   │   │       │   ├── DeviceLogService.java
│   │   │       │   ├── AuditLogService.java
│   │   │       │   ├── ReportService.java
│   │   │       │   └── SuperAdminService.java
│   │   │       ├── dao/                              ← JDBC; RowMapper; PreparedStatement
│   │   │       │   ├── UserDAO.java
│   │   │       │   ├── StudentDAO.java
│   │   │       │   ├── DeviceDAO.java
│   │   │       │   ├── EventRequestDAO.java
│   │   │       │   ├── EventRequestDeviceDAO.java
│   │   │       │   ├── DeviceLogDAO.java
│   │   │       │   └── AuditLogDAO.java              ← calls fn_write_audit_log()
│   │   │       ├── model/                            ← POJOs + enums per DB table
│   │   │       │   ├── User.java
│   │   │       │   ├── Student.java
│   │   │       │   ├── Device.java
│   │   │       │   ├── EventRequest.java
│   │   │       │   ├── EventRequestDevice.java
│   │   │       │   ├── DeviceLog.java
│   │   │       │   ├── AuditLog.java
│   │   │       │   ├── AuditActionTypes.java
│   │   │       │   ├── enums/
│   │   │       │   │   ├── Role.java
│   │   │       │   │   ├── DeviceType.java
│   │   │       │   │   └── RegistrationStatus.java
│   │   │       │   └── report/                       ← DTO classes for report queries
│   │   │       │       ├── ActiveDeviceRow.java
│   │   │       │       ├── DailyTrafficRow.java
│   │   │       │       ├── DeviceFrequencyRow.java
│   │   │       │       ├── IncidentOverrideRow.java
│   │   │       │       ├── MonthlyTrafficRow.java
│   │   │       │       └── PendingRegistrationRow.java
│   │   │       ├── config/                           ← Spring configuration beans
│   │   │       │   ├── DataSourceConfig.java         ← HikariCP + Railway env vars
│   │   │       │   ├── CorsConfig.java               ← Allow requests from JavaFX host
│   │   │       │   └── SecurityConfig.java           ← Stateless Spring Security filter chain
│   │   │       ├── exception/                        ← Custom exceptions + global handler
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   ├── BusinessRuleException.java
│   │   │       │   ├── ForbiddenException.java
│   │   │       │   └── GlobalExceptionHandler.java   ← @ControllerAdvice
│   │   │       └── util/                             ← Stateless helpers
│   │   │           ├── PasswordUtil.java              ← BCrypt hash / verify
│   │   │           ├── ValidationUtil.java
│   │   │           └── DateUtil.java
│   │   └── resources/
│   │       ├── application.properties                ← Spring config (reads Railway env vars)
│   │       └── db/
│   │           └── schema.sql                        ← Full PostgreSQL schema (reference copy)
│   └── test/
│       └── java/com/pup/byod/javabyodbackend/       ← Unit / integration tests
├── pom.xml                                           ← Maven: Spring Boot, JDBC, PostgreSQL driver, BCrypt
├── Procfile                                          ← Railway start command
├── .gitignore
└── README.md
```

---

## 11. Toolchain Summary

| Tool | Used For | Notes |
|---|---|---|
| **IntelliJ IDEA** | Both repos | IDE for JavaFX (Community Edition) and Spring Boot (Ultimate) |
| **Git** | Both repos | Local version control; commit before every Railway deploy |
| **GitHub** | Both repos | Two separate repositories: byod-frontend and byod-backend |
| **Railway** | Backend + DB | Hosts Spring Boot JAR and PostgreSQL database; environment variables for JDBC credentials |
| **Maven (pom.xml)** | Both repos | Dependency management for JavaFX SDK, Spring Boot, PostgreSQL JDBC driver, BCrypt, Jackson |
