# BYOD Device Management System — System Architecture Document

`com.pup.byod.javafxbyodclient` | JavaFX + Spring Boot + PostgreSQL

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
| **users** | user_id SERIAL | Admin, guard, and super admin accounts (email address in `email` column, `status` allows `pending` for first login onboarding) |
| **students** | student_id VARCHAR(50) | Student registry — never hard-delete; set status = inactive |
| **requests** | request_id SERIAL | Unified header for normal and event device access requests (school events, orgs, individual BYOD) |
| **request_devices** | request_device_id SERIAL | Individual devices attached to a request |
| **device_transactions** | transaction_id SERIAL | Daily ingress/egress transactions. Max 1 transaction per device per day. |
| **audit_logs** | audit_id SERIAL | Immutable system-wide audit trail — write via fn_write_audit_log() only |
| **system_settings** | setting_key VARCHAR(100) | System settings and policy parameters |

### 6.2 Views

| View Name | Purpose |
|---|---|
| **v_device_campus_status** | Derives inside/outside status per approved active request device from the latest transaction log |
| **v_active_requests** | Active approved access requests with device counts |

### 6.3 Key Functions and Triggers

| Name | Purpose |
|---|---|
| **fn_write_audit_log()** | Preferred writer for audit_logs. Called from DAO layer only. Prevents direct INSERT. |
| **fn_set_updated_at()** | Auto-refreshes updated_at on every UPDATE across all mutable tables |
| **fn_force_created_at()** | Forces server-side created_at timestamp on device_transactions and audit_logs to prevent backdating |
| **fn_guard_device_transaction_approved_only()** | Blocks transactions for devices or requests that are not approved |
| **fn_audit_log_immutable()** | Prevents UPDATE and DELETE on audit_logs rows |
| **fn_protect_request_device_delete()** | Prevents deleting request devices if they have logs |
| **fn_protect_request_delete()** | Prevents deleting requests if they have active transactions |
| **fn_protect_student_delete()** | Blocks student deletion if they have active requests or transactions |
| **fn_protect_user_delete()** | Blocks user deletion if they have audit logs; requires setting status = inactive instead |

---

## 7. Request Flow (End-to-End)

The table below traces a single user action — e.g. a guard approving a device — from the JavaFX UI to the database and back.

| # | Layer | What Happens | Passes To Next |
|---|---|---|---|
| **1** | JavaFX UI (FXML) | User clicks button / submits form | Raw input to Controller |
| **2** | JavaFX Controller | Intercepts UI event, collects input, and delegates to service layer | Method call with inputs/DTOs |
| **3** | JavaFX Service/API Layer | Configures HTTP request, serializes request DTO to JSON, sends HTTP request via HttpClient | HTTPS request to backend |
| **4** | Spring Boot @RestController | Deserializes JSON to DTO / model, calls Service method | Method call + parameters |
| **5** | Spring Boot @Service | Validates business rules, calls DAO, calls AuditLogDAO | Model objects or primitives |
| **6** | DAO (JDBC) | Executes PreparedStatement, calls fn_write_audit_log() | SQL + parameters to DB |
| **7** | PostgreSQL (Railway) | Executes SQL, fires triggers, returns ResultSet / confirmation | ResultSet rows |
| **8** | DAO → Service → Controller | ResultSet mapped to Model POJOs; returned up the chain | ResponseEntity (JSON) |
| **9** | JavaFX Service/API Layer | Deserializes JSON response payload, handles connection issues/exceptions | Model object, list, or error |
| **10** | JavaFX Controller | Receives callback/response on JavaFX Application thread, updates UI binding (ObservableList) | UI update (TableView / Label) |

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

### 10.1 Backend Repository — Spring Boot (IntelliJ IDEA)

The backend follows the standard Spring Boot layered structure.

**Controllers (`controller/`)**

| Controller Class | Endpoints / Role |
|---|---|
| AuthController | POST /auth/login, POST /auth/logout, POST /auth/forgot-password, POST /auth/reset-password |
| UserController | GET /users, GET /users/{id}, PUT /users/{id}/profile/password |
| StudentController | GET/POST/PUT /students — student registry CRUD |
| RequestController | GET/POST/PUT /requests — unified normal/event request lifecycle |
| DeviceTransactionController | GET/POST /transactions — gate scan entry/exit processing and batch checking |
| AuditLogController | GET /audit-logs — read-only audit trail queries |
| ReportController | GET /reports/* — traffic summaries, missed checkouts, active devices, device frequency, incidents, purpose breakdown |
| SuperAdminController | POST/PUT /super-admin/* — manage admins/guards |
| SystemSettingController | GET/PUT /api/v1/settings — query and edit configurations |

**Services (`service/`)**

| Service Class | Business Logic Handled |
|---|---|
| AuthService | Login, password reset token generation, session management |
| UserService | Retrieve user list and details, update password with BCrypt |
| StudentService | Student registration, soft-delete, search, CSV import |
| RequestService | Unified request creation, updates, verification status |
| DeviceTransactionService | Gate scan logic (ingress/egress validation), reconciliation |
| AuditLogService | Orchestrates audit log writing |
| ReportService | Produces daily traffic, active, frequency, incident, missed checkout, and purpose breakdown reports |
| SuperAdminService | Account CRUD, status updates, role changes for guards and admins |
| SystemSettingService | Manage settings and configuration parameters |
| ResendEmailService | Sends account recovery tokens and gate notifications using the Resend API |

**DAOs (`dao/`)**

| DAO Class | DB Table / View Accessed |
|---|---|
| UserDAO | users table |
| StudentDAO | students table |
| RequestDAO | requests table, v_active_requests |
| RequestDeviceDAO | request_devices table, v_device_campus_status |
| DeviceTransactionDAO | device_transactions table |
| AuditLogDAO | Calls fn_write_audit_log() |
| SystemSettingDAO | system_settings table |

**Models (`model/`)**

| Model Class | Maps To |
|---|---|
| User | users table |
| Student | students table |
| Request | requests table |
| RequestDevice | request_devices table |
| DeviceTransaction | device_transactions table |
| ActiveRequest | v_active_requests view |
| DeviceCampusStatus | v_device_campus_status view |
| DeviceScanResponse | Gate scan response JSON mapping |
| AuditLog | audit_logs table |
| SystemSetting | system_settings table |

**Report Models (`model/report/`)**

| Model Class | Maps To |
|---|---|
| ActiveDeviceRow | Real-time snapshot of active devices on campus |
| DailyTrafficRow | Entry/exit events on a given day |
| DeviceFrequencyRow | Device entry/exit frequency over a date range |
| IncidentOverrideRow | Admin overrides, rejections, and dispute resolutions |
| MonthlyTrafficRow | Aggregated monthly traffic grouped by category & student |
| MissedCheckoutRow | Aggregated missed check-outs listing |
| PurposeBreakdownRow | Statistical breakdown of requests by purpose |
| TimeDiscrepancyRow | Audit discrepancies in check-in/out times |

**Enums (`model/enums/`)**

| Enum Class | Used For |
|---|---|
| Role | users.role — admin, guard, super_admin |
| RequestType | requests.request_type — normal, event |
| RequestStatus | requests.status — pending, approved, rejected, returned |
| DeviceVerificationStatus | request_devices.device_status — pending, approved, rejected |
| DeviceType | request_devices.device_type constraint values (Personal Computers, Components & Peripherals, Display & Projection, Project Prototypes (Optional SN), Appliances (TLE), Other) |
