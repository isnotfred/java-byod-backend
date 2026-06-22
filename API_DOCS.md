# BYOD Device Management System — API Documentation

This document describes all REST API endpoints exposed by the Spring Boot backend (`com.pup.byod.javabyodbackend`).

## 1. General Information

- **Base URL:** `/api` (Note: Certain routes like `/reports` are mapped directly to root paths and do not use the `/api` prefix).
- **Default Port:** `8080` (locally) / hosted on Railway.
- **Content-Type:** `application/json` for all request/response bodies unless stated otherwise.
- **Role Constraints:** Surfaced in custom checks within services or controllers. Common roles are `guard`, `admin`, and `super_admin`.

---

## 2. API Endpoints

### 2.1 Authentication (`/api/v1/auth`)

#### `POST /api/v1/auth/login`
Authenticates a user and returns their profile details (excluding password hash).
- **Request Body:**
  ```json
  {
    "username": "guard_john",
    "password": "SecurePassword123"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 1,
    "username": "guard_john",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "role": "guard",
    "status": "active",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:30:00"
  }
  ```

#### `POST /api/v1/auth/logout`
Logs a user out and generates an audit log entry.
- **Request Body:**
  ```json
  {
    "userId": 1
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "message": "Logout successful."
  }
  ```

#### `POST /api/v1/auth/forgot-password`
Initiates the password reset flow. Generates a secure token, saves it to the database with a 15-minute expiration, and emails it to the user.
- **Request Body:**
  ```json
  {
    "email": "clara.oswald@example.com"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "message": "Password reset token sent to your email if the account exists."
  }
  ```

#### `POST /api/v1/auth/reset-password`
Completes the password reset process using a valid reset token.
- **Request Body:**
  ```json
  {
    "token": "4a2c918a-bbcf-4a37-9efb-665e89d12345",
    "newPassword": "MyBrandNewSecurePassword123"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "message": "Password reset successful."
  }
  ```

---

### 2.2 User Management (`/api/v1/users`)

#### `GET /api/v1/users`
Retrieves a list of all system users.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "userId": 1,
      "username": "guard_john",
      "email": "john.doe@example.com",
      "fullName": "John Doe",
      "role": "guard",
      "status": "active",
      "createdAt": "2026-06-09T10:30:00",
      "updatedAt": "2026-06-09T10:30:00"
    }
  ]
  ```

#### `GET /api/v1/users/{id}`
Retrieves details of a specific user.
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 1,
    "username": "guard_john",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "role": "guard",
    "status": "active",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:30:00"
  }
  ```

#### `PUT /api/v1/users/{id}/profile/password`
Allows a logged-in user to update their own password after verifying their current password.
- **Request Body:**
  ```json
  {
    "currentPassword": "SecurePassword123",
    "newPassword": "MyBrandNewPassword456"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 1,
    "username": "guard_john",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "role": "guard",
    "status": "active",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:52:00"
  }
  ```

---

### 2.3 Student Registry (`/api/v1/students`)

#### `GET /api/v1/students`
Retrieves all students.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "studentId": "2021-10023",
      "firstName": "Juan",
      "lastName": "Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "course": "BSIT",
      "yearLevel": 3,
      "status": "active",
      "createdAt": "2026-06-09T10:30:00",
      "updatedAt": "2026-06-09T10:30:00"
    }
  ]
  ```

#### `GET /api/v1/students/{studentId}`
Retrieves a student by their ID.
- **Response Body (`200 OK`):**
  ```json
  {
    "studentId": "2021-10023",
    "firstName": "Juan",
    "lastName": "Dela Cruz",
    "courseYearLevel": "BSIT 3-1",
    "course": "BSIT",
    "yearLevel": 3,
    "status": "active",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:30:00"
  }
  ```

#### `GET /api/v1/students/search`
Searches students using a search keyword (e.g. name or ID).
- **Parameters:**
  - `keyword` (Query String, Required)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "studentId": "2021-10023",
      "firstName": "Juan",
      "lastName": "Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "course": "BSIT",
      "yearLevel": 3,
      "status": "active",
      "createdAt": "2026-06-09T10:30:00",
      "updatedAt": "2026-06-09T10:30:00"
    }
  ]
  ```

