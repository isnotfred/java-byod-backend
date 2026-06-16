# BYOD Device Management System — API Documentation

This document describes all REST API endpoints exposed by the Spring Boot backend (`com.pup.byod.javabyodbackend`).

## 1. General Information

- **Base URL:** `/api/v1` (Note: Certain routes like `/reports` and `/super-admin` are mapped directly to root paths and do not use the `/api/v1` prefix).
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

### 2.4 Device Registrations (`/api/v1/devices`)

#### `GET /api/v1/devices`
Retrieves all registered devices.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 12,
      "studentId": "2021-10023",
      "deviceName": "My Work Laptop",
      "brand": "Dell",
      "model": "XPS 15",
      "serialNumber": "SN-DELL-12345",
      "deviceType": "PERSONAL_COMPUTERS",
      "devicePurpose": "Academic BYOD",
      "registrationStatus": "approved",
      "deviceStatus": "active",
      "reviewedBy": 2,
      "reviewedAt": "2026-06-09T10:35:00",
      "remarks": "Verified physical SN",
      "imagePath": "/images/devices/12_xps.jpg",
      "createdAt": "2026-06-09T10:30:00",
      "updatedAt": "2026-06-09T10:35:00"
    }
  ]
  ```

#### `GET /api/v1/devices/{deviceId}`
Retrieves a registered device by ID.
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 12,
    "studentId": "2021-10023",
    "deviceName": "My Work Laptop",
    "brand": "Dell",
    "model": "XPS 15",
    "serialNumber": "SN-DELL-12345",
    "deviceType": "PERSONAL_COMPUTERS",
    "devicePurpose": "Academic BYOD",
    "registrationStatus": "approved",
    "deviceStatus": "active",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T10:35:00",
    "remarks": "Verified physical SN",
    "imagePath": "/images/devices/12_xps.jpg",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:35:00"
  }
  ```

#### `GET /api/v1/devices/serial/{serialNumber}`
Retrieves a registered device by its unique Serial Number.
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 12,
    "studentId": "2021-10023",
    "deviceName": "My Work Laptop",
    "brand": "Dell",
    "model": "XPS 15",
    "serialNumber": "SN-DELL-12345",
    "deviceType": "PERSONAL_COMPUTERS",
    "devicePurpose": "Academic BYOD",
    "registrationStatus": "approved",
    "deviceStatus": "active",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T10:35:00",
    "remarks": "Verified physical SN",
    "imagePath": "/images/devices/12_xps.jpg",
    "createdAt": "2026-06-09T10:30:00",
    "updatedAt": "2026-06-09T10:35:00"
  }
  ```

#### `GET /api/v1/devices/student/{studentId}`
Retrieves all devices registered to a specific student.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 12,
      "studentId": "2021-10023",
      "deviceName": "My Work Laptop",
      "brand": "Dell",
      "model": "XPS 15",
      "serialNumber": "SN-DELL-12345",
      "deviceType": "PERSONAL_COMPUTERS",
      "devicePurpose": "Academic BYOD",
      "registrationStatus": "approved",
      "deviceStatus": "active",
      "reviewedBy": 2,
      "reviewedAt": "2026-06-09T10:35:00",
      "remarks": "Verified physical SN",
      "imagePath": "/images/devices/12_xps.jpg",
      "createdAt": "2026-06-09T10:30:00",
      "updatedAt": "2026-06-09T10:35:00"
    }
  ]
  ```

#### `GET /api/v1/devices/pending`
Retrieves a list of devices currently in the registration approval queue.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 13,
      "studentId": "2021-10023",
      "studentFullName": "Juan Dela Cruz",
      "deviceName": "iPad Pro",
      "deviceType": "COMPONENTS_AND_PERIPHERALS",
      "brand": "Apple",
      "model": "M2 Pro",
      "serialNumber": "SN-APPLE-88392",
      "devicePurpose": "Academic BYOD",
      "imagePath": "/images/devices/13_ipad.png",
      "createdAt": "2026-06-09T10:50:00"
    }
  ]
  ```

#### `GET /api/v1/devices/campus-status`
Retrieves the real-time campus status of all registered devices.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 12,
      "studentId": "2021-10023",
      "deviceName": "My Work Laptop",
      "serialNumber": "SN-DELL-12345",
      "brand": "Dell",
      "model": "XPS 15",
      "deviceType": "PERSONAL_COMPUTERS",
      "campusStatus": "inside",
      "lastEventTime": "2026-06-09T08:00:00"
    }
  ]
  ```

