package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.RequestDAO;
import com.pup.byod.javabyodbackend.dao.RequestDeviceDAO;
import com.pup.byod.javabyodbackend.dao.StudentDAO;
import com.pup.byod.javabyodbackend.dao.SystemSettingDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.ActiveRequest;
import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.Request;
import com.pup.byod.javabyodbackend.model.RequestDevice;
import com.pup.byod.javabyodbackend.model.enums.DeviceVerificationStatus;
import com.pup.byod.javabyodbackend.model.enums.RequestStatus;
import com.pup.byod.javabyodbackend.model.enums.RequestType;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * RequestService
 *
 * Handles all unified request lifecycle operations (normal + event requests).
 * Replaces both DeviceService and EventRequestService from the old architecture.
 */
@Service
public class RequestService {

    private final RequestDAO requestDAO;
    private final RequestDeviceDAO requestDeviceDAO;
    private final StudentDAO studentDAO;
    private final AuditLogService auditLogService;
    private final SystemSettingDAO systemSettingDAO;

    public RequestService(RequestDAO requestDAO,
                          RequestDeviceDAO requestDeviceDAO,
                          StudentDAO studentDAO,
                          AuditLogService auditLogService,
                          SystemSettingDAO systemSettingDAO) {
        this.requestDAO = requestDAO;
        this.requestDeviceDAO = requestDeviceDAO;
        this.studentDAO = studentDAO;
        this.auditLogService = auditLogService;
        this.systemSettingDAO = systemSettingDAO;
    }

    // ── Read Operations ────────────────────────────────────────────

    public List<Request> getAllRequests() {
        return requestDAO.findAll();
    }

    public Request getRequestById(int requestId) {
        return requestDAO.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found."));
    }

    public List<Request> getRequestsByStudentId(String studentId) {
        return requestDAO.findByStudentId(studentId);
    }

    public List<Request> getPendingRequests() {
        return requestDAO.findPendingRequests();
    }

    public List<ActiveRequest> getActiveRequests() {
        return requestDAO.findActiveRequests();
    }

    public List<RequestDevice> getDevicesForRequest(int requestId) {
        return requestDeviceDAO.findByRequestId(requestId);
    }

    public List<DeviceCampusStatus> getCampusStatus() {
        return requestDeviceDAO.findCampusStatus();
    }

