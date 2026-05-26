package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.DeviceLog;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class DeviceLogDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public DeviceLogDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<DeviceLog> rowMapper = (rs, rowNum) -> DeviceLog.builder()
            .logId(rs.getInt("log_id"))
            .deviceId(rs.getInt("device_id"))
            .studentId(rs.getString("student_id"))
            .eventType(rs.getString("event_type"))
            .eventTime(rs.getTimestamp("event_time") != null ? rs.getTimestamp("event_time").toLocalDateTime() : null)
            .handledBy(rs.getObject("handled_by") != null ? rs.getInt("handled_by") : null)
            .logoutType(rs.getString("logout_type"))
            .autoExit(rs.getBoolean("auto_exit"))
            .notes(rs.getString("notes"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<DeviceLog> findByDeviceId(int deviceId, int limit, int offset) {
        String sql = "SELECT * FROM device_logs WHERE device_id = :deviceId ORDER BY event_time DESC LIMIT :limit OFFSET :offset";
        var params = new MapSqlParameterSource()
                .addValue("deviceId", deviceId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbc.query(sql, params, rowMapper);
    }

    public List<DeviceLog> findByStudentId(String studentId, int limit, int offset) {
        String sql = "SELECT * FROM device_logs WHERE student_id = :studentId ORDER BY event_time DESC LIMIT :limit OFFSET :offset";
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbc.query(sql, params, rowMapper);
    }

    public Optional<DeviceLog> findLastLogForDevice(int deviceId) {
        String sql = "SELECT * FROM device_logs WHERE device_id = :deviceId ORDER BY event_time DESC LIMIT 1";
        var params = new MapSqlParameterSource("deviceId", deviceId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public int insert(DeviceLog log) {
        String sql = """
                INSERT INTO device_logs (
                    device_id,
                    student_id,
                    event_type,
                    event_time,
                    handled_by,
                    logout_type,
                    auto_exit,
                    notes
                ) VALUES (
                    :deviceId,
                    :studentId,
                    :eventType,
                    :eventTime,
                    :handledBy,
                    :logoutType,
                    :autoExit,
                    :notes
                )""";

        var params = new MapSqlParameterSource()
                .addValue("deviceId", log.getDeviceId())
                .addValue("studentId", log.getStudentId())
                .addValue("eventType", log.getEventType())
                .addValue("eventTime", log.getEventTime() != null ? Timestamp.valueOf(log.getEventTime()) : null)
                .addValue("handledBy", log.getHandledBy())
                .addValue("logoutType", log.getLogoutType())
                .addValue("autoExit", log.getAutoExit())
                .addValue("notes", log.getNotes());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"log_id"});
        return keyHolder.getKey().intValue();
    }
}
