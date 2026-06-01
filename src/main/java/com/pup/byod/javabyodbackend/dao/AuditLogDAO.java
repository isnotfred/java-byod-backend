package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.AuditLog;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
