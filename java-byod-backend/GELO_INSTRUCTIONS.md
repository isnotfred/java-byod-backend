# GELO_INSTRUCTIONS

This guide is for Gelo only. It explains the exact files you need to implement and how to integrate with the backend infrastructure that is already in place.

## 1. What is already done

The following foundation has already been completed:

- `application.properties` and `DataSourceConfig.java` are wired for PostgreSQL/HikariCP
- `CorsConfig.java` is configured for JavaFX client access
- `UserDAO`, `AuditLogDAO`, `PasswordUtil`, `ValidationUtil`, `ResourceNotFoundException`, `BusinessRuleException`, and `GlobalExceptionHandler` are implemented
- Core `AuthService`, `UserService`, `AuditLogService`, `AuthController`, `UserController`, and `AuditLogController` are now available
- Event request and device log DAOs are implemented and ready for the service layer

## 2. Your ownership

You own these files and features only:

### Enums
- `src/main/java/com/pup/byod/javabyodbackend/model/enums/DeviceType.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/enums/RegistrationStatus.java`

### Models
- `src/main/java/com/pup/byod/javabyodbackend/model/Student.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/Device.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/PendingDevice.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/DeviceCampusStatus.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/DeviceLog.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/EventRequest.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/EventRequestDevice.java`
- `src/main/java/com/pup/byod/javabyodbackend/model/ActiveEventRequest.java`

### Services
- `src/main/java/com/pup/byod/javabyodbackend/service/StudentService.java`
- `src/main/java/com/pup/byod/javabyodbackend/service/DeviceService.java`
- `src/main/java/com/pup/byod/javabyodbackend/service/EventRequestService.java`
- `src/main/java/com/pup/byod/javabyodbackend/service/DeviceLogService.java`

### Controllers
- `src/main/java/com/pup/byod/javabyodbackend/controller/StudentController.java`
- `src/main/java/com/pup/byod/javabyodbackend/controller/DeviceController.java`
- `src/main/java/com/pup/byod/javabyodbackend/controller/EventRequestController.java`
- `src/main/java/com/pup/byod/javabyodbackend/controller/DeviceLogController.java`

## 3. How to work safely

### Follow the project architecture order

Work in this sequence because each layer depends on the previous one:

1. Enums
2. Models
3. DAOs (already done for event requests and logs)
4. Services
5. Controllers

### Do not modify shared files without coordination

Do not change these files unless we both agree:

- `pom.xml`
- `JavaByodBackendApplication.java`
- `src/main/resources/db/schema.sql`
- `application.properties`

### Use the database schema as the source of truth

Your models should map directly to the schema in `src/main/resources/db/schema.sql`:

- `event_requests` → `EventRequest`
- `event_request_devices` → `EventRequestDevice`
- `device_logs` → `DeviceLog`
- `v_active_event_requests` → `ActiveEventRequest`
- `v_device_campus_status` → `DeviceCampusStatus`
- `v_pending_devices` → `PendingDevice`

### Reuse existing helpers

- Use `ValidationUtil` for input validation.
- Use `PasswordUtil` only for password hashing and verification.
- Throw `BusinessRuleException` for invalid business logic.
- Throw `ResourceNotFoundException` when a record is missing.
- Use `AuditLogService` to write audit entries in service operations.

## 4. API integration points

The backend currently exposes these base routes for the core user and audit paths:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `PUT /api/v1/users/{id}/deactivate`
- `GET /api/v1/audit-logs`
- `GET /api/v1/audit-logs/user/{userId}`

Your controllers should use the same prefix style with `/api/v1/`.

## 5. What Gelo should implement first

Start with the models and enums that are needed for the event/device flow:

- `DeviceType`
- `RegistrationStatus`
- `Student`
- `Device`
- `PendingDevice`
- `DeviceCampusStatus`
- `ActiveEventRequest`
- `DeviceLog`
- `EventRequest`
- `EventRequestDevice`

Then implement the service logic using the existing DAO layer and `AuditLogService`.

## 6. What to build in each service

### StudentService
- Register students
- Enforce soft-delete by updating `status` to `inactive`
- Provide search by student ID or name

### DeviceService
- Register devices
- Approve / reject pending registrations
- Deactivate devices safely
- Use `DeviceDAO` for `v_pending_devices` and `v_device_campus_status`

### EventRequestService
- Submit event requests and event request devices
- Approve / return / reject requests
- Validate date ranges and required approval documents

### DeviceLogService
- Log `entry` and `exit` events
- Prevent consecutive same-type events
- Support `auto_exit` reconciliation for missed scans

## 7. Final note

If you need to add new helper methods or DTOs, keep them in the same package layer and avoid introducing additional dependencies in `pom.xml` unless absolutely necessary.

If anything is unclear, ask before changing `pom.xml` or the schema.