#### `POST /api/v1/students`
Creates a student registry record.
- **Request Body:**
  ```json
  {
    "studentId": "2021-10023",
    "firstName": "Juan",
    "lastName": "Dela Cruz",
    "courseYearLevel": "BSIT 3-1"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "studentId": "2021-10023",
    "firstName": "Juan",
    "lastName": "Dela Cruz",
    "courseYearLevel": "BSIT 3-1",
    "course": "BSIT",
    "yearLevel": 3,
    "status": "active",
    "createdAt": "2026-06-09T10:40:00",
    "updatedAt": "2026-06-09T10:40:00"
  }
  ```

#### `PUT /api/v1/students/{studentId}`
Updates details of an existing student record.
- **Request Body:**
  ```json
  {
    "firstName": "Juan",
    "lastName": "Dela Cruz II",
    "courseYearLevel": "BSIT 4-1",
    "status": "active"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "studentId": "2021-10023",
    "firstName": "Juan",
    "lastName": "Dela Cruz II",
    "courseYearLevel": "BSIT 4-1",
    "course": "BSIT",
    "yearLevel": 4,
    "status": "active",
    "createdAt": "2026-06-09T10:40:00",
    "updatedAt": "2026-06-09T10:45:00"
  }
  ```

#### `PUT /api/v1/students/{studentId}/deactivate`
Deactivates a student record (soft-delete).
- **Response Body (`200 OK`):**
  ```json
  {
    "message": "Student deactivated."
  }
  ```

#### `POST /api/v1/students/import`
Bulk imports students from a CSV file.
- **Request format:** `multipart/form-data`
- **File parameter:** `file` (MultipartFile)
- **Response Body (`200 OK`):**
  ```json
  {
    "successCount": 150,
    "failedCount": 2,
    "errors": [
      "Row 12: Student ID already exists.",
      "Row 45: First name is required."
    ]
  }
  ```

---

### 2.4 Unified Access Requests (`/api/requests`)
Handles creation, verification, and querying of both normal (individual BYOD) and event requests.

#### `GET /api/requests`
Retrieves all access requests.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "requestId": 1,
      "requestType": "EVENT",
      "studentId": "2021-10023",
      "responsiblePerson": "Prof. Smith",
      "organization": "Acm Student Chapter",
      "eventName": "Tech Fair 2026",
      "purpose": "Exhibition and testing of prototypes",
      "approvalDocType": "Signed GPOA",
      "approvalDocRef": "REF-GPOA-009A",
      "startDate": "2026-06-15",
      "endDate": "2026-06-17",
      "expectedIngressTime": "08:00:00",
      "expectedEgressTime": "17:00:00",
      "status": "approved",
      "isSubmitted": true,
      "isAccommodated": true,
      "reviewedBy": 2,
      "reviewedAt": "2026-06-09T11:20:00",
      "remarks": "Approved GPOA document attached",
      "createdAt": "2026-06-09T11:15:00",
      "updatedAt": "2026-06-09T11:20:00"
    }
  ]
  ```

#### `GET /api/requests/{requestId}`
Retrieves a request by its ID.
- **Response Body (`200 OK`):**
  ```json
  {
    "requestId": 1,
    "requestType": "EVENT",
    "studentId": "2021-10023",
    "responsiblePerson": "Prof. Smith",
    "organization": "Acm Student Chapter",
    "eventName": "Tech Fair 2026",
    "purpose": "Exhibition and testing of prototypes",
    "approvalDocType": "Signed GPOA",
    "approvalDocRef": "REF-GPOA-009A",
    "startDate": "2026-06-15",
    "endDate": "2026-06-17",
    "expectedIngressTime": "08:00:00",
    "expectedEgressTime": "17:00:00",
    "status": "approved",
    "isSubmitted": true,
    "isAccommodated": true,
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:20:00",
    "remarks": "Approved",
    "createdAt": "2026-06-09T11:15:00",
    "updatedAt": "2026-06-09T11:20:00"
  }
  ```

#### `GET /api/requests/student/{studentId}`
Retrieves all requests submitted by a specific student.

#### `GET /api/requests/active`
Retrieves a list of active requests mapped to `v_active_requests` (including device counts).
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "requestId": 1,
      "requestType": "EVENT",
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "eventName": "Tech Fair 2026",
      "organization": "Acm Student Chapter",
      "startDate": "2026-06-15",
      "endDate": "2026-06-17",
      "status": "approved",
      "deviceCount": 3
    }
  ]
  ```

