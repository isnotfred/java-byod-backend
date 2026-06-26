package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.DeviceScanResponse;
import com.pup.byod.javabyodbackend.model.DeviceTransaction;
import com.pup.byod.javabyodbackend.service.DeviceTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DeviceTransactionController
 *
 * Handles gate scan operations and transaction queries.
 * Replaces DeviceLogController from the old architecture.
 *
 * Base path: /api/transactions
 */
@RestController
@RequestMapping("/api/transactions")
public class DeviceTransactionController {

    private final DeviceTransactionService deviceTransactionService;

    public DeviceTransactionController(DeviceTransactionService deviceTransactionService) {
        this.deviceTransactionService = deviceTransactionService;
    }

    // ── Gate Scan ───────────────────────────────────────────────────

    /**
     * Process a gate scan. Automatically determines whether this is
     * an ingress (check-in) or egress (check-out) based on the current
     * day's transaction state.
     *
     * POST /api/transactions/scan
     * Body: { "serialNumber": "...", "handledBy": 1, "notes": "..." }
     */
    @PostMapping("/scan")
    public ResponseEntity<DeviceScanResponse> processGateScan(@RequestBody Map<String, Object> body) {
        String serialNumber = (String) body.get("serialNumber");
        int handledBy = ((Number) body.get("handledBy")).intValue();
        String notes = (String) body.get("notes");
        String direction = (String) body.get("direction");

        DeviceScanResponse response = deviceTransactionService.processGateScan(serialNumber, handledBy, notes, direction);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch ingress for multiple devices.
     *
     * POST /api/transactions/batch-ingress
     * Body: { "requestDeviceIds": [1, 2, 3], "handledBy": 1 }
     */
    @PostMapping("/batch-ingress")
    public ResponseEntity<List<DeviceScanResponse>> processBatchIngress(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> requestDeviceIds = ((List<Number>) body.get("requestDeviceIds"))
                .stream().map(Number::intValue).toList();
        int handledBy = ((Number) body.get("handledBy")).intValue();

        return ResponseEntity.ok(deviceTransactionService.processBatchIngress(requestDeviceIds, handledBy));
    }

    /**
     * Batch egress for multiple devices.
     *
     * POST /api/transactions/batch-egress
     * Body: { "requestDeviceIds": [1, 2, 3], "handledBy": 1 }
     */
    @PostMapping("/batch-egress")
    public ResponseEntity<List<DeviceScanResponse>> processBatchEgress(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> requestDeviceIds = ((List<Number>) body.get("requestDeviceIds"))
                .stream().map(Number::intValue).toList();
        int handledBy = ((Number) body.get("handledBy")).intValue();

        return ResponseEntity.ok(deviceTransactionService.processBatchEgress(requestDeviceIds, handledBy));
    }

    // ── Reconciliation ──────────────────────────────────────────────

    /**
     * Manually trigger missed checkout reconciliation.
     *
     * POST /api/transactions/reconcile
     */
    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> reconcileMissedCheckouts() {
        int count = deviceTransactionService.reconcileMissedCheckouts();
        return ResponseEntity.ok(Map.of("markedAsMissed", count));
    }

    // ── Query Endpoints ─────────────────────────────────────────────

    /**
     * Get all transactions for a specific device.
     */
    @GetMapping("/device/{requestDeviceId}")
    public ResponseEntity<List<DeviceTransaction>> getTransactionsForDevice(@PathVariable int requestDeviceId) {
        return ResponseEntity.ok(deviceTransactionService.getTransactionsForDevice(requestDeviceId));
    }

    /**
     * Get a single transaction by ID.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<DeviceTransaction> getTransactionById(@PathVariable int transactionId) {
        return ResponseEntity.ok(deviceTransactionService.getTransactionById(transactionId));
    }
}
