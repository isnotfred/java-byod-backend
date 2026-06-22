# Java BYOD Backend

Backend API for a Bring Your Own Device (BYOD) device management system for PUP. The application manages administrative users, student records, access requests (both individual and event-based), gate entry/exit transactions, campus device status, and audit logs.

The project is a Spring Boot REST API backed by PostgreSQL. It is designed to support a desktop client, such as a JavaFX application, over JSON HTTP endpoints.

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring JDBC with `NamedParameterJdbcTemplate`
- Spring Security
- PostgreSQL
- Maven
- Apache Commons CSV
- Lombok

## Project Structure

```text
src/main/java/com/pup/byod/javabyodbackend
|-- config/       Application configuration for security, CORS, and datasource
|-- controller/   REST API controllers (RequestController, DeviceTransactionController, etc.)
|-- dao/          JDBC data access layer (RequestDAO, DeviceTransactionDAO, etc.)
|-- exception/    Global API exception handling
|-- model/        Domain models and enums (Request, RequestDevice, DeviceTransaction, etc.)
|-- service/      Business logic (RequestService, DeviceTransactionService, etc.)
`-- util/         Validation, password, and date helpers

src/main/resources
|-- application.properties
`-- db/
    |-- schema.sql                       Reference database schema (up-to-date)
    |-- migration_requests_overhaul.sql  Migration script to apply requests overhaul
```

## Requirements

- JDK 21 or later
- Maven, or the included Maven Wrapper
- PostgreSQL database

## Configuration

The application reads PostgreSQL connection details from environment variables.

| Variable | Description |
| --- | --- |
| `PGHOST` | PostgreSQL host |
| `PGPORT` | PostgreSQL port |
| `PGDATABASE` | PostgreSQL database name |
| `PGUSER` | PostgreSQL username |
| `PGPASSWORD` | PostgreSQL password |

Default server port:

```properties
server.port=8080
```

Database schema creation is manual:

```properties
spring.sql.init.mode=never
```

Before running the application for the first time, apply the schema:

```bash
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -f src/main/resources/db/schema.sql
```

On Windows PowerShell:

```powershell
psql -h $env:PGHOST -p $env:PGPORT -U $env:PGUSER -d $env:PGDATABASE -f src/main/resources/db/schema.sql
```

## Running Locally

Using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

## Testing

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

## Authentication and Security

The API includes login and logout endpoints for admin and guard users. Passwords are verified using BCrypt.

Current security configuration disables CSRF, uses stateless sessions, and permits all HTTP requests. The login endpoint returns the authenticated user object with `passwordHash` removed from the response.

## API Conventions

Base URL:

```text
/api
```

Request and response bodies use JSON unless otherwise noted. Timestamps are returned in ISO-like string format from Java date/time values.

Common error response:

```json
{
  "timestamp": "2026-06-22T11:50:00.000",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Device status is not approved. Cannot log transaction."
}
```

Common status codes:

| Status | Meaning |
| --- | --- |
| `200 OK` | Request completed successfully |
| `201 Created` | Resource created successfully |
| `400 Bad Request` | Validation or database trigger error |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Duplicate unique value |
| `422 Unprocessable Entity` | Business rule violation |
| `500 Internal Server Error` | Unexpected server error |

## Allowed Values

| Field | Values |
| --- | --- |
| User role | `admin`, `guard`, `super_admin` |
| User status | `active`, `inactive`, `pending` |
| Student status | `active`, `inactive` |
| Request type | `normal`, `event` |
| Request status | `pending`, `approved`, `rejected`, `returned` |
| Request device type | `Personal Computers`, `Components & Peripherals`, `Display & Projection`, `Project Prototypes (Optional SN)`, `Appliances (TLE)`, `Other` |
| Request device status | `pending`, `approved`, `rejected` |
| Request document type | `Paper Approval`, `Signed GPOA` |

---