#### `GET /api/v1/devices/campus-status/{serialNumber}`
Retrieves the real-time campus status of a specific device by serial number.
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 12,
    "studentId": "2021-10023",
    "deviceName": "My Work Laptop",
    "serialNumber": "SN-DELL-12345",
    "brand": "Dell",
    "model": "XPS 15",
    "deviceType": "PERSONAL_COMPUTERS",
    "campusStatus": "inside",
    "lastEventTime": "2026-06-09T08:00:00"
  }
  ```

#### `POST /api/v1/devices`
Registers a new BYOD device.
- **Request Body:**
  ```json
  {
    "studentId": "2021-10023",
    "deviceName": "Personal Laptop",
    "brand": "Lenovo",
    "model": "ThinkPad T14",
    "serialNumber": "SN-THINK-99221",
    "deviceType": "PERSONAL_COMPUTERS",
    "devicePurpose": "Academic BYOD",
    "remarks": "Guards registration entry",
    "imagePath": "/images/devices/lenovo.jpg"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "deviceId": 14,
    "studentId": "2021-10023",
    "deviceName": "Personal Laptop",
    "brand": "Lenovo",
    "model": "ThinkPad T14",
    "serialNumber": "SN-THINK-99221",
    "deviceType": "PERSONAL_COMPUTERS",
    "devicePurpose": "Academic BYOD",
    "registrationStatus": "pending",
    "deviceStatus": "active",
    "createdAt": "2026-06-09T10:55:00",
    "updatedAt": "2026-06-09T10:55:00"
  }
  ```

#### `PUT /api/v1/devices/{deviceId}`
Updates the details of a registered device.
- **Request Body:**
  ```json
  {
    "deviceName": "Personal Laptop (Modified)",
    "brand": "Lenovo",
    "model": "ThinkPad T14 Gen 3",
    "devicePurpose": "Academic BYOD",
    "remarks": "Updated specs info",
    "imagePath": "/images/devices/lenovo_new.jpg"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 14,
    "studentId": "2021-10023",
    "deviceName": "Personal Laptop (Modified)",
    "brand": "Lenovo",
    "model": "ThinkPad T14 Gen 3",
    "serialNumber": "SN-THINK-99221",
    "deviceType": "PERSONAL_COMPUTERS",
    "devicePurpose": "Academic BYOD",
    "registrationStatus": "pending",
    "deviceStatus": "active",
    "createdAt": "2026-06-09T10:55:00",
    "updatedAt": "2026-06-09T10:58:00"
  }
  ```

#### `PUT /api/v1/devices/{deviceId}/approve`
Approves a pending device registration (restricted to Admin roles).
- **Request Body:**
  ```json
  {
    "reviewedBy": 2
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 14,
    "registrationStatus": "approved",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:00:00"
  }
  ```

#### `PUT /api/v1/devices/{deviceId}/reject`
Rejects a pending device registration (restricted to Admin roles).
- **Request Body:**
  ```json
  {
    "reviewedBy": 2,
    "remarks": "Serial number mismatch on physical device check"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "deviceId": 14,
    "registrationStatus": "rejected",
    "remarks": "Serial number mismatch on physical device check",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:05:00"
  }
  ```

#### `PUT /api/v1/devices/{deviceId}/deactivate`
Deactivates a device registration (soft-delete).
- **Response Body (`200 OK`):**
  ```json
  {
    "message": "Device deactivated."
  }
  ```

#### `POST /api/v1/devices/import`
Bulk imports devices from a CSV file.
- **Request format:** `multipart/form-data`
- **File parameter:** `file` (MultipartFile)
- **Response Body (`200 OK`):**
  ```json
  {
    "inserted": 120,
    "skipped": 3,
    "errors": [
      {
        "row": 15,
        "serialNumber": "SN-XYZ-123",
        "reasons": [
          "Serial number already exists."
        ]
      }
    ]
  }
  ```

---

### 2.5 Event Request Lifecycle (`/api/v1/event-requests`)

#### `GET /api/v1/event-requests`
Retrieves all event requests.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "eventRequestId": 1,
      "studentId": "2021-10023",
      "responsiblePerson": "Prof. Smith",
      "organization": "Acm Student Chapter",
      "eventName": "Tech Fair 2026",
      "eventPurpose": "Exhibition and testing of prototypes",
      "approvalDocType": "Signed GPOA",
      "approvalDocRef": "REF-GPOA-009A",
      "startDate": "2026-06-15",
      "endDate": "2026-06-17",
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

#### `GET /api/v1/event-requests/{eventRequestId}`
Retrieves details of a specific event request by ID.
- **Response Body (`200 OK`):**
  ```json
  {
    "eventRequestId": 1,
    "studentId": "2021-10023",
    "responsiblePerson": "Prof. Smith",
    "organization": "Acm Student Chapter",
    "eventName": "Tech Fair 2026",
    "eventPurpose": "Exhibition and testing of prototypes",
    "approvalDocType": "Signed GPOA",
    "approvalDocRef": "REF-GPOA-009A",
    "startDate": "2026-06-15",
    "endDate": "2026-06-17",
    "status": "approved",
    "isSubmitted": true,
    "isAccommodated": true,
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:20:00",
    "remarks": "Approved GPOA document attached",
    "createdAt": "2026-06-09T11:15:00",
    "updatedAt": "2026-06-09T11:20:00"
  }
  ```

#### `GET /api/v1/event-requests/active`
Retrieves a list of pending/approved active requests, including device count.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "eventRequestId": 1,
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

#### `GET /api/v1/event-requests/{eventRequestId}/devices`
Retrieves all devices associated with a specific event request.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "eventDeviceId": 5,
      "eventRequestId": 1,
      "deviceName": "Project Prototype Unit 1",
      "brand": "Custom Build",
      "model": "RPi4-A1",
      "deviceType": "Project Prototypes (Optional SN)",
      "serialNumber": "SN-CUSTOM-PROT-1",
      "quantity": 1,
      "verifiedBy": 1,
      "verifiedAt": "2026-06-15T08:00:00",
      "deviceStatus": "approved",
      "remarks": "Hardware prototype verified at entrance",
      "createdAt": "2026-06-09T11:15:00",
      "updatedAt": "2026-06-15T08:00:00"
    }
  ]
  ```

