package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.EventRequestDAO;
import com.pup.byod.javabyodbackend.dao.EventRequestDeviceDAO;
import com.pup.byod.javabyodbackend.dao.StudentRepository;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.ActiveEventRequest;
import com.pup.byod.javabyodbackend.model.EventRequest;
import com.pup.byod.javabyodbackend.model.EventRequestDevice;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventRequestService {

    private final EventRequestDAO eventRequestDAO;
    private final EventRequestDeviceDAO eventRequestDeviceDAO;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    public EventRequestService(EventRequestDAO eventRequestDAO,
                               EventRequestDeviceDAO eventRequestDeviceDAO,
                               StudentRepository studentRepository,
                               AuditLogService auditLogService) {
        this.eventRequestDAO = eventRequestDAO;
        this.eventRequestDeviceDAO = eventRequestDeviceDAO;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
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
        List<ActiveEventRequest> results = new ArrayList<>();
        for (EventRequest request : eventRequestDAO.findActiveRequests()) {
            var student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
            var devices = eventRequestDeviceDAO.findByEventRequestId(request.getEventRequestId());

            results.add(ActiveEventRequest.builder()
                    .eventRequestId(request.getEventRequestId())
                    .studentId(request.getStudentId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .eventName(request.getEventName())
                    .organization(request.getOrganization())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .status(request.getStatus())
                    .deviceCount(devices.size())
                    .build());
        }
        return results;
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
                .status("pending")
                .isSubmitted(Boolean.TRUE.equals(isSubmitted))
                .isAccommodated(Boolean.TRUE.equals(isAccommodated))
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
                    .deviceStatus("pending")
                    .remarks(lineItem.remarks)
                    .build();

            eventRequestDeviceDAO.insert(device);
        }

        auditLogService.writeAuditLog(null, "EVENT_REQUEST_CREATED", "event_requests", String.valueOf(eventRequestId), null, null, null);
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

    private void validateApprovalDocType(String approvalDocType) {
        if (!"Paper Approval".equals(approvalDocType) && !"Signed GPOA".equals(approvalDocType)) {
            throw new BusinessRuleException("Approval document type must be Paper Approval or Signed GPOA.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date must not be before start date.");
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
