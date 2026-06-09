package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.SystemSettingDAO;
import com.pup.byod.javabyodbackend.dao.UserDAO;
import com.pup.byod.javabyodbackend.exception.ForbiddenException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.SystemSetting;
import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemSettingService {

    private final SystemSettingDAO systemSettingDAO;
    private final UserDAO userDAO;
    private final AuditLogService auditLogService;

    public SystemSettingService(SystemSettingDAO systemSettingDAO,
                                UserDAO userDAO,
                                AuditLogService auditLogService) {
        this.systemSettingDAO = systemSettingDAO;
        this.userDAO = userDAO;
        this.auditLogService = auditLogService;
    }

    public List<SystemSetting> getAllSettings() {
        return systemSettingDAO.findAll();
    }

    @Transactional
    public SystemSetting updateSetting(int actingUserId, String key, String value) {
        User actor = userDAO.findById(actingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Acting user not found."));

        if (actor.getRole() != Role.super_admin) {
            throw new ForbiddenException("Super admin access required.");
        }

        SystemSetting existing = systemSettingDAO.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting key '" + key + "' not found."));

        String oldValue = existing.getSettingValue();
        systemSettingDAO.update(key, value);

        SystemSetting updated = systemSettingDAO.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting key not found after update."));

        String oldPayload = String.format("{\"%s\":\"%s\"}", key, oldValue);
        String newPayload = String.format("{\"%s\":\"%s\"}", key, value);

        auditLogService.writeAuditLog(
                actingUserId,
                "SYSTEM_CONFIG_UPDATED",
                "system_settings",
                key,
                oldPayload,
                newPayload,
                null
        );

        return updated;
    }
}