#### `POST /api/v1/event-requests`
Submits a temporary event request listing one or more devices.
- **Request Body:**
  ```json
  {
    "studentId": "2021-10023",
    "responsiblePerson": "Prof. Smith",
    "organization": "Acm Student Chapter",
    "eventName": "Tech Fair 2026",
    "eventPurpose": "Exhibition and testing of prototypes",
    "approvalDocType": "Signed GPOA",
    "approvalDocRef": "REF-GPOA-009A",
    "startDate": "2026-06-15",
    "endDate": "2026-06-17",
    "isSubmitted": true,
    "isAccommodated": true,
    "remarks": "Prototype equipment request",
    "lineItems": [
      {
        "deviceName": "Project Prototype Unit 1",
        "brand": "Custom Build",
        "model": "RPi4-A1",
        "deviceType": "Project Prototypes (Optional SN)",
        "serialNumber": "SN-CUSTOM-PROT-1",
        "quantity": 1,
        "remarks": "Exhibition item"
      }
    ]
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "eventRequestId": 1,
    "studentId": "2021-10023",
    "responsiblePerson": "Prof. Smith",
    "organization": "Acm Student Chapter",
    "eventName": "Tech Fair 2026",
    "eventPurpose": "Exhibition and testing of prototypes",
    "approvalDocType": "Signed GPOA",
    "approvalDocRef": "REF-GPOA-009A",
    "startDate": "2026-06-15",
    "endDate": "2026-06-17",
    "status": "pending",
    "isSubmitted": true,
    "isAccommodated": true,
    "createdAt": "2026-06-09T11:15:00",
    "updatedAt": "2026-06-09T11:15:00"
  }
  ```