#### `GET /api/requests/{requestId}/devices`
Retrieves all devices associated with a request.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "requestDeviceId": 5,
      "requestId": 1,
      "deviceName": "Macbook Pro",
      "brand": "Apple",
      "model": "M3 Pro",
      "deviceType": "Personal Computers",
      "serialNumber": "SN-MAC-12345",
      "quantity": 1,
      "imagePath": "/uploads/devices/sn_mac.jpg",
      "deviceStatus": "approved",
      "verifiedBy": 2,
      "verifiedAt": "2026-06-15T08:00:00",
      "remarks": "Verified physical SN at gate",
      "createdAt": "2026-06-09T11:15:00",
      "updatedAt": "2026-06-15T08:00:00"
    }
  ]
  ```

#### `GET /api/requests/campus-status`
Retrieves campus presence status from `v_device_campus_status` for all request devices.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "requestDeviceId": 5,
      "requestId": 1,
      "studentId": "2021-10023",
      "deviceName": "Macbook Pro",
      "serialNumber": "SN-MAC-12345",
      "brand": "Apple",
      "model": "M3 Pro",
      "deviceType": "Personal Computers",
      "campusStatus": "entry",
      "lastEventTime": "2026-06-22T08:00:00",
      "noEgressMarked": false
    }
  ]
  ```

#### `GET /api/requests/campus-status/{serialNumber}`
Retrieves status for a single device by its serial number.

#### `POST /api/requests`
Submits a normal or event request along with its device list (line items).
- **Request Body:**
  ```json
  {
    "requestType": "normal",
    "studentId": "2021-10023",
    "purpose": "Academic BYOD study classes",
    "startDate": "2026-06-22",
    "endDate": "2026-07-22",
    "expectedIngressTime": "08:00:00",
    "expectedEgressTime": "17:00:00",
    "isSubmitted": true,
    "isAccommodated": false,
    "remarks": "Student BYOD request",
    "lineItems": [
      {
        "deviceName": "Work Laptop",
        "brand": "Lenovo",
        "model": "ThinkPad T14",
        "deviceType": "Personal Computers",
        "serialNumber": "SN-THINK-992",
        "quantity": 1,
        "imagePath": "/uploads/lenovo.png",
        "remarks": "For development"
      }
    ]
  }
  ```
- **Response Body (`201 Created`):** Returns the saved `Request` object header.

#### `PUT /api/requests/{requestId}/approve`
Approves a request (Admin only) and sets all associated devices to `approved`.
- **Request Body:** `{ "reviewerUserId": 2 }`

#### `PUT /api/requests/{requestId}/reject`
Rejects a request.
- **Request Body:** `{ "reviewerUserId": 2, "remarks": "Required documents missing" }`

#### `PUT /api/requests/{requestId}/return`
Returns a request for modifications.
- **Request Body:** `{ "reviewerUserId": 2, "remarks": "Incorrect serial number format" }`

