# Backend Redesign & Migration Guidelines: Request-Based System

This guide outlines the backend changes required to transition from the old permanent device registry to the new request-based campus access model. Share this document with your backend developers to guide their implementation.

---

## 1. Architectural Changes Overview

In the new architecture:
1. **No Permanent Devices**: The `devices` table is replaced entirely by `request_devices`. Every device must belong to an approved `request` that is active for a given date range.
2. **Unified Requests**: Individual (normal) and Group (event) requests are merged into the `requests` table, using `request_type = 'normal'` or `request_type = 'event'`.
3. **Daily Transactions**: Ingress/egress logs are tracked daily via `device_transactions` which allows exactly **one entry and one exit per device per day**.
4. **Auto-Exit Replaced**: There is no automatic checkout. Instead, unchecked-out devices from previous days are marked with `no_egress_marked = true` upon a new scan or via a nightly system job.

---

## 2. Models & Enums Updates (`model/`)

### A. Remove Old Models
Delete the following classes:
* `com.pup.byod.javabyodbackend.model.Device`
* `com.pup.byod.javabyodbackend.model.DeviceLog`
* `com.pup.byod.javabyodbackend.model.EventRequest`
* `com.pup.byod.javabyodbackend.model.EventRequestDevice`
* `com.pup.byod.javabyodbackend.model.EventDeviceLog`

### B. Add/Modify Enums (`model/enums/`)
Create or update enums:
* `RequestType`: `NORMAL`, `EVENT`
* `RequestStatus`: `PENDING`, `APPROVED`, `REJECTED`, `RETURNED`
* `DeviceVerificationStatus`: `PENDING`, `APPROVED`, `REJECTED`

### C. Create New Models

#### `Request.java`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    private Integer requestId;
    private RequestType requestType;
    private String studentId;
    
    // Event-specific details (can be null for individual/normal requests)
    private String eventName;
    private String organization;
    private String responsiblePerson;
    private String approvalDocType;
    private String approvalDocRef;
    
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime expectedIngressTime;
    private LocalTime expectedEgressTime;
    
    private RequestStatus status;
    private boolean isSubmitted;
    private boolean isAccommodated;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String remarks;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### `RequestDevice.java`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDevice {
    private Integer requestDeviceId;
    private Integer requestId;
    private String deviceName;
    private String brand;
    private String model;
    private String deviceType;
    private String serialNumber;
    private int quantity;
    private String imagePath;
    
    private DeviceVerificationStatus deviceStatus;
    private Integer verifiedBy;
    private LocalDateTime verifiedAt;
    private String remarks;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### `DeviceTransaction.java`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTransaction {
    private Integer transactionId;
    private Integer requestDeviceId;
    private LocalDate logDate;
    
    private LocalDateTime ingressTime;
    private Integer ingressHandledBy;
    
    private LocalDateTime egressTime;
    private Integer egressHandledBy;
    
    private boolean noEgressMarked;
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 3. Data Access Layer Updates (`dao/`)

### A. Remove Old DAOs
Delete these classes:
* `DeviceDAO.java`
* `EventRequestDAO.java`
* `EventRequestDeviceDAO.java`
* `EventDeviceLogDAO.java`
* `DeviceLogDAO.java`

### B. Create New DAOs

#### `RequestDAO.java`
Responsible for query operations on `requests`.
* `insert(Request request)` (uses `GeneratedKeyHolder` for `request_id`).
* `update(Request request)` (allows modifying details of normal or event requests, satisfying Requirement 8).
* `findById(Integer requestId)`.
* `findByStudentIdAndDateRange(String studentId, LocalDate date)`.
* `findPendingRequests()` (queries `v_pending_requests`).
* `findActiveRequests()` (queries `v_active_requests`).

#### `RequestDeviceDAO.java`
* `insert(RequestDevice device)`.
* `update(RequestDevice device)`.
* `delete(Integer requestDeviceId)`.
* `findByRequestId(Integer requestId)`.
* `findByRequestAndSerialNumber(Integer requestId, String serialNumber)`.

