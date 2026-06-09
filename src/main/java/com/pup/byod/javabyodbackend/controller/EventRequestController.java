package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.ActiveEventRequest;
import com.pup.byod.javabyodbackend.model.EventRequest;
import com.pup.byod.javabyodbackend.model.EventRequestDevice;
import com.pup.byod.javabyodbackend.service.EventRequestService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/event-requests")
public class EventRequestController {

    private final EventRequestService eventRequestService;

    public EventRequestController(EventRequestService eventRequestService) {
        this.eventRequestService = eventRequestService;
    }

    @GetMapping
    public List<EventRequest> listEventRequests() {
        return eventRequestService.getAllEventRequests();
    }

    @GetMapping("/{eventRequestId}")
    public EventRequest getEventRequest(@PathVariable int eventRequestId) {
        return eventRequestService.getEventRequestById(eventRequestId);
    }

    @GetMapping("/active")
    public List<ActiveEventRequest> getActiveEventRequests() {
        return eventRequestService.getActiveEventRequests();
    }

    @GetMapping("/{eventRequestId}/devices")
    public List<EventRequestDevice> getDevicesForRequest(@PathVariable int eventRequestId) {
        return eventRequestService.getDevicesForRequest(eventRequestId);
    }

    @PostMapping
    public ResponseEntity<EventRequest> createEventRequest(@RequestBody CreateEventRequestRequest request) {
        ValidationUtil.requireNonBlank(request.studentId, "Student ID");
        ValidationUtil.requireNonBlank(request.eventName, "Event name");
        ValidationUtil.requireNonBlank(request.approvalDocType, "Approval document type");

        EventRequest created = eventRequestService.createEventRequest(
                request.studentId,
                request.responsiblePerson,
                request.organization,
                request.eventName,
                request.eventPurpose,
                request.approvalDocType,
                request.approvalDocRef,
                request.startDate,
                request.endDate,
                request.isSubmitted,
                request.isAccommodated,
                request.remarks,
                request.lineItems
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{eventRequestId}/approve")
    public EventRequest approveEventRequest(@PathVariable int eventRequestId, @RequestBody ReviewActionRequest request) {
        ValidationUtil.requireNonNull(request.reviewerUserId, "Reviewer user ID");
        return eventRequestService.approveEventRequest(eventRequestId, request.reviewerUserId);
    }

    @PutMapping("/{eventRequestId}/return")
    public EventRequest returnEventRequest(@PathVariable int eventRequestId, @RequestBody ReturnActionRequest request) {
        ValidationUtil.requireNonNull(request.reviewerUserId, "Reviewer user ID");
        return eventRequestService.returnEventRequest(eventRequestId, request.reviewerUserId, request.remarks);
    }

    @PutMapping("/{eventRequestId}/reject")
    public EventRequest rejectEventRequest(@PathVariable int eventRequestId, @RequestBody RejectActionRequest request) {
        ValidationUtil.requireNonNull(request.reviewerUserId, "Reviewer user ID");
        ValidationUtil.requireNonBlank(request.remarks, "Remarks");
        return eventRequestService.rejectEventRequest(eventRequestId, request.reviewerUserId, request.remarks);
    }

    @PutMapping("/devices/{eventDeviceId}/verify")
    public EventRequestDevice verifyEventRequestDevice(@PathVariable int eventDeviceId, @RequestBody VerifyDeviceRequest request) {
        ValidationUtil.requireNonNull(request.verifiedBy, "Verified by");
        ValidationUtil.requireNonBlank(request.deviceStatus, "Device status");
        return eventRequestService.verifyEventRequestDevice(eventDeviceId, request.verifiedBy, request.deviceStatus);
    }

    public static class CreateEventRequestRequest {
        public String studentId;
        public String responsiblePerson;
        public String organization;
        public String eventName;
        public String eventPurpose;
        public String approvalDocType;
        public String approvalDocRef;
        public LocalDate startDate;
        public LocalDate endDate;
        public Boolean isSubmitted;
        public Boolean isAccommodated;
        public String remarks;
        public List<EventRequestService.LineItemRequest> lineItems;
    }

    public static class ReviewActionRequest {
        public Integer reviewerUserId;
    }

    public static class ReturnActionRequest {
        public Integer reviewerUserId;
        public String remarks;
    }

    public static class RejectActionRequest {
        public Integer reviewerUserId;
        public String remarks;
    }

    public static class VerifyDeviceRequest {
        public Integer verifiedBy;
        public String deviceStatus;
    }
}