#### `PUT /api/v1/event-requests/{eventRequestId}/approve`
Approves an event request. Automatically sets all child devices status to `approved`.
- **Request Body:**
  ```json
  {
    "reviewerUserId": 2
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "eventRequestId": 1,
    "status": "approved",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:20:00"
  }
  ```

#### `PUT /api/v1/event-requests/{eventRequestId}/return`
Returns/sends back an event request for corrections.
- **Request Body:**
  ```json
  {
    "reviewerUserId": 2,
    "remarks": "Please provide a clearer reference link for the GPOA"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "eventRequestId": 1,
    "status": "returned",
    "remarks": "Please provide a clearer reference link for the GPOA",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:25:00"
  }
  ```

#### `PUT /api/v1/event-requests/{eventRequestId}/reject`
Rejects an event request.
- **Request Body:**
  ```json
  {
    "reviewerUserId": 2,
    "remarks": "No matching approved event scheduled."
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "eventRequestId": 1,
    "status": "rejected",
    "remarks": "No matching approved event scheduled.",
    "reviewedBy": 2,
    "reviewedAt": "2026-06-09T11:30:00"
  }
  ```

#### `PUT /api/v1/event-requests/devices/{eventDeviceId}/verify`
Verifies a single device under an event request, updates status when scanned.
- **Request Body:**
  ```json
  {
    "verifiedBy": 1,
    "deviceStatus": "approved"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "eventDeviceId": 5,
    "eventRequestId": 1,
    "deviceName": "Project Prototype Unit 1",
    "deviceStatus": "approved",
    "verifiedBy": 1,
    "verifiedAt": "2026-06-15T08:00:00"
  }
  ```

#### `GET /api/v1/event-requests/guard`
Retrieves active event requests filtered for guard view.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "eventRequestId": 1,
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

#### `POST /api/v1/event-requests/devices/log-entry`
Logs campus entry for one or more event request devices.
- **Request Body:**
  ```json
  {
    "deviceIds": [5, 6],
    "guardId": 1
  }
  ```
- **Response Body (`200 OK`):** Empty response.

#### `POST /api/v1/event-requests/devices/log-exit`
Logs campus exit for one or more event request devices.
- **Request Body:**
  ```json
  {
    "deviceIds": [5, 6],
    "guardId": 1
  }
  ```
- **Response Body (`200 OK`):** Empty response.

#### `GET /api/v1/event-requests/devices/reconciliation-report`
Retrieves all event request devices and their statuses for reconciliation reports.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "eventDeviceId": 5,
      "eventRequestId": 1,
      "deviceName": "Project Prototype Unit 1",
      "brand": "Custom Build",
      "model": "RPi4-A1",
      "deviceType": "Project Prototypes (Optional SN)",
      "serialNumber": "SN-CUSTOM-PROT-1",
      "quantity": 1,
      "verifiedBy": 1,
      "verifiedAt": "2026-06-15T08:00:00",
      "deviceStatus": "approved",
      "remarks": "Hardware prototype verified at entrance",
      "createdAt": "2026-06-09T11:15:00",
      "updatedAt": "2026-06-15T08:00:00"
    }
  ]
  ```

---

### 2.6 Gate Ingress / Egress Logging (`/api/v1/device-logs`)

#### `GET /api/v1/device-logs/devices/{deviceId}`
Retrieves ingress/egress history logs for a specific device.
- **Parameters:**
  - `limit` (Query String, Optional, Default: 50)
  - `offset` (Query String, Optional, Default: 0)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "logId": 102,
      "deviceId": 12,
      "studentId": "2021-10023",
      "eventType": "entry",
      "eventTime": "2026-06-09T08:00:00",
      "handledBy": 1,
      "logoutType": null,
      "autoExit": false,
      "notes": "Regular entry",
      "createdAt": "2026-06-09T08:00:00"
    }
  ]
  ```

