package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.DeviceTransactionDAO;
import com.pup.byod.javabyodbackend.dao.RequestDeviceDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.DeviceScanResponse;
import com.pup.byod.javabyodbackend.model.DeviceTransaction;
import com.pup.byod.javabyodbackend.model.RequestDevice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DeviceTransactionService
 *
 * Handles daily ingress/egress gate scan logic.
 * Replaces DeviceLogService from the old architecture.
 *
 * Key business rules:
 *   - Max 1 transaction per device per day (enforced by DB UNIQUE constraint).
 *   - First scan = ingress (create transaction).
 *   - Second scan = egress (update existing transaction).
 *   - Unclosed transactions from past days are marked as missed egress.
 *   - Only approved devices on approved requests can be scanned.
 */
@Service
public class DeviceTransactionService {

    private final DeviceTransactionDAO deviceTransactionDAO;
    private final RequestDeviceDAO requestDeviceDAO;
    private final AuditLogService auditLogService;

    public DeviceTransactionService(DeviceTransactionDAO deviceTransactionDAO,
                                    RequestDeviceDAO requestDeviceDAO,
                                    AuditLogService auditLogService) {
        this.deviceTransactionDAO = deviceTransactionDAO;
        this.requestDeviceDAO = requestDeviceDAO;
        this.auditLogService = auditLogService;
    }

    // ── Gate Scan Processing ────────────────────────────────────────

