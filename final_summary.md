# Backend Migration Final Summary: Request-Based System Overhaul

This document provides a comprehensive summary of the backend transition from a permanent device registry model to a unified request-based campus access model. All architectural updates have been implemented and verified. The codebase builds successfully, and the Spring Boot integration tests pass cleanly.

---

## 1. Architectural Accomplishments

1. **Decommissioned Permanent Registries**: Completely removed the permanent device registry database schema dependencies (`devices`, `device_logs`, `event_requests`, `event_request_devices`, `event_device_logs`).
2. **Unified Request System**: Merged individual student and event-based requests into a singular, cohesive `requests` table utilizing the `request_type` flag (`normal` or `event`).
3. **Daily Transaction Lifecycle**: Implemented gate entry and exit logging using the new `device_transactions` model. This structure allows exactly **one check-in and one check-out per device per day**.
4. **Automated Reconciliation**: Integrated a reconciliation step prior to processing new scans, which automatically flags unclosed transactions from prior days as `no_egress_marked = true`.
5. **Modernized Reporting**: Rewrote report query logic to extract metrics from `device_transactions` and `v_device_campus_status` view instead of legacy log tables. Integrated new Missed Checkout, Time Discrepancy, and Purpose Breakdown analytics.

---

## 2. Inventory of New and Modified Code Components

Below is the directory mapping of the components that were created, updated, or removed:

### A. New Database Domain Enums
* `model/enums/RequestType.java`: Maps normal and event request categories.
* `model/enums/RequestStatus.java`: Tracks the lifecycle of requests (`pending`, `approved`, `rejected`, `returned`).
* `model/enums/DeviceVerificationStatus.java`: Tracks individual device checks (`pending`, `approved`, `rejected`).

### B. Core Data Models
* `model/Request.java`: Maps the unified request headers (normal or event).
* `model/RequestDevice.java`: Represents student or event-associated devices.
* `model/DeviceTransaction.java`: Stores check-in/check-out events per day.
* `model/ActiveRequest.java`: Map to `v_active_requests` database view.
* `model/DeviceCampusStatus.java`: Updated to map to the new `v_device_campus_status` view.
* `model/DeviceScanResponse.java`: Gateway scan response payload.

### C. Data Access Objects (JDBC Layer)
* `dao/RequestDAO.java`: Handles CRUD for individual/event requests.
* `dao/RequestDeviceDAO.java`: Handles request-device membership.
* `dao/DeviceTransactionDAO.java`: Performs gate check-in/check-out, unclosed scan reconciliation, and report querying.
* `dao/AuditLogDAO.java`: Refactored to map the new action types in the incident override view.

### D. Service Layer
* `service/RequestService.java`: Encapsulates validations (e.g., date checks, active student checks) and lifecycle updates.
* `service/DeviceTransactionService.java`: Core gate scan controller handling ingress check-in, egress check-out, and auto-marking missed exits.
* `service/ReportService.java`: Reconfigured to pull metrics for daily traffic, active devices, missed checkouts, time discrepancies, and purpose breakdowns.

### E. Controllers (REST API Layer)
* `controller/RequestController.java`: Endpoints to create, update, review, and query student/event requests.
* `controller/DeviceTransactionController.java`: Entry/exit gate scan, batch ingress/egress, and reconciliation triggers.
* `controller/ReportController.java`: Reporting endpoints updated to return the new model DTOs.

### F. Refactored Legacy Models
* `model/report/DailyTrafficRow.java`: Converted timestamp field to `LocalDateTime`.
* `model/report/DeviceFrequencyRow.java`: Updated timestamps to `LocalDateTime`.
* `model/report/ActiveDeviceRow.java`: Updated entry timestamp to `LocalDateTime`.
* `model/report/PendingRegistrationRow.java`: Updated submission timestamp to `LocalDateTime`.
* `model/AuditActionTypes.java`: Extended to include constants like `REQUEST_CREATED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_RETURNED`, `DEVICE_VERIFIED`.

---

## 3. Compilation & Build Verification

The project compiles and tests pass cleanly under the target configuration:
* **JDK Version**: Java 21
* **Build Tool**: Maven (`.\mvnw.cmd`)
* **Compilation Command**: `.\mvnw.cmd clean compile`
* **Test Suite Command**: `.\mvnw.cmd test`
* **Test Status**: `BUILD SUCCESS` (0 failures, 0 errors, 1 test run).

To bypass local PostgreSQL environment variable requirements during testing, `JavaByodBackendApplicationTests` uses a mocked `DataSource` configuration context mapping to a sandbox environment, ensuring high code reliability and isolation.

---

## 4. Database Setup & Migrations

Before launching the backend, apply the migrations in the database using the scripts in `src/main/resources/db/`:
1. Check that the base table schema exists using `schema.sql`.
2. Apply the requests overhaul schema using `migration_requests_overhaul.sql` to instantiate the new tables, triggers (`fn_write_audit_log`), and views (`v_device_campus_status`, `v_active_requests`).
