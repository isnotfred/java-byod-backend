# Java BYOD Backend

Backend API for a Bring Your Own Device (BYOD) device management system for PUP. The application manages administrative users, student records, registered devices, event-based temporary device requests, gate entry/exit logs, campus device status, and audit logs.

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
|-- controller/   REST API controllers
|-- dao/          JDBC data access layer
|-- exception/    Global API exception handling
|-- model/        Domain models and enums
|-- service/      Business logic
`-- util/         Validation, password, and date helpers

src/main/resources
|-- application.properties
`-- db/schema.sql
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
/api/v1
```

Request and response bodies use JSON unless otherwise noted. Timestamps are returned in ISO-like string format from Java date/time values.

Common error response:

```json
{
  "timestamp": "2026-06-05T16:30:00.000",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Device is not approved and cannot be logged."
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
| User role | `admin`, `guard` |
| User status | `active`, `inactive` |
| Student status | `active`, `inactive` |
| Device type | `laptop`, `tablet`, `phone` |
| Device purpose | `Academic BYOD`, `School Event`, `Organization Activity`, `Temporary Equipment`, `Other Approved Purpose` |
| Device registration status | `pending`, `approved`, `rejected` |
| Device status | `active`, `inactive` |
| Event request status | `pending`, `approved`, `returned`, `rejected` |
| Event approval document type | `Paper Approval`, `Signed GPOA` |
| Event request device type | `laptop`, `tablet`, `phone`, `camera`, `projector`, `other` |
| Event request device status | `pending`, `approved`, `returned` |
| Device log event type | `entry`, `exit` |
| Device log logout type | `manual`, `automatic` |

## Endpoints

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Authenticate a user by username and password |
| `POST` | `/api/v1/auth/logout` | Record a logout audit event for a user |
| `POST` | `/api/v1/auth/forgot-password` | Request password reset token via email |
| `POST` | `/api/v1/auth/reset-password` | Complete password reset using token |

#### `POST /api/v1/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "password123"
}
```

Response: authenticated user object with `passwordHash` omitted.

#### `POST /api/v1/auth/logout`

Request:

```json
{
  "userId": 1
}
```

Response:

```json
{
  "message": "Logout successful."
}
```

#### `POST /api/v1/auth/forgot-password`

Request:

```json
{
  "email": "user@example.com"
}
```

Response:

```json
{
  "message": "Password reset token sent to your email if the account exists."
}
```

#### `POST /api/v1/auth/reset-password`

Request:

```json
{
  "token": "4a2c918a-bbcf-4a37-9efb-665e89d12345",
  "newPassword": "NewSecurePassword123"
}
```

Response:

```json
{
  "message": "Password reset successful."
}
```

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/users` | List all users |
| `GET` | `/api/v1/users/{id}` | Get a user by ID |
| `POST` | `/api/v1/users` | Create an admin or guard user |
| `PUT` | `/api/v1/users/{id}` | Update a user's name, role, and status |
| `PUT` | `/api/v1/users/{id}/deactivate` | Deactivate a user |

#### `POST /api/v1/users`

```json
{
  "username": "guard01",
  "password": "password123",
  "fullName": "Gate Guard",
  "role": "guard"
}
```

#### `PUT /api/v1/users/{id}`

```json
{
  "fullName": "Gate Guard",
  "role": "guard",
  "status": "active"
}
```