#### `PUT /api/requests/devices/{requestDeviceId}/verify`
Verifies or updates the approval status of a single device (e.g. approve/reject individual hardware).
- **Request Body:**
  ```json
  {
    "verifiedBy": 2,
    "deviceStatus": "approved"
  }
  ```

---

### 2.5 Daily Gate Transactions (`/api/transactions`)
Manages scan events at the campus gates. Replaces legacy `device_logs`.

#### `POST /api/transactions/scan`
Processes a gate scan. The backend automatically determines whether the transaction is an Ingress (Check-In) or Egress (Check-Out) based on the device's status for the current day.
- **Request Body:**
  ```json
  {
    "serialNumber": "SN-MAC-12345",
    "handledBy": 2,
    "notes": "Main gate scan"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "status": "CHECK_IN_SUCCESS",
    "message": "Device 'Macbook Pro' checked in successfully.",
    "device": {
      "requestDeviceId": 5,
      "requestId": 1,
      "deviceName": "Macbook Pro",
      "serialNumber": "SN-MAC-12345"
    }
  }
  ```

#### `POST /api/transactions/batch-ingress`
Performs batch check-in for multiple devices.
- **Request Body:** `{ "requestDeviceIds": [5, 6], "handledBy": 2 }`

#### `POST /api/transactions/batch-egress`
Performs batch check-out for multiple devices.
- **Request Body:** `{ "requestDeviceIds": [5, 6], "handledBy": 2 }`

#### `POST /api/transactions/reconcile`
Triggers check-out reconciliation, automatically flagging un-egressed transactions from prior days as missed check-out (`no_egress_marked = true`).
- **Response Body (`200 OK`):** `{ "markedAsMissed": 12 }`

#### `GET /api/transactions/device/{requestDeviceId}`
Queries transaction logs history for a specific request device.

#### `GET /api/transactions/{transactionId}`
Queries details of a specific transaction by ID.

---

### 2.6 Audit Logs (`/api/v1/audit-logs`)

#### `GET /api/v1/audit-logs`
Retrieves system-wide audit logs.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "auditId": 25,
      "userId": 2,
      "actionType": "DEVICE_CHECK_IN",
      "targetTable": "device_transactions",
      "targetId": "12",
      "ipAddress": "192.168.1.5",
      "createdAt": "2026-06-22T08:00:00"
    }
  ]
  ```

---

### 2.7 Business Reports (`/reports`)
Restricted to Admin and Super Admin users. Pulls analytics from request transactions.

#### `GET /reports/daily-traffic`
Generates a daily traffic report.
- **Parameters:** `date` (Query String, Required), `studentId` (Optional), `deviceType` (Optional).

#### `GET /reports/monthly-traffic`
Aggregates entries, exits, and missed checkouts for a calendar month.
- **Parameters:** `year`, `month`.

#### `GET /reports/active-devices`
Lists devices currently inside the campus.

#### `GET /reports/device-frequency`
Gets device entry frequency over a date range.
- **Parameters:** `from`, `to`.

#### `GET /reports/incidents`
Lists administrative incident logs, rejections, overrides, and returned requests.
- **Parameters:** `from`, `to`.

#### `GET /reports/missed-checkouts`
Lists all transactions flagged with `no_egress_marked = true` or left unclosed.
- **Parameters:** `from`, `to`.

#### `GET /reports/purpose-breakdown`
Retrieves statistical breakdown of requests and devices grouped by purpose.

---

### 2.8 Super Admin Operations (`/super-admin`)
Endpoints restricted to `super_admin` role to manage administrative accounts.

#### `POST /super-admin/admins`
#### `POST /super-admin/guards`
#### `POST /super-admin/users`
#### `PUT /super-admin/users/{userId}`
#### `PUT /super-admin/users/{userId}/deactivate`
#### `PUT /super-admin/users/{userId}/role`

---

### 2.9 System Configuration (`/api/v1/settings`)

#### `GET /api/v1/settings`
#### `PUT /api/v1/settings/{key}`