#### `DeviceTransactionDAO.java`
* `insert(DeviceTransaction transaction)` (invoked at check-in).
* `update(DeviceTransaction transaction)` (invoked at check-out or when marking no-egress).
* `findByDeviceAndDate(Integer requestDeviceId, LocalDate date)`.
* `findActiveInsideCampus()` (queries `v_device_campus_status` where status = `'entry'`).
* `markUnclosedTransactionsAsMissed(Integer requestDeviceId, LocalDate beforeDate)`:
  ```sql
  UPDATE device_transactions
  SET no_egress_marked = true, updated_at = CURRENT_TIMESTAMP
  WHERE request_device_id = :requestDeviceId
    AND egress_time IS NULL
    AND no_egress_marked = false
    AND log_date < :beforeDate
  ```

---

## 4. Service & Gate Scan Business Logic (`service/`)

### A. Create `RequestService.java`
Merges normal and event request validation and approvals.
* **Validation on Creation**:
  * `startDate` must be on or after `LocalDate.now()`.
  * `endDate` must be on or after `startDate`.
  * Verify `studentId` exists in `students` table and status is `active`.
  * `expectedIngressTime` and `expectedEgressTime` must not be null.
* **Editing Requests** (Requirement 8):
  * Requests can only be edited if they are in `PENDING` or `RETURNED` status.
  * Changing details should reset status to `PENDING` if it was `RETURNED`.

### B. Create `DeviceTransactionService.java` (Gate Scanning)
This is the core scan processing service.

```java
@Service
@RequiredArgsConstructor
public class DeviceTransactionService {

    private final RequestDAO requestDAO;
    private final RequestDeviceDAO requestDeviceDAO;
    private final DeviceTransactionDAO transactionDAO;
    private final AuditLogService auditLogService;

    @Transactional
    public DeviceScanResponse processGateScan(String studentId, String serialNumber, Integer guardUserId) {
        LocalDate today = LocalDate.now();

        // 1. Locate an approved request for the student valid for today
        List<Request> activeRequests = requestDAO.findActiveRequestsForStudent(studentId, today);
        if (activeRequests.isEmpty()) {
            throw new BusinessRuleException("No active approved requests found for student ID: " + studentId + " today.");
        }
        
        // Take the first matching request (should ideally be unique)
        Request activeRequest = activeRequests.get(0);

        // 2. Locate the approved device in the request matching the serial number
        Optional<RequestDevice> optDevice = requestDeviceDAO.findByRequestAndSerialNumber(
                activeRequest.getRequestId(), serialNumber);
        
        if (optDevice.isEmpty() || optDevice.get().getDeviceStatus() != DeviceVerificationStatus.APPROVED) {
            throw new BusinessRuleException("Device with Serial Number: " + serialNumber + " is not approved for this request.");
        }
        
        RequestDevice device = optDevice.get();

        // 3. Reconcile past days: close any open scans prior to today
        transactionDAO.markUnclosedTransactionsAsMissed(device.getRequestDeviceId(), today);

        // 4. Retrieve or create today's transaction
        Optional<DeviceTransaction> optTx = transactionDAO.findByDeviceAndDate(device.getRequestDeviceId(), today);

        if (optTx.isEmpty()) {
            // ================= INGRESS (CHECK-IN) =================
            DeviceTransaction newTx = DeviceTransaction.builder()
                    .requestDeviceId(device.getRequestDeviceId())
                    .logDate(today)
                    .ingressTime(LocalDateTime.now())
                    .ingressHandledBy(guardUserId)
                    .noEgressMarked(false)
                    .build();
            
            transactionDAO.insert(newTx);
            
            // Audit Log
            auditLogService.writeAuditLog(
                    guardUserId, 
                    "DEVICE_ENTRY", 
                    "request_devices", 
                    device.getRequestDeviceId().toString(), 
                    null, 
                    JsonUtil.toJson(newTx)
            );

            return new DeviceScanResponse("CHECK_IN_SUCCESS", "Device scanned IN successfully.", device);
            
        } else {
            DeviceTransaction tx = optTx.get();
            
            if (tx.getEgressTime() != null) {
                // Already checked out today. Block since max 1 transaction per day is allowed.
                throw new BusinessRuleException("Device already checked OUT today. Only 1 ingress/egress is allowed per day.");
            }
            
            if (tx.isNoEgressMarked()) {
                throw new BusinessRuleException("Today's transaction was marked as MISSED egress. Cannot perform further scans.");
            }

            // ================= EGRESS (CHECK-OUT) =================
            tx.setEgressTime(LocalDateTime.now());
            tx.setEgressHandledBy(guardUserId);
            
            transactionDAO.update(tx);
            
            // Audit Log
            auditLogService.writeAuditLog(
                    guardUserId, 
                    "DEVICE_EXIT", 
                    "request_devices", 
                    device.getRequestDeviceId().toString(), 
                    null, 
                    JsonUtil.toJson(tx)
            );

            return new DeviceScanResponse("CHECK_OUT_SUCCESS", "Device scanned OUT successfully.", device);
        }
    }
}
```

