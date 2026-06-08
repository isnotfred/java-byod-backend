package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.AuditLog;
import com.pup.byod.javabyodbackend.model.report.IncidentOverrideRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AuditLogDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public AuditLogDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<AuditLog> auditLogRowMapper = (rs, rowNum) -> AuditLog.builder()
            .auditId(rs.getInt("audit_id"))
            .userId(rs.getObject("user_id") != null ? rs.getInt("user_id") : null)
            .actionType(rs.getString("action_type"))
            .targetTable(rs.getString("target_table"))
            .targetId(rs.getString("target_id"))
            .oldValues(rs.getString("old_values"))
            .newValues(rs.getString("new_values"))
            .ipAddress(rs.getString("ip_address"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<IncidentOverrideRow> incidentOverrideRowMapper = (rs, rowNum) -> {
        var row = new IncidentOverrideRow();
        row.setAuditId(rs.getInt("audit_id"));
        row.setActionType(rs.getString("action_type"));
        row.setTargetTable(rs.getString("target_table"));
        row.setTargetId(rs.getString("target_id"));
        row.setOldValues(rs.getString("old_values"));
        row.setNewValues(rs.getString("new_values"));
        row.setIpAddress(rs.getString("ip_address"));
        row.setCreatedAt(getOffsetDateTime(rs, "created_at"));
        row.setPerformedBy(rs.getString("performed_by"));
        row.setPerformerRole(rs.getString("performer_role"));
        return row;
    };

    // ── Write via function (NEVER direct INSERT) ──────────────────────

    /**
     * Write an audit entry by calling fn_write_audit_log().
     * This is the ONLY way to insert into audit_logs.
     * Always call this inside the same transaction as the triggering write.
     *
     * @param userId      ID of the acting user (can be null for system actions)
     * @param actionType  e.g. "DEVICE_APPROVED"
     * @param targetTable e.g. "devices"
     * @param targetId    PK of the affected row as a string
     * @param oldValues   JSONB-compatible string of before-state (or null)
     * @param newValues   JSONB-compatible string of after-state (or null)
     * @param ipAddress   Client IP address (or null)
     */
    public void writeAuditLog(
            Integer userId,
            String actionType,
            String targetTable,
            String targetId,
            String oldValues,
            String newValues,
            String ipAddress
    ) {
        String sql = """
                SELECT fn_write_audit_log(
                    :userId,
                    :actionType,
                    :targetTable,
                    :targetId,
                    :oldValues::jsonb,
                    :newValues::jsonb,
                    :ipAddress
                )
                """;

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("actionType", actionType)
                .addValue("targetTable", targetTable)
                .addValue("targetId", targetId)
                .addValue("oldValues", oldValues)
                .addValue("newValues", newValues)
                .addValue("ipAddress", ipAddress);

        jdbc.query(sql, params, rs -> null);
    }

    // ── Queries ──────────────────────────────────────────────────────

    public List<AuditLog> findAll(int limit, int offset) {
        String sql = """
                SELECT * FROM audit_logs
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbc.query(sql, params, auditLogRowMapper);
    }

    public List<AuditLog> findByUserId(int userId) {
        String sql = "SELECT * FROM audit_logs WHERE user_id = :userId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbc.query(sql, params, auditLogRowMapper);
    }

    public List<AuditLog> findByActionType(String actionType) {
        String sql = "SELECT * FROM audit_logs WHERE action_type = :actionType ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("actionType", actionType);
        return jdbc.query(sql, params, auditLogRowMapper);
    }

    public List<AuditLog> findByTargetTable(String targetTable) {
        String sql = "SELECT * FROM audit_logs WHERE target_table = :targetTable ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("targetTable", targetTable);
        return jdbc.query(sql, params, auditLogRowMapper);
    }

    public List<IncidentOverrideRow> getIncidentOverrides(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    al.audit_id,
                    al.action_type,
                    al.target_table,
                    al.target_id,
                    al.old_values::text AS old_values,
                    al.new_values::text AS new_values,
                    al.ip_address,
                    al.created_at,
                    u.full_name AS performed_by,
                    u.role AS performer_role
                FROM audit_logs al
                LEFT JOIN users u ON u.user_id = al.user_id
                WHERE al.action_type IN (
                    'DEVICE_REJECTED',
                    'DEVICE_DEACTIVATED',
                    'ADMIN_DEACTIVATED',
                    'GUARD_DEACTIVATED_BY_SUPER',
                    'USER_ROLE_CHANGED',
                    'DEVICE_AUTO_EXIT',
                    'EVENT_REQUEST_REJECTED',
                    'EVENT_REQUEST_RETURNED'
                )
                  AND al.created_at >= :from
                  AND al.created_at < CAST(:to AS date) + INTERVAL '1 day'
                ORDER BY al.created_at DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(sql, params, incidentOverrideRowMapper);
    }

    private OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class);
    }
}
