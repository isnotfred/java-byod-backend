package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Maps the audit_logs table.
 * This table is immutable — rows are written only via fn_write_audit_log().
 * The backend never INSERTs into this table directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private Integer auditId;
    private Integer userId;         // who performed the action (FK → users.user_id)
    private String actionType;      // e.g. DEVICE_APPROVED, USER_LOGIN
    private String targetTable;     // e.g. devices, students
    private String targetId;        // PK of the affected row (stored as VARCHAR)
    private String oldValues;       // JSONB serialised as String
    private String newValues;       // JSONB serialised as String
    private String ipAddress;
    private LocalDateTime createdAt;
}