### Students

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/students` | List all students |
| `GET` | `/api/v1/students/{studentId}` | Get a student by student ID |
| `GET` | `/api/v1/students/search?keyword={keyword}` | Search students by keyword |
| `POST` | `/api/v1/students` | Create a student record |
| `PUT` | `/api/v1/students/{studentId}` | Update a student record |
| `PUT` | `/api/v1/students/{studentId}/deactivate` | Deactivate a student |
| `POST` | `/api/v1/students/import` | Import students from a CSV file |

#### `POST /api/v1/students`

```json
{
  "studentId": "2024-00001",
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "courseYearLevel": "BSIT 2-1"
}
```

#### `PUT /api/v1/students/{studentId}`

```json
{
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "courseYearLevel": "BSIT 2-1",
  "status": "active"
}
```

#### `POST /api/v1/students/import`

Content type: `multipart/form-data`

| Form field | Type | Description |
| --- | --- | --- |
| `file` | CSV file | Student import file |

Expected CSV headers:

```csv
student_id,first_name,last_name,course_year_level
2024-00001,Juan,Dela Cruz,BSIT 2-1
```

Response:

```json
{
  "inserted": 1,
  "skipped": 0,
  "errors": []
}
```

### Devices

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/devices` | List all registered devices |
| `GET` | `/api/v1/devices/{deviceId}` | Get a device by ID |
| `GET` | `/api/v1/devices/serial/{serialNumber}` | Get a device by serial number |
| `GET` | `/api/v1/devices/student/{studentId}` | List devices owned by a student |
| `GET` | `/api/v1/devices/pending` | List pending device registrations |
| `GET` | `/api/v1/devices/campus-status` | List current campus status for devices |
| `GET` | `/api/v1/devices/campus-status/{serialNumber}` | Get campus status for one device |
| `POST` | `/api/v1/devices` | Register a device |
| `PUT` | `/api/v1/devices/{deviceId}` | Update editable device details |
| `PUT` | `/api/v1/devices/{deviceId}/approve` | Approve a pending device |
| `PUT` | `/api/v1/devices/{deviceId}/reject` | Reject a pending device |
| `PUT` | `/api/v1/devices/{deviceId}/deactivate` | Deactivate a device |

#### `POST /api/v1/devices`

```json
{
  "studentId": "2024-00001",
  "deviceName": "Juan's Laptop",
  "brand": "Lenovo",
  "model": "ThinkPad E14",
  "serialNumber": "ABC123456",
  "deviceType": "laptop",
  "devicePurpose": "Academic BYOD",
  "registrationStatus": "pending",
  "remarks": "For class use",
  "imagePath": "/uploads/devices/ABC123456.jpg"
}
```

If `registrationStatus` is omitted, the service defaults to `approved`.

#### `PUT /api/v1/devices/{deviceId}`

```json
{
  "deviceName": "Juan's Laptop",
  "brand": "Lenovo",
  "model": "ThinkPad E14",
  "devicePurpose": "Academic BYOD",
  "remarks": "Updated details",
  "imagePath": "/uploads/devices/ABC123456.jpg"
}
```

#### `PUT /api/v1/devices/{deviceId}/approve`

```json
{
  "reviewedBy": 1
}
```

#### `PUT /api/v1/devices/{deviceId}/reject`

```json
{
  "reviewedBy": 1,
  "remarks": "Serial number could not be verified."
}
```

### Device Logs

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/device-logs/devices/{deviceId}?limit=50&offset=0` | List logs for a device |
| `GET` | `/api/v1/device-logs/students/{studentId}?limit=50&offset=0` | List logs for a student |
| `POST` | `/api/v1/device-logs/entry` | Record a manual device entry scan |
| `POST` | `/api/v1/device-logs/exit` | Record a manual device exit scan |
| `POST` | `/api/v1/device-logs/auto-exit` | Run automatic exit reconciliation for devices still inside campus |

Manual entry and exit require an approved, active device. Consecutive duplicate events are rejected; for example, a second `entry` cannot be logged until an `exit` exists.

#### `POST /api/v1/device-logs/entry`

```json
{
  "serialNumber": "ABC123456",
  "handledBy": 2,
  "notes": "Main gate entry"
}
```

#### `POST /api/v1/device-logs/exit`

```json
{
  "serialNumber": "ABC123456",
  "handledBy": 2,
  "notes": "Main gate exit"
}
```

### Event Requests

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/event-requests` | List all event requests |
| `GET` | `/api/v1/event-requests/{eventRequestId}` | Get an event request by ID |
| `GET` | `/api/v1/event-requests/active` | List currently active event requests |
| `GET` | `/api/v1/event-requests/{eventRequestId}/devices` | List devices attached to an event request |
| `POST` | `/api/v1/event-requests` | Create an event request with device line items |
| `PUT` | `/api/v1/event-requests/{eventRequestId}/approve` | Approve an event request |
| `PUT` | `/api/v1/event-requests/{eventRequestId}/return` | Return an event request for revision |
| `PUT` | `/api/v1/event-requests/{eventRequestId}/reject` | Reject an event request |
| `PUT` | `/api/v1/event-requests/devices/{eventDeviceId}/verify` | Verify or update the status of an event request device |

