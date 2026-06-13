package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.Device;
import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.PendingDevice;
import com.pup.byod.javabyodbackend.model.enums.DeviceType;
import com.pup.byod.javabyodbackend.service.DeviceService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<Device> listDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{deviceId}")
    public Device getDevice(@PathVariable int deviceId) {
        return deviceService.getDeviceById(deviceId);
    }

    @GetMapping("/serial/{serialNumber}")
    public Device getDeviceBySerial(@PathVariable String serialNumber) {
        return deviceService.getDeviceBySerial(serialNumber);
    }

    @GetMapping("/student/{studentId}")
    public List<Device> getDevicesByStudent(@PathVariable String studentId) {
        return deviceService.getDevicesByStudentId(studentId);
    }

    @GetMapping("/pending")
    public List<PendingDevice> listPendingDevices() {
        return deviceService.getPendingDevices();
    }

    @GetMapping("/campus-status")
    public List<DeviceCampusStatus> getCampusStatus() {
        return deviceService.getCampusStatus();
    }

    @GetMapping("/campus-status/{serialNumber}")
    public DeviceCampusStatus getCampusStatusBySerial(@PathVariable String serialNumber) {
        return deviceService.getCampusStatusBySerial(serialNumber);
    }

    @PostMapping
    public ResponseEntity<Device> registerDevice(@RequestBody RegisterDeviceRequest request) {
        ValidationUtil.requireNonBlank(request.studentId, "Student ID");
        ValidationUtil.requireNonBlank(request.deviceName, "Device name");
        ValidationUtil.requireNonBlank(request.deviceType, "Device type");
        ValidationUtil.requireNonBlank(request.devicePurpose, "Device purpose");
        ValidationUtil.requireValidSerialNumber(request.serialNumber);

        DeviceType.fromString(request.deviceType);

        Device created = deviceService.registerDevice(
                request.studentId,
                request.deviceName,
                request.brand,
                request.model,
                request.serialNumber,
                request.deviceType,
                request.devicePurpose,
                request.registrationStatus,
                request.remarks,
                request.imagePath
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{deviceId}")
    public Device updateDevice(@PathVariable int deviceId, @RequestBody UpdateDeviceRequest request) {
        ValidationUtil.requireNonBlank(request.deviceName, "Device name");
        ValidationUtil.requireNonBlank(request.devicePurpose, "Device purpose");

        return deviceService.updateDevice(
                deviceId,
                request.deviceName,
                request.brand,
                request.model,
                request.devicePurpose,
                request.remarks,
                request.imagePath
        );
    }

    @PutMapping("/{deviceId}/approve")
    public Device approveDevice(@PathVariable int deviceId, @RequestBody ReviewDeviceRequest request) {
        ValidationUtil.requireNonNull(request.reviewedBy, "Reviewed by");
        return deviceService.approveDevice(deviceId, request.reviewedBy);
    }

    @PutMapping("/{deviceId}/reject")
    public Device rejectDevice(@PathVariable int deviceId, @RequestBody RejectDeviceRequest request) {
        ValidationUtil.requireNonNull(request.reviewedBy, "Reviewed by");
        ValidationUtil.requireNonBlank(request.remarks, "Remarks");
        return deviceService.rejectDevice(deviceId, request.reviewedBy, request.remarks);
    }

    @PutMapping("/{deviceId}/deactivate")
    public Map<String, Object> deactivateDevice(@PathVariable int deviceId) {
        deviceService.deactivateDevice(deviceId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Device deactivated.");
        return body;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importDevices(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = deviceService.importFromCsv(file);
        return ResponseEntity.ok(result);
    }

    public static class RegisterDeviceRequest {
        public String studentId;
        public String deviceName;
        public String brand;
        public String model;
        public String serialNumber;
        public String deviceType;
        public String devicePurpose;
        public String registrationStatus;
        public String remarks;
        public String imagePath;
    }

    public static class UpdateDeviceRequest {
        public String deviceName;
        public String brand;
        public String model;
        public String devicePurpose;
        public String remarks;
        public String imagePath;
    }

    public static class ReviewDeviceRequest {
        public Integer reviewedBy;
    }

    public static class RejectDeviceRequest {
        public Integer reviewedBy;
        public String remarks;
    }

    private void requireNonNull(Object value, String fieldName) {
        ValidationUtil.requireNonNull(value, fieldName);
    }
}
