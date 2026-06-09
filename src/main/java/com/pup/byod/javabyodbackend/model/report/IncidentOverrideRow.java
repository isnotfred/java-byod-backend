package com.pup.byod.javabyodbackend.model.report;

import java.time.OffsetDateTime;

/**
 * One row in the Incident / Override Report.
 * Surfaces admin overrides, dispute resolutions, and rejected registrations
 * sourced from audit_logs.
 */
public class IncidentOverrideRow {

    private int            auditId;
    private String         actionType;        // e.g. DEVICE_REJECTED, USER_ROLE_CHANGED
    private String         targetTable;       // e.g. "devices", "users"
    private String         targetId;          // PK of the affected record
    private String         oldValues;         // JSONB serialised as String; parse as needed
    private String         newValues;         // JSONB serialised as String; parse as needed
    private String         ipAddress;
    private OffsetDateTime createdAt;
    private String         performedBy;       // full_name of the acting user; null if deleted
    private String         performerRole;     // role of the acting user at time of action

    // ── Constructors ──────────────────────────────────────────────────────────

    public IncidentOverrideRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int            getAuditId()              { return auditId; }
    public void           setAuditId(int v)          { this.auditId = v; }

    public String         getActionType()            { return actionType; }
    public void           setActionType(String v)    { this.actionType = v; }

    public String         getTargetTable()           { return targetTable; }
    public void           setTargetTable(String v)   { this.targetTable = v; }

    public String         getTargetId()              { return targetId; }
    public void           setTargetId(String v)      { this.targetId = v; }

    public String         getOldValues()             { return oldValues; }
    public void           setOldValues(String v)     { this.oldValues = v; }

    public String         getNewValues()             { return newValues; }
    public void           setNewValues(String v)     { this.newValues = v; }

    public String         getIpAddress()             { return ipAddress; }
    public void           setIpAddress(String v)     { this.ipAddress = v; }

    public OffsetDateTime getCreatedAt()             { return createdAt; }
    public void           setCreatedAt(OffsetDateTime v) { this.createdAt = v; }

    public String         getPerformedBy()           { return performedBy; }
    public void           setPerformedBy(String v)   { this.performedBy = v; }

    public String         getPerformerRole()         { return performerRole; }
    public void           setPerformerRole(String v) { this.performerRole = v; }
}