#### `POST /api/v1/event-requests`

```json
{
  "studentId": "2024-00001",
  "responsiblePerson": "Juan Dela Cruz",
  "organization": "Computer Society",
  "eventName": "Tech Expo",
  "eventPurpose": "Organization Activity",
  "approvalDocType": "Signed GPOA",
  "approvalDocRef": "GPOA-2026-001",
  "startDate": "2026-06-10",
  "endDate": "2026-06-12",
  "isSubmitted": true,
  "isAccommodated": false,
  "remarks": "For event booth setup",
  "lineItems": [
    {
      "deviceName": "Camera",
      "brand": "Canon",
      "model": "EOS 80D",
      "deviceType": "camera",
      "serialNumber": "CAM123456",
      "quantity": 1,
      "remarks": "For documentation"
    }
  ]
}
```

Each event request must include at least one `lineItems` entry. `approvalDocType` must be either `Paper Approval` or `Signed GPOA`. `endDate` must not be earlier than `startDate`.

#### `PUT /api/v1/event-requests/{eventRequestId}/approve`

```json
{
  "reviewerUserId": 1
}
```

#### `PUT /api/v1/event-requests/{eventRequestId}/return`

```json
{
  "reviewerUserId": 1,
  "remarks": "Please attach the correct approval document."
}
```

#### `PUT /api/v1/event-requests/{eventRequestId}/reject`

```json
{
  "reviewerUserId": 1,
  "remarks": "Request does not meet event access requirements."
}
```

#### `PUT /api/v1/event-requests/devices/{eventDeviceId}/verify`

```json
{
  "verifiedBy": 2,
  "deviceStatus": "approved"
}
```

### Audit Logs

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/audit-logs?limit=50&offset=0` | List audit logs with pagination |
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
EVENT_REQUEST_CREATED
EVENT_REQUEST_APPROVED
EVENT_REQUEST_RETURNED
EVENT_REQUEST_REJECTED
USER_CREATED
USER_UPDATED
USER_DEACTIVATED
USER_LOGIN
USER_LOGOUT
USER_LOGIN_FAILED
SYSTEM_AUTO_EXIT_BATCH
```

## Business Rules

- Students, users, and devices are deactivated instead of hard-deleted.
- Registered device serial numbers must be unique and contain no whitespace.
- Only approved and active devices can be logged through gate entry or exit endpoints.
- Manual device logs require a `handledBy` user ID.
- Automatic exit logs are system-generated and do not require a handler.
- Device registration status cannot move directly from `approved` to `rejected`; deactivate the device instead.
- Device registration status cannot move directly from `rejected` to `approved`; it must return to `pending` first.
- Event requests require at least one device line item.
- Audit logs and device logs are designed as append-only records.

## Notes for API Consumers

- All endpoints are currently accessible without bearer tokens or session cookies because the security filter permits all requests.
- Login and logout still write audit records and should be used by client applications for accountability.
- The API omits `passwordHash` in user responses returned by controllers.
- For paginated endpoints, `limit` defaults to `50` and `offset` defaults to `0`.
- CSV student import only accepts files with a `.csv` extension.