#### `GET /api/v1/device-logs/students/{studentId}`
Retrieves ingress/egress logs for all devices belonging to a student.
- **Parameters:**
  - `limit` (Query String, Optional, Default: 50)
  - `offset` (Query String, Optional, Default: 0)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "logId": 102,
      "deviceId": 12,
      "studentId": "2021-10023",
      "eventType": "entry",
      "eventTime": "2026-06-09T08:00:00",
      "handledBy": 1,
      "logoutType": null,
      "autoExit": false,
      "notes": "Regular entry",
      "createdAt": "2026-06-09T08:00:00"
    }
  ]
  ```

#### `POST /api/v1/device-logs/entry`
Logs a device entry at the security gate.
- **Request Body:**
  ```json
  {
    "serialNumber": "SN-DELL-12345",
    "handledBy": 1,
    "notes": "Entered with bag"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "logId": 103,
    "deviceId": 12,
    "studentId": "2021-10023",
    "eventType": "entry",
    "eventTime": "2026-06-09T12:00:00",
    "handledBy": 1,
    "logoutType": null,
    "autoExit": false,
    "notes": "Entered with bag",
    "createdAt": "2026-06-09T12:00:00"
  }
  ```

#### `POST /api/v1/device-logs/exit`
Logs a device exit at the security gate.
- **Request Body:**
  ```json
  {
    "serialNumber": "SN-DELL-12345",
    "handledBy": 1,
    "notes": "Manual exit scan"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "logId": 104,
    "deviceId": 12,
    "studentId": "2021-10023",
    "eventType": "exit",
    "eventTime": "2026-06-09T17:00:00",
    "handledBy": 1,
    "logoutType": "manual",
    "autoExit": false,
    "notes": "Manual exit scan",
    "createdAt": "2026-06-09T17:00:00"
  }
  ```

#### `POST /api/v1/device-logs/auto-exit`
Triggers a system batch run to auto-exit any devices left on campus at the end of the day.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "logId": 105,
      "deviceId": 12,
      "studentId": "2021-10023",
      "eventType": "exit",
      "eventTime": "2026-06-09T22:00:00",
      "handledBy": null,
      "logoutType": "automatic",
      "autoExit": true,
      "notes": "System automatic checkout at campus close",
      "createdAt": "2026-06-09T22:00:00"
    }
  ]
  ```

---

### 2.7 Audit Logs (`/api/v1/audit-logs`)

#### `GET /api/v1/audit-logs`
Retrieves system-wide audit trail logs.
- **Parameters:**
  - `limit` (Query String, Optional, Default: 50)
  - `offset` (Query String, Optional, Default: 0)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "auditId": 25,
      "userId": 2,
      "actionType": "DEVICE_APPROVED",
      "targetTable": "devices",
      "targetId": "12",
      "oldValues": "{\"registrationStatus\":\"pending\"}",
      "newValues": "{\"registrationStatus\":\"approved\"}",
      "ipAddress": "192.168.1.5",
      "createdAt": "2026-06-09T10:35:00"
    }
  ]
  ```

#### `GET /api/v1/audit-logs/user/{userId}`
Retrieves audit trail logs generated by actions of a specific user.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "auditId": 25,
      "userId": 2,
      "actionType": "DEVICE_APPROVED",
      "targetTable": "devices",
      "targetId": "12",
      "oldValues": "{\"registrationStatus\":\"pending\"}",
      "newValues": "{\"registrationStatus\":\"approved\"}",
      "ipAddress": "192.168.1.5",
      "createdAt": "2026-06-09T10:35:00"
    }
  ]
  ```

#### `GET /api/v1/audit-logs/action/{actionType}`
Retrieves audit logs filtered by a specific system action type.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "auditId": 25,
      "userId": 2,
      "actionType": "DEVICE_APPROVED",
      "targetTable": "devices",
      "targetId": "12",
      "oldValues": "{\"registrationStatus\":\"pending\"}",
      "newValues": "{\"registrationStatus\":\"approved\"}",
      "ipAddress": "192.168.1.5",
      "createdAt": "2026-06-09T10:35:00"
    }
  ]
  ```

---

### 2.8 Business Reports (`/reports`)
*Note: All endpoints below are restricted to roles `admin` and `super_admin` only.*

#### `GET /reports/daily-traffic`
Generates a daily device traffic report listing all ingress/egress scans for the day.
- **Parameters:**
  - `date` (Query String, Optional, Default: Today, Format: `yyyy-MM-dd`)
  - `studentId` (Query String, Optional)
  - `deviceType` (Query String, Optional)
  - `status` (Query String, Optional)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "logId": 102,
      "eventType": "entry",
      "eventTime": "2026-06-09T08:00:00+08:00",
      "autoExit": false,
      "logoutType": null,
      "notes": "Regular entry",
      "deviceId": 12,
      "deviceName": "My Work Laptop",
      "serialNumber": "SN-DELL-12345",
      "deviceType": "Personal Computers",
      "registrationStatus": "approved",
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "handledByName": "John Doe"
    }
  ]
  ```

