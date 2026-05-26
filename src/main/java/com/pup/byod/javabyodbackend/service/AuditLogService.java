package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.AuditLogDAO;
import com.pup.byod.javabyodbackend.model.AuditLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogDAO auditLogDAO;

    public AuditLogService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    public void writeAuditLog(
            Integer userId,
            String actionType,
            String targetTable,
            String targetId,
            String oldValues,
            String newValues,
            String ipAddress
    ) {
        auditLogDAO.writeAuditLog(userId, actionType, targetTable, targetId, oldValues, newValues, ipAddress);
    }

    public List<AuditLog> findAll(int limit, int offset) {
        return auditLogDAO.findAll(limit, offset);
    }

    public List<AuditLog> findByUserId(int userId) {
        return auditLogDAO.findByUserId(userId);
    }

    public List<AuditLog> findByActionType(String actionType) {
        return auditLogDAO.findByActionType(actionType);
    }
}