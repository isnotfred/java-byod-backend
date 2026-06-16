package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.DeviceLog;
import com.pup.byod.javabyodbackend.service.DeviceLogService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-logs")
public class DeviceLogController {

    private final DeviceLogService deviceLogService;

    public DeviceLogController(DeviceLogService deviceLogService) {
        this.deviceLogService = deviceLogService;
    }

    @GetMapping("/devices/{deviceId}")
    public List<DeviceLog> getLogsByDeviceId(
            @PathVariable int deviceId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return deviceLogService.getLogsByDeviceId(deviceId, limit, offset);
    }

    @GetMapping("/students/{studentId}")
    public List<DeviceLog> getLogsByStudentId(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return deviceLogService.getLogsByStudentId(studentId, limit, offset);
    }

    @PostMapping("/entry")
    public ResponseEntity<DeviceLog> logEntry(@RequestBody GateScanRequest request) {
        ValidationUtil.requireValidSerialNumber(request.serialNumber);
        ValidationUtil.requireNonNull(request.handledBy, "Handled by");

        DeviceLog log = deviceLogService.logEntry(request.serialNumber, request.handledBy, request.notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PostMapping("/exit")
    public ResponseEntity<DeviceLog> logExit(@RequestBody GateScanRequest request) {
        ValidationUtil.requireValidSerialNumber(request.serialNumber);
        ValidationUtil.requireNonNull(request.handledBy, "Handled by");

        DeviceLog log = deviceLogService.logExit(request.serialNumber, request.handledBy, request.notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PostMapping("/auto-exit")
    public List<DeviceLog> runAutoExitBatch() {
        return deviceLogService.runAutoExitBatch();
    }

    public static class GateScanRequest {
        public String serialNumber;
        public Integer handledBy;
        public String notes;
    }
}
