package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.EventDeviceLogDAO;
import com.pup.byod.javabyodbackend.model.EventDeviceLog;
import com.pup.byod.javabyodbackend.dao.EventRequestDAO;
import com.pup.byod.javabyodbackend.dao.EventRequestDeviceDAO;
import com.pup.byod.javabyodbackend.dao.StudentDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.ActiveEventRequest;
import com.pup.byod.javabyodbackend.model.EventRequest;
import com.pup.byod.javabyodbackend.model.EventRequestDevice;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import com.pup.byod.javabyodbackend.dao.SystemSettingDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventRequestService {

    private final EventRequestDAO eventRequestDAO;
    private final EventRequestDeviceDAO eventRequestDeviceDAO;
    private final StudentDAO studentRepository;
    private final AuditLogService auditLogService;
    private final EventDeviceLogDAO eventDeviceLogDAO;
    private final SystemSettingDAO systemSettingDAO;

    public EventRequestService(EventRequestDAO eventRequestDAO,
                               EventRequestDeviceDAO eventRequestDeviceDAO,
                               StudentDAO studentRepository,
                               AuditLogService auditLogService,
                               EventDeviceLogDAO eventDeviceLogDAO,
                               SystemSettingDAO systemSettingDAO) {
        this.eventRequestDAO = eventRequestDAO;
        this.eventRequestDeviceDAO = eventRequestDeviceDAO;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
        this.eventDeviceLogDAO = eventDeviceLogDAO;
        this.systemSettingDAO = systemSettingDAO;
    }


    public List<EventRequest> getAllEventRequests() {
        return eventRequestDAO.findAll();
    }

    public EventRequest getEventRequestById(int eventRequestId) {
        return eventRequestDAO.findById(eventRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Event request not found."));
    }

    public List<EventRequestDevice> getDevicesForRequest(int eventRequestId) {
        return eventRequestDeviceDAO.findByEventRequestId(eventRequestId);
    }

    public List<ActiveEventRequest> getActiveEventRequests() {
        return eventRequestDAO.findActiveRequests();
    }

    @Transactional
    public EventRequest createEventRequest(String studentId,
                                           String responsiblePerson,
                                           String organization,
                                           String eventName,
                                           String eventPurpose,
                                           String approvalDocType,
                                           String approvalDocRef,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Boolean isSubmitted,
                                           Boolean isAccommodated,
                                           String remarks,
                                           Integer creatorUserId,
                                           List<LineItemRequest> lineItems) {
        ValidationUtil.requireNonBlank(studentId, "Student ID");
        ValidationUtil.requireNonBlank(eventName, "Event name");
        ValidationUtil.requireNonBlank(approvalDocType, "Approval document type");

        if (studentRepository.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found.");
        }

        if (lineItems == null || lineItems.isEmpty()) {
            throw new BusinessRuleException("At least one event request device is required.");
        }

        validateApprovalDocType(approvalDocType);
        validateDateRange(startDate, endDate);

        EventRequest request = EventRequest.builder()
                .studentId(studentId)
                .responsiblePerson(responsiblePerson)
                .organization(organization)
                .eventName(eventName)
                .eventPurpose(eventPurpose)
                .approvalDocType(approvalDocType)
                .approvalDocRef(approvalDocRef)
                .startDate(startDate)
                .endDate(endDate)
                .status("approved")
                .isSubmitted(true)
                .isAccommodated(Boolean.TRUE.equals(isAccommodated))
                .reviewedBy(creatorUserId)
                .reviewedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();

        int eventRequestId = eventRequestDAO.insert(request);

        for (LineItemRequest lineItem : lineItems) {
            EventRequestDevice device = EventRequestDevice.builder()
                    .eventRequestId(eventRequestId)
                    .deviceName(lineItem.deviceName)
                    .brand(lineItem.brand)
                    .model(lineItem.model)
                    .deviceType(lineItem.deviceType)
                    .serialNumber(lineItem.serialNumber)
                    .quantity(lineItem.quantity == null ? 1 : lineItem.quantity)
                    .deviceStatus("approved")
                    .remarks(lineItem.remarks)
                    .build();

            eventRequestDeviceDAO.insert(device);
        }

        auditLogService.writeAuditLog(creatorUserId, "EVENT_REQUEST_CREATED", "event_requests", String.valueOf(eventRequestId), null, null, null);
        auditLogService.writeAuditLog(creatorUserId, "EVENT_REQUEST_APPROVED", "event_requests", String.valueOf(eventRequestId), null, null, null);
        return getEventRequestById(eventRequestId);
    }


    @Transactional
    public EventRequest approveEventRequest(int eventRequestId, int reviewerUserId) {
        EventRequest request = getEventRequestById(eventRequestId);
        request.setStatus("approved");
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setIsSubmitted(true);

        eventRequestDAO.update(request);
        for (EventRequestDevice device : eventRequestDeviceDAO.findByEventRequestId(eventRequestId)) {
            device.setDeviceStatus("approved");
            device.setVerifiedBy(null);
            device.setVerifiedAt(null);
            eventRequestDeviceDAO.update(device);
        }

        auditLogService.writeAuditLog(reviewerUserId, "EVENT_REQUEST_APPROVED", "event_requests", String.valueOf(eventRequestId), null, null, null);
        return getEventRequestById(eventRequestId);
    }

    @Transactional
    public EventRequest returnEventRequest(int eventRequestId, int reviewerUserId, String remarks) {
        EventRequest request = getEventRequestById(eventRequestId);
        request.setStatus("returned");
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRemarks(remarks);
        eventRequestDAO.update(request);
        auditLogService.writeAuditLog(reviewerUserId, "EVENT_REQUEST_RETURNED", "event_requests", String.valueOf(eventRequestId), null, null, null);
        return getEventRequestById(eventRequestId);
    }

    @Transactional
    public EventRequest rejectEventRequest(int eventRequestId, int reviewerUserId, String remarks) {
        EventRequest request = getEventRequestById(eventRequestId);
        request.setStatus("rejected");
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRemarks(remarks);
        eventRequestDAO.update(request);
        auditLogService.writeAuditLog(reviewerUserId, "EVENT_REQUEST_REJECTED", "event_requests", String.valueOf(eventRequestId), null, null, null);
        return getEventRequestById(eventRequestId);
    }

    @Transactional
    public EventRequestDevice verifyEventRequestDevice(int eventDeviceId, int verifiedBy, String deviceStatus) {
        EventRequestDevice device = eventRequestDeviceDAO.findById(eventDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Event request device not found."));

        if (!List.of("pending", "approved", "returned").contains(deviceStatus)) {
            throw new BusinessRuleException("Invalid event request device status.");
        }

        device.setDeviceStatus(deviceStatus);
        device.setVerifiedBy(verifiedBy);
        device.setVerifiedAt(LocalDateTime.now());
        eventRequestDeviceDAO.update(device);
        return device;
    }

    public List<ActiveEventRequest> getGuardEventRequests() {
        return eventRequestDAO.findApprovedActiveRequestsForGuard();
    }

    @Transactional
    public void logDeviceEntry(List<Integer> deviceIds, int guardId) {
        for (int eventDeviceId : deviceIds) {
            EventRequestDevice device = eventRequestDeviceDAO.findById(eventDeviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event device not found."));

            if (!"approved".equalsIgnoreCase(device.getDeviceStatus())) {
                throw new BusinessRuleException("Device " + device.getDeviceName() + " is not approved.");
            }

            var lastLog = eventDeviceLogDAO.findLastLogForDevice(eventDeviceId).orElse(null);
            if (lastLog != null && "entry".equalsIgnoreCase(lastLog.getEventType())) {
                throw new BusinessRuleException("Device " + device.getDeviceName() + " is already checked in.");
            }

            EventDeviceLog log = EventDeviceLog.builder()
                    .eventDeviceId(eventDeviceId)
                    .eventType("entry")
                    .eventTime(LocalDateTime.now())
                    .handledBy(guardId)
                    .build();
            eventDeviceLogDAO.insert(log);

            auditLogService.writeAuditLog(guardId, "DEVICE_ENTRY", "event_request_devices", String.valueOf(eventDeviceId), null, null, null);
        }
    }

    @Transactional
    public void logDeviceExit(List<Integer> deviceIds, int guardId) {
        for (int eventDeviceId : deviceIds) {
            EventRequestDevice device = eventRequestDeviceDAO.findById(eventDeviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event device not found."));

            if (!"approved".equalsIgnoreCase(device.getDeviceStatus())) {
                throw new BusinessRuleException("Device " + device.getDeviceName() + " is not approved.");
            }

            var lastLog = eventDeviceLogDAO.findLastLogForDevice(eventDeviceId).orElse(null);
            if (lastLog == null || "exit".equalsIgnoreCase(lastLog.getEventType())) {
                throw new BusinessRuleException("Device " + device.getDeviceName() + " is already checked out.");
            }

            EventDeviceLog log = EventDeviceLog.builder()
                    .eventDeviceId(eventDeviceId)
                    .eventType("exit")
                    .eventTime(LocalDateTime.now())
                    .handledBy(guardId)
                    .build();
            eventDeviceLogDAO.insert(log);

            auditLogService.writeAuditLog(guardId, "DEVICE_EXIT", "event_request_devices", String.valueOf(eventDeviceId), null, null, null);
        }
    }

    public List<EventRequestDevice> getReconciliationReport() {
        return eventRequestDeviceDAO.findReconciliationReport();
    }

    private void validateApprovalDocType(String approvalDocType) {
        if (!"Paper Approval".equals(approvalDocType) && !"Signed GPOA".equals(approvalDocType)) {
            throw new BusinessRuleException("Approval document type must be Paper Approval or Signed GPOA.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new BusinessRuleException("End date must not be before start date.");
            }

            int maxDays = 7;
            try {
                maxDays = Integer.parseInt(systemSettingDAO.getValue("event_request_max_duration_days", "7"));
            } catch (NumberFormatException e) {
                // fallback
            }

            if (ChronoUnit.DAYS.between(startDate, endDate) > maxDays) {
                throw new BusinessRuleException("Event duration exceeds the maximum allowed limit of " + maxDays + " days.");
            }
        }
    }

    public static class LineItemRequest {
        public String deviceName;
        public String brand;
        public String model;
        public String deviceType;
        public String serialNumber;
        public Integer quantity;
        public String remarks;
    }
}