#### `GET /reports/monthly-traffic`
Generates monthly aggregated counts of entries/exits grouped by device category and student.
- **Parameters:**
  - `year` (Query String, Optional, Default: Current Year)
  - `month` (Query String, Optional, Default: Current Month, Range: 1-12)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "reportMonth": "2026-06-01",
      "deviceCategory": "Personal Computers",
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "entryCount": 18,
      "exitCount": 18,
      "totalEvents": 36
    }
  ]
  ```

#### `GET /reports/pending-registrations`
Real-time report listing all devices awaiting registration approval.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 13,
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "deviceName": "iPad Pro",
      "brand": "Apple",
      "model": "M2 Pro",
      "serialNumber": "SN-APPLE-88392",
      "deviceType": "Components & Peripherals",
      "devicePurpose": "Academic BYOD",
      "imagePath": "/images/devices/13_ipad.png",
      "submittedAt": "2026-06-09T10:50:00+08:00",
      "submittedBy": "John Doe"
    }
  ]
  ```

#### `GET /reports/active-devices`
Snapshot listing all devices currently on campus (whose current derived status is `inside`).
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 12,
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "deviceName": "My Work Laptop",
      "serialNumber": "SN-DELL-12345",
      "deviceType": "Personal Computers",
      "brand": "Dell",
      "model": "XPS 15",
      "enteredAt": "2026-06-09T08:00:00+08:00"
    }
  ]
  ```

#### `GET /reports/device-frequency`
Schedules a lookup of bring-in frequencies for devices and students within a date range.
- **Parameters (Required):**
  - `from` (Query String, Format: `yyyy-MM-dd`)
  - `to` (Query String, Format: `yyyy-MM-dd`)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "deviceId": 12,
      "deviceName": "My Work Laptop",
      "serialNumber": "SN-DELL-12345",
      "deviceType": "Personal Computers",
      "brand": "Dell",
      "model": "XPS 15",
      "studentId": "2021-10023",
      "studentName": "Juan Dela Cruz",
      "courseYearLevel": "BSIT 3-1",
      "entryCount": 18,
      "exitCount": 18,
      "firstSeen": "2026-06-01T08:00:00+08:00",
      "lastSeen": "2026-06-09T08:00:00+08:00"
    }
  ]
  ```

#### `GET /reports/incidents`
Fetches a list of audit incidents (overrides, status role changes, deactivations, rejections).
- **Parameters (Required):**
  - `from` (Query String, Format: `yyyy-MM-dd`)
  - `to` (Query String, Format: `yyyy-MM-dd`)
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "auditId": 25,
      "actionType": "DEVICE_REJECTED",
      "targetTable": "devices",
      "targetId": "14",
      "oldValues": "{\"registrationStatus\":\"pending\"}",
      "newValues": "{\"registrationStatus\":\"rejected\"}",
      "ipAddress": "192.168.1.5",
      "createdAt": "2026-06-09T11:05:00+08:00",
      "performedBy": "Jane Smith",
      "performerRole": "admin"
    }
  ]
  ```

---

### 2.9 Super Admin Operations (`/super-admin`)
*Note: All endpoints below are restricted to the `super_admin` role.*

#### `POST /super-admin/admins`
Creates a new Admin account.
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "username": "admin_clara",
    "password": "AdminPassword567",
    "fullName": "Clara Oswald"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "userId": 5,
    "username": "admin_clara",
    "email": "clara.oswald@example.com",
    "fullName": "Clara Oswald",
    "role": "admin",
    "status": "active",
    "createdAt": "2026-06-09T11:45:00",
    "updatedAt": "2026-06-09T11:45:00"
  }
  ```

