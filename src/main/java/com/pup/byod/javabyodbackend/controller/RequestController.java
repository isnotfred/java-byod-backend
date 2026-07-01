package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.ActiveRequest;
import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.Request;
import com.pup.byod.javabyodbackend.model.RequestDevice;
import com.pup.byod.javabyodbackend.service.RequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RequestController
 *
 * Unified controller for managing device access requests (normal + event).
 * Replaces both DeviceController and EventRequestController.
 *
 * Base path: /api/requests
 */
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // ── GET Endpoints ───────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Request>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Request> getRequestById(@PathVariable int requestId) {
        return ResponseEntity.ok(requestService.getRequestById(requestId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Request>> getRequestsByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(requestService.getRequestsByStudentId(studentId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Request>> getPendingRequests() {
        return ResponseEntity.ok(requestService.getPendingRequests());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ActiveRequest>> getActiveRequests() {
        return ResponseEntity.ok(requestService.getActiveRequests());
    }

    @GetMapping("/{requestId}/devices")
    public ResponseEntity<List<RequestDevice>> getDevicesForRequest(@PathVariable int requestId) {
        return ResponseEntity.ok(requestService.getDevicesForRequest(requestId));
    }

    @GetMapping("/campus-status")
    public ResponseEntity<List<DeviceCampusStatus>> getCampusStatus() {
        return ResponseEntity.ok(requestService.getCampusStatus());
    }

    @GetMapping("/campus-status/{serialNumber}")
    public ResponseEntity<DeviceCampusStatus> getCampusStatusBySerial(@PathVariable String serialNumber) {
        return ResponseEntity.ok(requestService.getCampusStatusBySerial(serialNumber));
    }

    // ── POST Endpoints ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Request> createRequest(@RequestBody Map<String, Object> body) {
        String requestType = (String) body.get("requestType");
        String studentId = (String) body.get("studentId");
        String eventName = (String) body.get("eventName");
        String venue = (String) body.get("venue");
        String organization = (String) body.get("organization");
        String responsiblePerson = (String) body.get("responsiblePerson");
        String purpose = (String) body.get("purpose");

        LocalDate startDate = body.get("startDate") != null ? LocalDate.parse((String) body.get("startDate")) : null;
        LocalDate endDate = body.get("endDate") != null ? LocalDate.parse((String) body.get("endDate")) : null;
        LocalTime expectedIngressTime = body.get("expectedIngressTime") != null
                ? LocalTime.parse((String) body.get("expectedIngressTime")) : null;
        LocalTime expectedEgressTime = body.get("expectedEgressTime") != null
                ? LocalTime.parse((String) body.get("expectedEgressTime")) : null;

        Boolean isSubmitted = (Boolean) body.get("isSubmitted");
        Boolean isAccommodated = (Boolean) body.get("isAccommodated");
        String remarks = (String) body.get("remarks");
        Integer creatorUserId = body.get("creatorUserId") != null
                ? ((Number) body.get("creatorUserId")).intValue() : null;

        // Parse line items
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("lineItems");
        List<RequestService.LineItemRequest> lineItems = new ArrayList<>();

        if (rawItems != null) {
            for (Map<String, Object> item : rawItems) {
                RequestService.LineItemRequest lineItem = new RequestService.LineItemRequest();
                lineItem.deviceName = (String) item.get("deviceName");
                lineItem.brand = (String) item.get("brand");
                lineItem.model = (String) item.get("model");
                lineItem.deviceType = (String) item.get("deviceType");
                lineItem.serialNumber = (String) item.get("serialNumber");
                lineItem.quantity = item.get("quantity") != null ? ((Number) item.get("quantity")).intValue() : null;
                lineItem.imagePath = (String) item.get("imagePath");
                lineItem.remarks = (String) item.get("remarks");
                lineItems.add(lineItem);
            }
        }

        Request created = requestService.createRequest(
                requestType, studentId, eventName, venue, organization, responsiblePerson,
                purpose, startDate, endDate,
                expectedIngressTime, expectedEgressTime,
                isSubmitted, isAccommodated, remarks, creatorUserId, lineItems
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<Request> updateRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        String requestType = (String) body.get("requestType");
        String studentId = (String) body.get("studentId");
        String eventName = (String) body.get("eventName");
        String venue = (String) body.get("venue");
        String organization = (String) body.get("organization");
        String responsiblePerson = (String) body.get("responsiblePerson");
        String purpose = (String) body.get("purpose");

        LocalDate startDate = body.get("startDate") != null ? LocalDate.parse((String) body.get("startDate")) : null;
        LocalDate endDate = body.get("endDate") != null ? LocalDate.parse((String) body.get("endDate")) : null;
        LocalTime expectedIngressTime = body.get("expectedIngressTime") != null
                ? LocalTime.parse((String) body.get("expectedIngressTime")) : null;
        LocalTime expectedEgressTime = body.get("expectedEgressTime") != null
                ? LocalTime.parse((String) body.get("expectedEgressTime")) : null;

        Boolean isSubmitted = (Boolean) body.get("isSubmitted");
        Boolean isAccommodated = (Boolean) body.get("isAccommodated");
        String remarks = (String) body.get("remarks");
        Integer creatorUserId = body.get("creatorUserId") != null
                ? ((Number) body.get("creatorUserId")).intValue() : null;

        // Parse line items
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("lineItems");
        List<RequestService.LineItemRequest> lineItems = new ArrayList<>();

        if (rawItems != null) {
            for (Map<String, Object> item : rawItems) {
                RequestService.LineItemRequest lineItem = new RequestService.LineItemRequest();
                lineItem.deviceName = (String) item.get("deviceName");
                lineItem.brand = (String) item.get("brand");
                lineItem.model = (String) item.get("model");
                lineItem.deviceType = (String) item.get("deviceType");
                lineItem.serialNumber = (String) item.get("serialNumber");
                lineItem.quantity = item.get("quantity") != null ? ((Number) item.get("quantity")).intValue() : null;
                lineItem.imagePath = (String) item.get("imagePath");
                lineItem.remarks = (String) item.get("remarks");
                lineItems.add(lineItem);
            }
        }

        Request updated = requestService.updateRequest(
                requestId, requestType, studentId, eventName, venue, organization, responsiblePerson,
                purpose, startDate, endDate,
                expectedIngressTime, expectedEgressTime,
                isSubmitted, isAccommodated, remarks, creatorUserId, lineItems
        );

        return ResponseEntity.ok(updated);
    }

    // ── PUT Endpoints ───────────────────────────────────────────────


    @PutMapping("/{requestId}/approve")
    public ResponseEntity<Request> approveRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        int reviewerUserId = ((Number) body.get("reviewerUserId")).intValue();
        return ResponseEntity.ok(requestService.approveRequest(requestId, reviewerUserId));
    }

    @PutMapping("/{requestId}/reject")
    public ResponseEntity<Request> rejectRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        int reviewerUserId = ((Number) body.get("reviewerUserId")).intValue();
        String remarks = (String) body.get("remarks");
        return ResponseEntity.ok(requestService.rejectRequest(requestId, reviewerUserId, remarks));
    }

    @PutMapping("/{requestId}/return")
    public ResponseEntity<Request> returnRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        int reviewerUserId = ((Number) body.get("reviewerUserId")).intValue();
        String remarks = (String) body.get("remarks");
        return ResponseEntity.ok(requestService.returnRequest(requestId, reviewerUserId, remarks));
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<Request> cancelRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        int modifierUserId = ((Number) body.get("modifierUserId")).intValue();
        String remarks = body.containsKey("remarks") ? (String) body.get("remarks") : null;
        return ResponseEntity.ok(requestService.cancelRequest(requestId, modifierUserId, remarks));
    }

    @PutMapping("/devices/{requestDeviceId}/verify")
    public ResponseEntity<RequestDevice> verifyDevice(
            @PathVariable int requestDeviceId,
            @RequestBody Map<String, Object> body) {
        int verifiedBy = ((Number) body.get("verifiedBy")).intValue();
        String status = (String) body.get("deviceStatus");
        return ResponseEntity.ok(requestService.verifyDevice(requestDeviceId, verifiedBy, status));
    }
}
