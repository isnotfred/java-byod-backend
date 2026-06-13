package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.DeviceDAO;
import com.pup.byod.javabyodbackend.dao.StudentDAO;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class DeviceService {

    private final DeviceDAO deviceDAO;
    private final StudentDAO studentDAO;
    private final AuditLogService auditLogService;

    public DeviceService(DeviceDAO deviceDAO, StudentDAO studentDAO, AuditLogService auditLogService) {
        this.deviceDAO = deviceDAO;
        this.studentDAO = studentDAO;
        this.auditLogService = auditLogService;
    }

    public List<Device> getAllDevices() {
        return deviceDAO.findAll();
    }

    public Device getDeviceById(int deviceId) {
        return deviceDAO.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));
    }

    public Device getDeviceBySerial(String serialNumber) {
        return deviceDAO.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));
    }

    public List<Device> getDevicesByStudentId(String studentId) {
        return deviceDAO.findByStudentId(studentId);
    }

    public List<PendingDevice> getPendingDevices() {
        return deviceDAO.findAllPending();
    }

    public List<DeviceCampusStatus> getCampusStatus() {
        return deviceDAO.findCampusStatus();
    }

    public DeviceCampusStatus getCampusStatusBySerial(String serialNumber) {
        return deviceDAO.findCampusStatusBySerial(serialNumber)
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

        if (studentDAO.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found.");
        }

        if (deviceDAO.findBySerialNumber(serialNumber).isPresent()) {
            throw new BusinessRuleException("Serial number already exists.");
        }

        DeviceType parsedType = DeviceType.fromString(deviceType);
        RegistrationStatus parsedStatus = registrationStatus == null
                ? RegistrationStatus.pending
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

        int deviceId = deviceDAO.insert(device);
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

        deviceDAO.update(updated);
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

        deviceDAO.updateRegistrationStatus(deviceId, RegistrationStatus.approved);
        deviceDAO.updateReviewInfo(deviceId, reviewedBy, LocalDateTime.now());
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

        deviceDAO.update(Device.builder()
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

        deviceDAO.updateRegistrationStatus(deviceId, RegistrationStatus.rejected);
        deviceDAO.updateReviewInfo(deviceId, reviewedBy, LocalDateTime.now());
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

        deviceDAO.setDeviceStatus(deviceId, "inactive");
        auditLogService.writeAuditLog(null, "DEVICE_DEACTIVATED", "devices", String.valueOf(deviceId), null, null, null);
    }

    @Transactional
    public Map<String, Object> importFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("CSV file is required.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessRuleException("File must be a .csv file.");
        }

        int inserted = 0;
        int skipped = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<String> allowedPurposes = Set.of(
                "Academic BYOD",
                "School Event",
                "Organization Activity",
                "Temporary Equipment",
                "Other Approved Purpose",
                "PROTOTYPE",
                "APPLIANCE"
        );

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream()));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader("student_id", "device_name", "brand", "model", "serial_number", "device_type", "device_purpose", "registration_status", "remarks")
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build())
        ) {
            for (CSVRecord record : csvParser) {
                int rowNum = (int) record.getRecordNumber();

                String studentId = record.get("student_id");
                String deviceName = record.get("device_name");
                String brand = record.get("brand");
                String model = record.get("model");
                String serialNumber = record.get("serial_number");
                String deviceType = record.get("device_type");
                String devicePurpose = record.get("device_purpose");
                String registrationStatus = record.get("registration_status");
                String remarks = record.get("remarks");

                List<String> rowErrors = new ArrayList<>();

                if (studentId == null || studentId.isBlank()) {
                    rowErrors.add("student_id is required.");
                } else if (studentDAO.findById(studentId).isEmpty()) {
                    rowErrors.add("Student with ID '" + studentId + "' not found.");
                }

                if (deviceName == null || deviceName.isBlank()) {
                    rowErrors.add("device_name is required.");
                }

                if (serialNumber == null || serialNumber.isBlank()) {
                    rowErrors.add("serial_number is required.");
                } else {
                    try {
                        ValidationUtil.requireValidSerialNumber(serialNumber);
                    } catch (Exception e) {
                        rowErrors.add(e.getMessage());
                    }
                }

                DeviceType parsedType = null;
                if (deviceType == null || deviceType.isBlank()) {
                    rowErrors.add("device_type is required.");
                } else {
                    try {
                        parsedType = DeviceType.fromString(deviceType);
                    } catch (Exception e) {
                        rowErrors.add(e.getMessage());
                    }
                }

                if (devicePurpose == null || devicePurpose.isBlank()) {
                    rowErrors.add("device_purpose is required.");
                } else if (!allowedPurposes.contains(devicePurpose.trim())) {
                    rowErrors.add("device_purpose must be one of: " + allowedPurposes);
                }

                RegistrationStatus parsedStatus = RegistrationStatus.pending;
                if (registrationStatus != null && !registrationStatus.isBlank()) {
                    try {
                        parsedStatus = RegistrationStatus.fromString(registrationStatus);
                    } catch (Exception e) {
                        rowErrors.add(e.getMessage());
                    }
                }

                if (!rowErrors.isEmpty()) {
                    Map<String, Object> errorEntry = new LinkedHashMap<>();
                    errorEntry.put("row", rowNum);
                    errorEntry.put("serialNumber", serialNumber);
                    errorEntry.put("reasons", rowErrors);
                    errors.add(errorEntry);
                    continue;
                }

                // Skip duplicates
                if (deviceDAO.findBySerialNumber(serialNumber).isPresent()) {
                    skipped++;
                    continue;
                }

                // Insert
                try {
                    Device device = Device.builder()
                            .studentId(studentId.trim())
                            .deviceName(deviceName.trim())
                            .brand(brand != null ? brand.trim() : null)
                            .model(model != null ? model.trim() : null)
                            .serialNumber(serialNumber.trim())
                            .deviceType(parsedType)
                            .devicePurpose(devicePurpose.trim())
                            .registrationStatus(parsedStatus)
                            .deviceStatus("active")
                            .remarks(remarks != null ? remarks.trim() : null)
                            .build();

                    int deviceId = deviceDAO.insert(device);
                    auditLogService.writeAuditLog(null, "DEVICE_REGISTERED", "devices", String.valueOf(deviceId), null, null, null);
                    inserted++;
                } catch (Exception e) {
                    Map<String, Object> errorEntry = new LinkedHashMap<>();
                    errorEntry.put("row", rowNum);
                    errorEntry.put("serialNumber", serialNumber);
                    errorEntry.put("reasons", List.of(e.getMessage()));
                    errors.add(errorEntry);
                }
            }

        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read CSV file: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }
}