    public DeviceCampusStatus getCampusStatusBySerial(String serialNumber) {
        return requestDeviceDAO.findCampusStatusBySerial(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device status not found."));
    }

    // ── Create Request ─────────────────────────────────────────────

    @Transactional
    public Request createRequest(String requestTypeStr,
                                 String studentId,
                                 String eventName,
                                 String venue,
                                 String organization,
                                 String responsiblePerson,
                                 String purpose,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 LocalTime expectedIngressTime,
                                 LocalTime expectedEgressTime,
                                 Boolean isSubmitted,
                                 Boolean isAccommodated,
                                 String remarks,
                                 Integer creatorUserId,
                                 List<LineItemRequest> lineItems) {

        ValidationUtil.requireNonBlank(studentId, "Student ID");
        ValidationUtil.requireNonBlank(purpose, "Purpose");
        ValidationUtil.requireNonNull(startDate, "Start date");
        ValidationUtil.requireNonNull(endDate, "End date");
        ValidationUtil.requireNonNull(expectedIngressTime, "Expected ingress time");
        ValidationUtil.requireNonNull(expectedEgressTime, "Expected egress time");

        RequestType requestType = RequestType.fromString(requestTypeStr != null ? requestTypeStr : "normal");

        // Validate student exists
        if (studentDAO.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found.");
        }

        // Event-specific validations
        if (requestType == RequestType.event) {
            ValidationUtil.requireValidEventName(eventName);
            if (responsiblePerson != null && !responsiblePerson.isBlank()) {
                ValidationUtil.requireValidName(responsiblePerson, "Responsible person");
            }
            if (organization != null && !organization.isBlank()) {
                ValidationUtil.requireValidOrganization(organization);
            }
        }

        // Validate date range
        validateDateRange(startDate, endDate, requestType);
        validateTimes(startDate, expectedIngressTime, expectedEgressTime, requestType);

        // At least one device is required
        if (lineItems == null || lineItems.isEmpty()) {
            throw new BusinessRuleException("At least one device is required.");
        }

        // Validate max devices per student
        int maxDevices = 5;
        try {
            maxDevices = Integer.parseInt(systemSettingDAO.getValue("max_devices_per_student", "5"));
        } catch (NumberFormatException ignored) {}

        int currentDeviceCount = requestDeviceDAO.countApprovedDevicesForStudent(studentId);
        if (currentDeviceCount + lineItems.size() > maxDevices) {
            throw new BusinessRuleException(
                    "Maximum devices limit exceeded (current: " + currentDeviceCount + ", adding: " + lineItems.size() + ", max: " + maxDevices + ").");
        }

        // Build and insert the request
        Request request = Request.builder()
                .requestType(requestType)
                .studentId(studentId)
                .eventName(eventName)
                .venue(venue)
                .organization(organization)
                .responsiblePerson(responsiblePerson)
                .purpose(purpose)
                .startDate(startDate)
                .endDate(endDate)
                .expectedIngressTime(expectedIngressTime)
                .expectedEgressTime(expectedEgressTime)
                .status(RequestStatus.approved)
                .reviewedBy(creatorUserId)
                .reviewedAt(java.time.LocalDateTime.now())
                .isSubmitted(Boolean.TRUE.equals(isSubmitted))
                .isAccommodated(Boolean.TRUE.equals(isAccommodated))
                .remarks(remarks)
                .build();

        int requestId = requestDAO.insert(request);

        // Insert line items (devices)
        for (LineItemRequest lineItem : lineItems) {
            validateLineItem(lineItem);

            RequestDevice device = RequestDevice.builder()
                    .requestId(requestId)
                    .deviceName(lineItem.deviceName)
                    .brand(lineItem.brand)
                    .model(lineItem.model)
                    .deviceType(lineItem.deviceType)
                    .serialNumber(lineItem.serialNumber)
                    .quantity(lineItem.quantity != null ? lineItem.quantity : 1)
                    .imagePath(lineItem.imagePath)
                    .deviceStatus(DeviceVerificationStatus.approved)
                    .verifiedBy(creatorUserId)
                    .verifiedAt(java.time.LocalDateTime.now())
                    .remarks(lineItem.remarks)
                    .build();

            requestDeviceDAO.insert(device);
        }

        auditLogService.writeAuditLog(creatorUserId, "REQUEST_CREATED", "requests",
                String.valueOf(requestId), null, null, null);

        return getRequestById(requestId);
    }

    @Transactional
    public Request updateRequest(int requestId,
                                 String requestTypeStr,
                                 String studentId,
                                 String eventName,
                                 String venue,
                                 String organization,
                                 String responsiblePerson,
                                 String purpose,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 LocalTime expectedIngressTime,
                                 LocalTime expectedEgressTime,
                                 Boolean isSubmitted,
                                 Boolean isAccommodated,
                                 String remarks,
                                 Integer modifierUserId,
                                 List<LineItemRequest> lineItems) {

        Request request = getRequestById(requestId);

        ValidationUtil.requireNonBlank(studentId, "Student ID");
        ValidationUtil.requireNonBlank(purpose, "Purpose");
        ValidationUtil.requireNonNull(startDate, "Start date");
        ValidationUtil.requireNonNull(endDate, "End date");
        ValidationUtil.requireNonNull(expectedIngressTime, "Expected ingress time");
        ValidationUtil.requireNonNull(expectedEgressTime, "Expected egress time");

        RequestType requestType = RequestType.fromString(requestTypeStr != null ? requestTypeStr : "normal");

        // Validate student exists
        if (studentDAO.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found.");
        }

        // Event-specific validations
        if (requestType == RequestType.event) {
            ValidationUtil.requireValidEventName(eventName);
            if (responsiblePerson != null && !responsiblePerson.isBlank()) {
                ValidationUtil.requireValidName(responsiblePerson, "Responsible person");
            }
            if (organization != null && !organization.isBlank()) {
                ValidationUtil.requireValidOrganization(organization);
            }
        }

        // Validate date range
        if (!request.getStartDate().equals(startDate)) {
            validateDateRange(startDate, endDate, requestType);
        } else {
            if (endDate.isBefore(startDate)) {
                throw new BusinessRuleException("End date must not be before start date.");
            }
        }
        validateTimes(startDate, expectedIngressTime, expectedEgressTime, requestType);
        if (requestType == RequestType.event) {
            int maxDays = 7;
            try {
                maxDays = Integer.parseInt(systemSettingDAO.getValue("event_request_max_duration_days", "7"));
            } catch (NumberFormatException ignored) {}

            if (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > maxDays) {
                throw new BusinessRuleException("Event duration exceeds the maximum allowed limit of " + maxDays + " days.");
            }
        }

        // At least one device is required
        if (lineItems == null || lineItems.isEmpty()) {
            throw new BusinessRuleException("At least one device is required.");
        }

        // Validate max devices per student
        int maxDevices = 5;
        try {
            maxDevices = Integer.parseInt(systemSettingDAO.getValue("max_devices_per_student", "5"));
        } catch (NumberFormatException ignored) {}

        int currentDeviceCount = requestDeviceDAO.countApprovedDevicesForStudent(studentId);
        int currentRequestActiveDevices = 0;
        if (request.getStudentId().equals(studentId) && (request.getStatus() == RequestStatus.pending || request.getStatus() == RequestStatus.approved)) {
            for (RequestDevice device : requestDeviceDAO.findByRequestId(requestId)) {
                if (device.getDeviceStatus() == DeviceVerificationStatus.pending || device.getDeviceStatus() == DeviceVerificationStatus.approved) {
                    currentRequestActiveDevices++;
                }
            }
        }
        int otherDeviceCount = Math.max(0, currentDeviceCount - currentRequestActiveDevices);
        if (otherDeviceCount + lineItems.size() > maxDevices) {
            throw new BusinessRuleException(
                    "Maximum devices limit exceeded (current: " + otherDeviceCount + ", adding: " + lineItems.size() + ", max: " + maxDevices + ").");
        }

        // Preserve status and reviewer details if already approved
        RequestStatus originalStatus = request.getStatus();
        Integer originalReviewedBy = request.getReviewedBy();
        LocalDateTime originalReviewedAt = request.getReviewedAt();

        // Update request headers
        request.setRequestType(requestType);
        request.setStudentId(studentId);
        request.setEventName(eventName);
        request.setVenue(venue);
        request.setOrganization(organization);
        request.setResponsiblePerson(responsiblePerson);
        request.setPurpose(purpose);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExpectedIngressTime(expectedIngressTime);
        request.setExpectedEgressTime(expectedEgressTime);
        request.setStatus(originalStatus);
        request.setReviewedBy(originalReviewedBy);
        request.setReviewedAt(originalReviewedAt);
        request.setSubmitted(Boolean.TRUE.equals(isSubmitted));
        request.setAccommodated(Boolean.TRUE.equals(isAccommodated));
        request.setRemarks(remarks);

        requestDAO.update(request);

        // Delete existing devices and insert new ones
        requestDeviceDAO.deleteByRequestId(requestId);

        DeviceVerificationStatus deviceStatus = DeviceVerificationStatus.pending;
        Integer deviceVerifiedBy = null;
        LocalDateTime deviceVerifiedAt = null;
        if (originalStatus == RequestStatus.approved) {
            deviceStatus = DeviceVerificationStatus.approved;
            deviceVerifiedBy = originalReviewedBy != null ? originalReviewedBy : modifierUserId;
            deviceVerifiedAt = originalReviewedAt != null ? originalReviewedAt : LocalDateTime.now();
        }

        for (LineItemRequest lineItem : lineItems) {
            validateLineItem(lineItem);

            RequestDevice device = RequestDevice.builder()
                    .requestId(requestId)
                    .deviceName(lineItem.deviceName)
                    .brand(lineItem.brand)
                    .model(lineItem.model)
                    .deviceType(lineItem.deviceType)
                    .serialNumber(lineItem.serialNumber)
                    .quantity(lineItem.quantity != null ? lineItem.quantity : 1)
                    .imagePath(lineItem.imagePath)
                    .deviceStatus(deviceStatus)
                    .verifiedBy(deviceVerifiedBy)
                    .verifiedAt(deviceVerifiedAt)
                    .remarks(lineItem.remarks)
                    .build();

            requestDeviceDAO.insert(device);
        }

        auditLogService.writeAuditLog(modifierUserId, "REQUEST_UPDATED", "requests",
                String.valueOf(requestId), null, null, null);

        return getRequestById(requestId);
    }

    // ── Request Lifecycle ──────────────────────────────────────────


    @Transactional
    public Request approveRequest(int requestId, int reviewerUserId) {
        Request request = getRequestById(requestId);

        if (request.getStatus() == RequestStatus.approved) {
            return request;
        }

        request.setStatus(RequestStatus.approved);
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setSubmitted(true);
        requestDAO.update(request);

        // Auto-approve all pending devices when request is approved
        for (RequestDevice device : requestDeviceDAO.findByRequestId(requestId)) {
            if (device.getDeviceStatus() == DeviceVerificationStatus.pending) {
                device.setDeviceStatus(DeviceVerificationStatus.approved);
                device.setVerifiedBy(reviewerUserId);
                device.setVerifiedAt(LocalDateTime.now());
                requestDeviceDAO.update(device);
            }
        }

        auditLogService.writeAuditLog(reviewerUserId, "REQUEST_APPROVED", "requests",
                String.valueOf(requestId), null, null, null);

        return getRequestById(requestId);
    }

    @Transactional
    public Request rejectRequest(int requestId, int reviewerUserId, String remarks) {
        Request request = getRequestById(requestId);

        if (request.getStatus() == RequestStatus.rejected) {
            return request;
        }

        ValidationUtil.requireNonBlank(remarks, "Remarks");

        request.setStatus(RequestStatus.rejected);
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRemarks(remarks);
        requestDAO.update(request);

        auditLogService.writeAuditLog(reviewerUserId, "REQUEST_REJECTED", "requests",
                String.valueOf(requestId), null, null, null);

        return getRequestById(requestId);
    }

    @Transactional
    public Request returnRequest(int requestId, int reviewerUserId, String remarks) {
        Request request = getRequestById(requestId);

        request.setStatus(RequestStatus.returned);
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRemarks(remarks);
        requestDAO.update(request);

        auditLogService.writeAuditLog(reviewerUserId, "REQUEST_RETURNED", "requests",
                String.valueOf(requestId), null, null, null);

        return getRequestById(requestId);
    }

    // ── Device Verification ────────────────────────────────────────

    @Transactional
    public RequestDevice verifyDevice(int requestDeviceId, int verifiedBy, String statusStr) {
        RequestDevice device = requestDeviceDAO.findById(requestDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Request device not found."));

        DeviceVerificationStatus newStatus = DeviceVerificationStatus.fromString(statusStr);

        device.setDeviceStatus(newStatus);
        device.setVerifiedBy(verifiedBy);
        device.setVerifiedAt(LocalDateTime.now());
        requestDeviceDAO.update(device);

        auditLogService.writeAuditLog(verifiedBy, "DEVICE_VERIFIED", "request_devices",
                String.valueOf(requestDeviceId), null, null, null);

        return device;
    }

    // ── Validation Helpers ─────────────────────────────────────────

    private void validateLineItem(LineItemRequest lineItem) {
        ValidationUtil.requireNonBlank(lineItem.deviceName, "Device name");
        ValidationUtil.requireNonBlank(lineItem.deviceType, "Device type");
        ValidationUtil.requireNonBlank(lineItem.serialNumber, "Serial number");

        if (lineItem.quantity != null && lineItem.quantity <= 0) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
    }



    private void validateDateRange(LocalDate startDate, LocalDate endDate, RequestType requestType) {
        LocalDate today = LocalDate.now();

        if (startDate.isBefore(today)) {
            throw new BusinessRuleException("Start date cannot be in the past.");
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date must not be before start date.");
        }

        if (requestType == RequestType.event) {
            int maxDays = 7;
            try {
                maxDays = Integer.parseInt(systemSettingDAO.getValue("event_request_max_duration_days", "7"));
            } catch (NumberFormatException ignored) {}

            if (ChronoUnit.DAYS.between(startDate, endDate) > maxDays) {
                throw new BusinessRuleException("Event duration exceeds the maximum allowed limit of " + maxDays + " days.");
            }
        }
    }

    private void validateTimes(LocalDate startDate, LocalTime ingressTime, LocalTime egressTime, RequestType requestType) {
        LocalTime limitStart = LocalTime.of(6, 0);
        LocalTime limitEnd = LocalTime.of(21, 0);

        if (ingressTime.isBefore(limitStart) || ingressTime.isAfter(limitEnd)) {
            throw new BusinessRuleException("Expected ingress time must be between 6:00 AM and 9:00 PM.");
        }
        if (egressTime.isBefore(limitStart) || egressTime.isAfter(limitEnd)) {
            throw new BusinessRuleException("Expected egress time must be between 6:00 AM and 9:00 PM.");
        }
        if (requestType == RequestType.normal && !egressTime.isAfter(ingressTime)) {
            throw new BusinessRuleException("Expected egress time must be after ingress time.");
        }

        if (startDate.equals(LocalDate.now())) {
            if (ingressTime.isBefore(LocalTime.now())) {
                throw new BusinessRuleException("Expected ingress time cannot be in the past for today's request.");
            }
        }
    }

    // ── Inner DTO ──────────────────────────────────────────────────

    public static class LineItemRequest {
        public String deviceName;
        public String brand;
        public String model;
        public String deviceType;
        public String serialNumber;
        public Integer quantity;
        public String imagePath;
        public String remarks;
    }
}