## Endpoints

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Authenticate a user by username and password |
| `POST` | `/api/v1/auth/logout` | Record a logout audit event for a user |
| `POST` | `/api/v1/auth/forgot-password` | Request password reset token via email |
| `POST` | `/api/v1/auth/reset-password` | Complete password reset using token |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/users` | List all users |
| `GET` | `/api/v1/users/{id}` | Get a user by ID |
| `POST` | `/super-admin/users` | Create an admin or guard user (Super Admin only) |
| `PUT` | `/super-admin/users/{id}` | Update a user's status |
| `PUT` | `/super-admin/users/{id}/deactivate` | Deactivate a user |
| `PUT` | `/super-admin/users/{id}/role` | Change a user's system role |

### Students

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/students` | List all students |
| `GET` | `/api/v1/students/{studentId}` | Get a student by student ID |
| `GET` | `/api/v1/students/search?keyword={keyword}` | Search students by keyword |
| `POST` | `/api/v1/students` | Create a student record |
| `PUT` | `/api/v1/students/{studentId}` | Update a student record |
| `PUT` | `/api/v1/students/{studentId}/deactivate` | Deactivate a student (soft-delete) |
| `POST` | `/api/v1/students/import` | Import students from a CSV file |

### Access Requests

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/requests` | List all normal and event access requests |
| `GET` | `/api/requests/{requestId}` | Get a request by ID |
| `GET` | `/api/requests/student/{studentId}` | Get requests submitted by student |
| `GET` | `/api/requests/active` | Get active approved requests with device counts |
| `GET` | `/api/requests/{requestId}/devices` | Get devices attached to a request |
| `GET` | `/api/requests/campus-status` | Get campus presence status of request devices |
| `GET` | `/api/requests/campus-status/{serialNumber}` | Get campus status for one device by serial |
| `POST` | `/api/requests` | Create an access request with device list |
| `PUT` | `/api/requests/{requestId}/approve` | Approve a request (Admin only) |
| `PUT` | `/api/requests/{requestId}/reject` | Reject a request (Admin only) |
| `PUT` | `/api/requests/{requestId}/return` | Return a request for corrections (Admin only) |
| `PUT` | `/api/requests/devices/{requestDeviceId}/verify` | Verify status of an individual device |

### Daily Gate Transactions

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/transactions/scan` | Scan device at gate (auto check-in / check-out) |
| `POST` | `/api/transactions/batch-ingress` | Batch check-in devices |
| `POST` | `/api/transactions/batch-egress` | Batch check-out devices |
| `POST` | `/api/transactions/reconcile` | Manually run nightly missed check-out batch |
| `GET` | `/api/transactions/device/{requestDeviceId}` | Query transactions history for a device |
| `GET` | `/api/transactions/{transactionId}` | Get single transaction by ID |

### Audit Logs

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/audit-logs` | List audit logs with pagination |
| `GET` | `/api/v1/audit-logs/user/{userId}` | List audit logs by user ID |
| `GET` | `/api/v1/audit-logs/action/{actionType}` | List audit logs by action type |

Known audit action types:

```text
DEVICE_REGISTERED
DEVICE_APPROVED
DEVICE_REJECTED
DEVICE_DEACTIVATED
DEVICE_UPDATED
DEVICE_ENTRY
DEVICE_EXIT
DEVICE_AUTO_EXIT
STUDENT_CREATED
STUDENT_UPDATED
STUDENT_DEACTIVATED
REQUEST_CREATED
REQUEST_APPROVED
REQUEST_REJECTED
REQUEST_RETURNED
DEVICE_VERIFIED
USER_CREATED
USER_UPDATED
USER_DEACTIVATED
USER_LOGIN
USER_LOGOUT
USER_LOGIN_FAILED
SYSTEM_AUTO_EXIT_BATCH
ADMIN_CREATED
ADMIN_UPDATED
ADMIN_DEACTIVATED
GUARD_CREATED
GUARD_UPDATED
GUARD_DEACTIVATED_BY_SUPER
USER_ROLE_CHANGED
SYSTEM_CONFIG_UPDATED
```

## Business Rules

- Students, users, and requests are deactivated/archived instead of hard-deleted.
- Every device scan is checked against active approved requests.
- A device can have **at most one ingress and one egress transaction per calendar day**.
- Gate scanning automatically reconciles previous days, marking open scans as missed checkouts (`no_egress_marked = true`).
- Devices can only check in/out if both the parent request and the device are marked as `approved`.
- Audit logs are immutable records written via system triggers and database routines.
