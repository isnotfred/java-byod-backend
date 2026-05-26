package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.AuditLog;
import com.pup.byod.javabyodbackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLog> listAuditLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return auditLogService.findAll(limit, offset);
    }

    @GetMapping("/user/{userId}")
    public List<AuditLog> getByUserId(@PathVariable int userId) {
        return auditLogService.findByUserId(userId);
    }

    @GetMapping("/action/{actionType}")
    public List<AuditLog> getByActionType(@PathVariable String actionType) {
        return auditLogService.findByActionType(actionType);
    }
}