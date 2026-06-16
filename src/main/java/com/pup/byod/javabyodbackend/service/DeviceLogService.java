package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.DeviceLogDAO;
import com.pup.byod.javabyodbackend.dao.DeviceDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.Device;
import com.pup.byod.javabyodbackend.model.DeviceLog;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import com.pup.byod.javabyodbackend.dao.SystemSettingDAO;
import com.pup.byod.javabyodbackend.model.enums.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceLogService {

    private final DeviceDAO deviceRepository;
    private final DeviceLogDAO deviceLogDAO;
    private final AuditLogService auditLogService;
    private final SystemSettingDAO systemSettingDAO;

    public DeviceLogService(DeviceDAO deviceRepository, DeviceLogDAO deviceLogDAO, AuditLogService auditLogService, SystemSettingDAO systemSettingDAO) {
        this.deviceRepository = deviceRepository;
        this.deviceLogDAO = deviceLogDAO;
        this.auditLogService = auditLogService;
        this.systemSettingDAO = systemSettingDAO;
    }

    public List<DeviceLog> getLogsByDeviceId(int deviceId, int limit, int offset) {
        return deviceLogDAO.findByDeviceId(deviceId, limit, offset);
    }

    public List<DeviceLog> getLogsByStudentId(String studentId, int limit, int offset) {
        return deviceLogDAO.findByStudentId(studentId, limit, offset);
    }

    @Transactional
    public DeviceLog logEntry(String serialNumber, Integer handledBy, String notes) {
        ValidationUtil.requireNonBlank(serialNumber, "Serial number");
        ValidationUtil.requireNonNull(handledBy, "Handled by");
        return insertDeviceLog(serialNumber, "entry", handledBy, "manual", false, notes);
    }

    @Transactional
    public DeviceLog logExit(String serialNumber, Integer handledBy, String notes) {
        ValidationUtil.requireNonBlank(serialNumber, "Serial number");
        ValidationUtil.requireNonNull(handledBy, "Handled by");
        return insertDeviceLog(serialNumber, "exit", handledBy, "manual", false, notes);
    }

    @Transactional
    public List<DeviceLog> runAutoExitBatch() {
        List<DeviceLog> inserted = new ArrayList<>();
        List<DeviceLog> latest = new ArrayList<>();

        for (DeviceLog existing : deviceLogDAO.findByDeviceId(0, 0, 0)) {
            latest.add(existing);
        }

        String cutoffStr = systemSettingDAO.getValue("auto_exit_cutoff_time", "22:00");
        LocalTime cutoff = LocalTime.parse("22:00");
        try {
            cutoff = LocalTime.parse(cutoffStr);
        } catch (Exception e) {
            // fallback
        }

        LocalDateTime now = LocalDateTime.now();
        boolean passedCutoffToday = !now.toLocalTime().isBefore(cutoff);

        var campusStatuses = deviceRepository.findCampusStatus();
        for (var status : campusStatuses) {
            if (!"entry".equalsIgnoreCase(status.getCampusStatus())) {
                continue;
            }

            LocalDateTime lastEventTime = status.getLastEventTime();
            if (lastEventTime == null) {
                continue;
            }

            boolean isPreviousDay = lastEventTime.toLocalDate().isBefore(now.toLocalDate());
            boolean isTodayBeforeCutoff = lastEventTime.toLocalDate().isEqual(now.toLocalDate())
                    && passedCutoffToday
                    && lastEventTime.toLocalTime().isBefore(cutoff);

            if (isPreviousDay || isTodayBeforeCutoff) {
                Device device = deviceRepository.findById(status.getDeviceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Device not found."));

                DeviceLog insertedLog = insertDeviceLog(
                        device.getSerialNumber(),
                        "exit",
                        null,
                        "automatic",
                        true,
                        "Automatic logout batch"
                );
                inserted.add(insertedLog);
            }
        }

        return inserted;
    }

    private DeviceLog insertDeviceLog(String serialNumber,
                                      String eventType,
                                      Integer handledBy,
                                      String logoutType,
                                      boolean autoExit,
                                      String notes) {
        ValidationUtil.requireNonBlank(eventType, "Event type");

        Device device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));

        boolean allowUnregistered = Boolean.parseBoolean(
                systemSettingDAO.getValue("allow_unregistered_devices", "true")
        );

        if (device.getRegistrationStatus() == null) {
            throw new BusinessRuleException("Device registration status is unknown.");
        }

        RegistrationStatus status = device.getRegistrationStatus();
        if (allowUnregistered) {
            if (status != RegistrationStatus.approved && status != RegistrationStatus.pending) {
                if (!("exit".equalsIgnoreCase(eventType) && status == RegistrationStatus.rejected)) {
                    throw new BusinessRuleException("Device is not approved or pending and cannot be logged.");
                }
            }
        } else {
            if (status != RegistrationStatus.approved) {
                if (!("exit".equalsIgnoreCase(eventType) && status == RegistrationStatus.rejected)) {
                    throw new BusinessRuleException("Device is not approved and cannot be logged.");
                }
            }
        }

        if (device.getDeviceStatus() != null && "inactive".equalsIgnoreCase(device.getDeviceStatus())) {
            throw new BusinessRuleException("Device is inactive and cannot be logged.");
        }

        if (!autoExit && handledBy == null) {
            throw new BusinessRuleException("Handled by is required for manual gate scans.");
        }

        DeviceLog lastLog = deviceLogDAO.findLastLogForDevice(device.getDeviceId()).orElse(null);
        if (lastLog != null && lastLog.getEventType() != null && lastLog.getEventType().equalsIgnoreCase(eventType) && !autoExit) {
            throw new BusinessRuleException("Device already has a consecutive '" + eventType + "' event.");
        }

        if (autoExit && lastLog != null && lastLog.getEventType() != null && !lastLog.getEventType().equalsIgnoreCase("entry")) {
            return lastLog;
        }

        DeviceLog log = DeviceLog.builder()
                .deviceId(device.getDeviceId())
                .studentId(device.getStudentId())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .handledBy(autoExit ? null : handledBy)
                .logoutType(autoExit ? "automatic" : logoutType)
                .autoExit(autoExit)
                .notes(notes)
                .build();

        int logId = deviceLogDAO.insert(log);
        DeviceLog saved = deviceLogDAO.findByDeviceId(device.getDeviceId(), 1, 0).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Device log not created."));

        if (autoExit) {
            auditLogService.writeAuditLog(null, "DEVICE_AUTO_EXIT", "device_logs", String.valueOf(logId), null, null, null);
        } else if ("entry".equalsIgnoreCase(eventType)) {
            auditLogService.writeAuditLog(handledBy, "DEVICE_ENTRY", "device_logs", String.valueOf(logId), null, null, null);
        } else {
            auditLogService.writeAuditLog(handledBy, "DEVICE_EXIT", "device_logs", String.valueOf(logId), null, null, null);
        }

        return saved;
    }
}
