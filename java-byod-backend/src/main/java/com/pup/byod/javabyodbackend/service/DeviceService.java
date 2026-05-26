package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.DeviceRepository;
import com.pup.byod.javabyodbackend.dao.StudentRepository;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.Device;
import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.PendingDevice;
import com.pup.byod.javabyodbackend.model.enums.DeviceType;
import com.pup.byod.javabyodbackend.model.enums.RegistrationStatus;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    public DeviceService(DeviceRepository deviceRepository, StudentRepository studentRepository, AuditLogService auditLogService) {
        this.deviceRepository = deviceRepository;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Device getDeviceById(int deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));
    }

    public Device getDeviceBySerial(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));
    }

    public List<Device> getDevicesByStudentId(String studentId) {
        return deviceRepository.findByStudentId(studentId);
    }

    public List<PendingDevice> getPendingDevices() {
        return deviceRepository.findPendingDevices();
    }

    public List<DeviceCampusStatus> getCampusStatus() {
        return deviceRepository.findCampusStatus();
    }

    public DeviceCampusStatus getCampusStatusBySerial(String serialNumber) {
        return deviceRepository.findCampusStatusBySerial(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device status not found."));
    }

    @Transactional
    public Device registerDevice(String studentId,
                                String deviceName,
                                String brand,
                                String model,
                                String serialNumber,
                                String deviceType,
                                String devicePurpose,
                                String registrationStatus,
                                String remarks,
                                String imagePath) {
        ValidationUtil.requireNonBlank(studentId, "Student ID");
        ValidationUtil.requireValidSerialNumber(serialNumber);
        ValidationUtil.requireNonBlank(deviceType, "Device type");
        ValidationUtil.requireNonBlank(devicePurpose, "Device purpose");

        if (studentRepository.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found.");
        }

        if (deviceRepository.findBySerialNumber(serialNumber).isPresent()) {
            throw new BusinessRuleException("Serial number already exists.");
        }

        DeviceType parsedType = DeviceType.fromString(deviceType);
        RegistrationStatus parsedStatus = registrationStatus == null
                ? RegistrationStatus.approved
                : RegistrationStatus.fromString(registrationStatus);

        Device device = Device.builder()
                .studentId(studentId)
                .deviceName(deviceName)
                .brand(brand)
                .model(model)
                .serialNumber(serialNumber)
                .deviceType(parsedType)
                .devicePurpose(devicePurpose)
                .registrationStatus(parsedStatus)
                .deviceStatus("active")
                .remarks(remarks)
                .imagePath(imagePath)
                .build();

        int deviceId = deviceRepository.insert(device);
        Device saved = getDeviceById(deviceId);
        auditLogService.writeAuditLog(null, "DEVICE_REGISTERED", "devices", String.valueOf(deviceId), null, null, null);
        return saved;
    }

    @Transactional
    public Device updateDevice(int deviceId,
                              String deviceName,
                              String brand,
                              String model,
                              String devicePurpose,
                              String remarks,
                              String imagePath) {
        Device existing = getDeviceById(deviceId);

        Device updated = Device.builder()
                .deviceId(existing.getDeviceId())
                .deviceName(deviceName)
                .brand(brand)
                .model(model)
                .devicePurpose(devicePurpose)
                .remarks(remarks)
                .imagePath(imagePath)
                .studentId(existing.getStudentId())
                .serialNumber(existing.getSerialNumber())
                .deviceType(existing.getDeviceType())
                .registrationStatus(existing.getRegistrationStatus())
                .deviceStatus(existing.getDeviceStatus())
                .reviewedBy(existing.getReviewedBy())
                .reviewedAt(existing.getReviewedAt())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        deviceRepository.update(updated);
        Device saved = getDeviceById(deviceId);
        auditLogService.writeAuditLog(null, "DEVICE_UPDATED", "devices", String.valueOf(deviceId), null, null, null);
        return saved;
    }

    @Transactional
    public Device approveDevice(int deviceId, int reviewedBy) {
        Device existing = getDeviceById(deviceId);

        if (existing.getRegistrationStatus() != null && existing.getRegistrationStatus() == RegistrationStatus.approved) {
            return existing;
        }

        if (existing.getRegistrationStatus() != null && existing.getRegistrationStatus() == RegistrationStatus.rejected) {
            throw new BusinessRuleException("Cannot go directly from rejected to approved. Reset to pending first.");
        }

        if (existing.getDeviceStatus() != null && existing.getDeviceStatus().equalsIgnoreCase("inactive")) {
            throw new BusinessRuleException("Inactive devices cannot be approved.");
        }

        deviceRepository.updateRegistrationStatus(deviceId, RegistrationStatus.approved, reviewedBy, LocalDateTime.now());
        Device saved = getDeviceById(deviceId);
        auditLogService.writeAuditLog(reviewedBy, "DEVICE_APPROVED", "devices", String.valueOf(deviceId), null, null, null);
        return saved;
    }

    @Transactional
    public Device rejectDevice(int deviceId, int reviewedBy, String remarks) {
        Device existing = getDeviceById(deviceId);

        if (existing.getRegistrationStatus() != null && existing.getRegistrationStatus() == RegistrationStatus.rejected) {
            return existing;
        }

        if (existing.getRegistrationStatus() != null && existing.getRegistrationStatus() == RegistrationStatus.approved) {
            throw new BusinessRuleException("Cannot go directly from approved to rejected. Set device_status to inactive instead.");
        }

        ValidationUtil.requireNonBlank(remarks, "Remarks");

        deviceRepository.update(Device.builder()
                .deviceId(existing.getDeviceId())
                .studentId(existing.getStudentId())
                .deviceName(existing.getDeviceName())
                .brand(existing.getBrand())
                .model(existing.getModel())
                .serialNumber(existing.getSerialNumber())
                .deviceType(existing.getDeviceType())
                .devicePurpose(existing.getDevicePurpose())
                .registrationStatus(existing.getRegistrationStatus())
                .deviceStatus(existing.getDeviceStatus())
                .reviewedBy(existing.getReviewedBy())
                .reviewedAt(existing.getReviewedAt())
                .remarks(remarks)
                .imagePath(existing.getImagePath())
                .build());

        deviceRepository.updateRegistrationStatus(deviceId, RegistrationStatus.rejected, reviewedBy, LocalDateTime.now());
        Device saved = getDeviceById(deviceId);
        auditLogService.writeAuditLog(reviewedBy, "DEVICE_REJECTED", "devices", String.valueOf(deviceId), null, null, null);
        return saved;
    }

    @Transactional
    public void deactivateDevice(int deviceId) {
        Device existing = getDeviceById(deviceId);

        if (existing.getDeviceStatus() != null && existing.getDeviceStatus().equalsIgnoreCase("inactive")) {
            return;
        }

        deviceRepository.setDeviceStatus(deviceId, "inactive");
        auditLogService.writeAuditLog(null, "DEVICE_DEACTIVATED", "devices", String.valueOf(deviceId), null, null, null);
    }
}