#### `POST /super-admin/guards`
Creates a new Security Guard account.
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "username": "guard_amy",
    "password": "GuardPassword567",
    "fullName": "Amy Pond"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "userId": 6,
    "username": "guard_amy",
    "email": "amy.pond@example.com",
    "fullName": "Amy Pond",
    "role": "guard",
    "status": "active",
    "createdAt": "2026-06-09T11:50:00",
    "updatedAt": "2026-06-09T11:50:00"
  }
  ```

#### `POST /super-admin/users`
Creates a new pending Admin or Guard account and triggers an email containing a default randomly-generated password for first login.
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "admin"
  }
  ```
- **Response Body (`201 Created`):**
  ```json
  {
    "userId": 7,
    "username": "jane.doe@example.com",
    "email": "jane.doe@example.com",
    "fullName": "Jane Doe",
    "role": "admin",
    "status": "pending",
    "createdAt": "2026-06-09T13:00:00",
    "updatedAt": "2026-06-09T13:00:00"
  }
  ```

#### `PUT /super-admin/users/{userId}`
Updates an Admin or Guard account's name and status.
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "fullName": "Amy Pond-Williams",
    "status": "active"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 6,
    "username": "guard_amy",
    "email": "amy.pond@example.com",
    "fullName": "Amy Pond-Williams",
    "role": "guard",
    "status": "active",
    "createdAt": "2026-06-09T11:50:00",
    "updatedAt": "2026-06-09T11:55:00"
  }
  ```

#### `PUT /super-admin/users/{userId}/deactivate`
Deactivates a user account (restricted to Admin & Guard targets).
- **Request Body:**
  ```json
  {
    "actingUserId": 9
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 6,
    "username": "guard_amy",
    "email": "amy.pond@example.com",
    "fullName": "Amy Pond-Williams",
    "role": "guard",
    "status": "inactive",
    "createdAt": "2026-06-09T11:50:00",
    "updatedAt": "2026-06-09T12:00:00"
  }
  ```

#### `PUT /super-admin/users/{userId}/role`
Changes a user's system role (e.g. promoting a guard to admin, or vice versa).
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "role": "admin"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "userId": 6,
    "username": "guard_amy",
    "email": "amy.pond@example.com",
    "fullName": "Amy Pond-Williams",
    "role": "admin",
    "status": "active",
    "createdAt": "2026-06-09T11:50:00",
    "updatedAt": "2026-06-09T12:05:00"
  }
  ```

---

### 2.10 System Configuration (`/api/v1/settings`)

#### `GET /api/v1/settings`
Retrieves all system configuration parameters and policies. Available to all authenticated roles.
- **Response Body (`200 OK`):**
  ```json
  [
    {
      "settingKey": "max_devices_per_student",
      "settingValue": "5",
      "description": "Maximum number of active registered devices allowed per student",
      "updatedAt": "2026-06-14T14:00:00"
    },
    {
      "settingKey": "allow_unregistered_devices",
      "settingValue": "true",
      "description": "Whether unapproved devices can be checked in by guards",
      "updatedAt": "2026-06-14T14:00:00"
    },
    {
      "settingKey": "event_request_max_duration_days",
      "settingValue": "7",
      "description": "Maximum duration in days for an event request",
      "updatedAt": "2026-06-14T14:00:00"
    },
    {
      "settingKey": "auto_exit_cutoff_time",
      "settingValue": "22:00",
      "description": "Cutoff time after which checked-in devices are auto-exited",
      "updatedAt": "2026-06-14T14:00:00"
    }
  ]
  ```

#### `PUT /api/v1/settings/{key}`
Modifies a specific system configuration parameter. Only allowed for `super_admin` role. Generates a `SYSTEM_CONFIG_UPDATED` audit log row.
*Note: For the key `auto_exit_cutoff_time`, updates are validated and only permit values of `'20:00'`, `'21:00'`, or `'22:00'` (returns `400 Bad Request` / `BusinessRuleException` on other values).*
- **Request Body:**
  ```json
  {
    "actingUserId": 9,
    "settingValue": "4"
  }
  ```
- **Response Body (`200 OK`):**
  ```json
  {
    "settingKey": "max_devices_per_student",
    "settingValue": "4",
    "description": "Maximum number of active registered devices allowed per student",
    "updatedAt": "2026-06-14T14:05:00"
  }
  ```