---

## 5. Controller Layer Updates (`controller/`)

### A. New endpoints under `/api/v1/requests`
* `POST /api/v1/requests` - Submit a new normal or event request along with its device list (pre-approved or created directly as active).
* `PUT /api/v1/requests/{id}` - Modify request header and devices (supported for active/approved requests).
* `GET /api/v1/requests/active` - Fetch approved, active requests.

### B. Gate Scan Endpoint under `/api/v1/device-transactions`
* `POST /api/v1/device-transactions/scan`
  * Payload:
    ```json
    {
      "studentId": "2023-00123-MN-0",
      "serialNumber": "SN-XYZ-789"
    }
    ```
  * Performs the entry/exit transaction and returns check-in/check-out success details.

---

## 6. Reporting Layer Overhaul (`controller/ReportController.java` & `service/ReportService.java`)

To align reports with the unified request architecture, the reporting module needs the following adjustments.

### A. Deprecated Report Classes / Endpoints
Delete the following response models and their associated endpoints:
* **Remove**: `PendingRegistrationRow.java` and endpoint `GET /reports/pending-registrations`.
* **Remove**: `GET /reports/unreconciled-event-devices` (replaced by missed checkouts report).

### B. New Report DTOs & Mappings (`model/report/`)

#### 1. `MissedCheckoutRow.java` (Lists logs flagged with no-egress)
```java
@Data
@Builder
public class MissedCheckoutRow {
    private Integer transactionId;
    private String studentId;
    private String studentName;
    private String deviceName;
    private String serialNumber;
    private LocalDate logDate;
    private LocalDateTime ingressTime;
    private boolean noEgressMarked;
    private String notes;
}
```

#### 2. `TimeDiscrepancyRow.java` (Audit discrepancies in check-in/out times)
```java
@Data
@Builder
public class TimeDiscrepancyRow {
    private Integer requestId;
    private String studentId;
    private String studentName;
    private String deviceName;
    private String serialNumber;
    private LocalTime expectedTime;
    private LocalDateTime actualTime;
    private long discrepancyMinutes;
    private String type; // 'EARLY_ARRIVAL', 'LATE_DEPARTURE'
}
```

#### 3. `PurposeBreakdownRow.java` (Analytics by purpose volume)
```java
@Data
@Builder
public class PurposeBreakdownRow {
    private String purpose;
    private int requestCount;
    private int totalDevicesApproved;
    private double percentage;
}
```

### C. Updated Report Service Query Logic (`ReportService.java`)
- **Daily Traffic (`/reports/daily-traffic`)**: Modify underlying query to fetch from `device_transactions` (instead of `device_logs`):
  ```sql
  SELECT dt.transaction_id, r.student_id, rd.device_name, rd.serial_number, 
         dt.log_date, dt.ingress_time, dt.egress_time
  FROM device_transactions dt
  JOIN request_devices rd ON rd.request_device_id = dt.request_device_id
  JOIN requests r ON r.request_id = rd.request_id
  WHERE dt.log_date = :date
  ```
- **Active Devices (`/reports/active-devices`)**: Modify to query `v_device_campus_status` where status is `'entry'`.
- **Missed Checkouts (`/reports/missed-checkouts`)**: Select rows where `no_egress_marked = true` or `(egress_time IS NULL AND log_date < CURRENT_DATE)`.