    /**
     * Process a gate scan for a device identified by serial number.
     *
     * Logic:
     * 1. Look up the serial number against approved request_devices on active approved requests.
     * 2. Check if a transaction exists for today:
     *    - No transaction → create ingress (check-in)
     *    - Transaction with ingress but no egress → record egress (check-out)
     *    - Transaction already complete → reject (max 1 per day)
     *
     * @param serialNumber The scanned device serial number
     * @param handledBy    The user (guard) who scanned
     * @param notes        Optional notes
     * @return DeviceScanResponse with the result
     */
    @Transactional
    public DeviceScanResponse processGateScan(String serialNumber, int handledBy, String notes, String direction) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BusinessRuleException("Serial number is required.");
        }

        // 1. Reconcile past unclosed transactions before processing today's scan
        deviceTransactionDAO.markUnclosedTransactionsAsMissed();

        // 2. Find the approved device on an active approved request
        RequestDevice device = requestDeviceDAO.findBySerialNumberForActiveRequest(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No approved device found with serial number '" + serialNumber + "' on an active request."));

        // 3. Find if there is an open transaction (egress_time IS NULL)
        Optional<DeviceTransaction> openTx = deviceTransactionDAO.findOpenTransaction(device.getRequestDeviceId());

        // 4. Process based on explicit direction or auto-detect fallback
        if ("ingress".equalsIgnoreCase(direction)) {
            if (openTx.isPresent()) {
                throw new BusinessRuleException(
                        "Device '" + device.getDeviceName() + "' is already checked in (needs to check out first).");
            }

            Optional<DeviceTransaction> todayTx = deviceTransactionDAO.findTodayTransaction(device.getRequestDeviceId());
            if (todayTx.isPresent() && todayTx.get().getEgressTime() != null) {
                throw new BusinessRuleException(
                        "Device '" + device.getDeviceName() + "' has already completed its ingress/egress cycle for today.");
            }

            // Create ingress
            int txId = deviceTransactionDAO.insertIngress(device.getRequestDeviceId(), handledBy, notes);
            auditLogService.writeAuditLog(handledBy, "DEVICE_CHECK_IN", "device_transactions",
                    String.valueOf(txId), null, null, null);

            return DeviceScanResponse.builder()
                    .status("CHECK_IN_SUCCESS")
                    .message("Device '" + device.getDeviceName() + "' checked in successfully.")
                    .device(device)
                    .build();

        } else if ("egress".equalsIgnoreCase(direction)) {
            if (openTx.isEmpty()) {
                throw new BusinessRuleException(
                        "Device '" + device.getDeviceName() + "' is not currently checked in.");
            }

            // Record egress
            DeviceTransaction existingTx = openTx.get();
            deviceTransactionDAO.updateEgress(existingTx.getTransactionId(), handledBy, notes);

            auditLogService.writeAuditLog(handledBy, "DEVICE_CHECK_OUT", "device_transactions",
                    String.valueOf(existingTx.getTransactionId()), null, null, null);

            return DeviceScanResponse.builder()
                    .status("CHECK_OUT_SUCCESS")
                    .message("Device '" + device.getDeviceName() + "' checked out successfully.")
                    .device(device)
                    .build();

        } else {
            // Direction is null (auto-detect / fallback scan)
            if (openTx.isPresent()) {
                // Record egress
                DeviceTransaction existingTx = openTx.get();
                deviceTransactionDAO.updateEgress(existingTx.getTransactionId(), handledBy, notes);

                auditLogService.writeAuditLog(handledBy, "DEVICE_CHECK_OUT", "device_transactions",
                        String.valueOf(existingTx.getTransactionId()), null, null, null);

                return DeviceScanResponse.builder()
                        .status("CHECK_OUT_SUCCESS")
                        .message("Device '" + device.getDeviceName() + "' checked out successfully.")
                        .device(device)
                        .build();
            }

            // Prevent duplicates today
            Optional<DeviceTransaction> todayTx = deviceTransactionDAO.findTodayTransaction(device.getRequestDeviceId());
            if (todayTx.isPresent()) {
                throw new BusinessRuleException(
                        "Device '" + device.getDeviceName() + "' has already completed its ingress/egress cycle for today.");
            }

            // Create ingress
            int txId = deviceTransactionDAO.insertIngress(device.getRequestDeviceId(), handledBy, notes);
            auditLogService.writeAuditLog(handledBy, "DEVICE_CHECK_IN", "device_transactions",
                    String.valueOf(txId), null, null, null);

            return DeviceScanResponse.builder()
                    .status("CHECK_IN_SUCCESS")
                    .message("Device '" + device.getDeviceName() + "' checked in successfully.")
                    .device(device)
                    .build();
        }
    }

    // ── Batch Operations ────────────────────────────────────────────

    /**
     * Process batch ingress for multiple devices.
     */
    @Transactional
    public List<DeviceScanResponse> processBatchIngress(List<Integer> requestDeviceIds, int handledBy) {
        return requestDeviceIds.stream()
                .map(id -> {
                    RequestDevice device = requestDeviceDAO.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + id));
                    return processGateScan(device.getSerialNumber(), handledBy, null, "ingress");
                })
                .toList();
    }

    /**
     * Process batch egress for multiple devices.
     */
    @Transactional
    public List<DeviceScanResponse> processBatchEgress(List<Integer> requestDeviceIds, int handledBy) {
        return requestDeviceIds.stream()
                .map(id -> {
                    RequestDevice device = requestDeviceDAO.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + id));
                    return processGateScan(device.getSerialNumber(), handledBy, null, "egress");
                })
                .toList();
    }

    // ── Reconciliation ──────────────────────────────────────────────

    /**
     * Mark all unclosed past transactions as missed egress.
     * Called manually or via scheduled batch.
     */
    @Transactional
    public int reconcileMissedCheckouts() {
        int count = deviceTransactionDAO.markUnclosedTransactionsAsMissed();
        if (count > 0) {
            auditLogService.writeAuditLog(null, "MISSED_EGRESS_BATCH", "device_transactions",
                    null, null, "{\"count\":" + count + "}", null);
        }
        return count;
    }

    /**
     * Automatically reconcile missed checkouts daily at midnight (12:00 AM Manila time).
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * *")
    public void autoReconcileMissedCheckouts() {
        reconcileMissedCheckouts();
    }

    // ── Query Operations ────────────────────────────────────────────

    public List<DeviceTransaction> getTransactionsForDevice(int requestDeviceId) {
        return deviceTransactionDAO.findByRequestDeviceId(requestDeviceId);
    }

    public DeviceTransaction getTransactionById(int transactionId) {
        return deviceTransactionDAO.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));
    }
}
