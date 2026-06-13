package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.EventDeviceLog;
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
public class EventDeviceLogDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public EventDeviceLogDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<EventDeviceLog> rowMapper = (rs, rowNum) -> EventDeviceLog.builder()
            .eventLogId(rs.getInt("event_log_id"))
            .eventDeviceId(rs.getInt("event_device_id"))
            .eventType(rs.getString("event_type"))
            .eventTime(rs.getTimestamp("event_time") != null ? rs.getTimestamp("event_time").toLocalDateTime() : null)
            .handledBy(rs.getObject("handled_by") != null ? rs.getInt("handled_by") : null)
            .notes(rs.getString("notes"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<EventDeviceLog> findByEventDeviceId(int eventDeviceId, int limit, int offset) {
        String sql = "SELECT * FROM event_device_logs WHERE event_device_id = :eventDeviceId ORDER BY event_time DESC LIMIT :limit OFFSET :offset";
        var params = new MapSqlParameterSource()
                .addValue("eventDeviceId", eventDeviceId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbc.query(sql, params, rowMapper);
    }

    public Optional<EventDeviceLog> findLastLogForDevice(int eventDeviceId) {
        String sql = "SELECT * FROM event_device_logs WHERE event_device_id = :eventDeviceId ORDER BY event_time DESC LIMIT 1";
        var params = new MapSqlParameterSource("eventDeviceId", eventDeviceId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public int insert(EventDeviceLog log) {
        String sql = """
                INSERT INTO event_device_logs (
                    event_device_id,
                    event_type,
                    event_time,
                    handled_by,
                    notes
                ) VALUES (
                    :eventDeviceId,
                    :eventType,
                    :eventTime,
                    :handledBy,
                    :notes
                )""";

        var params = new MapSqlParameterSource()
                .addValue("eventDeviceId", log.getEventDeviceId())
                .addValue("eventType", log.getEventType())
                .addValue("eventTime", log.getEventTime() != null ? Timestamp.valueOf(log.getEventTime()) : null)
                .addValue("handledBy", log.getHandledBy())
                .addValue("notes", log.getNotes());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"event_log_id"});
        return keyHolder.getKey().intValue();
    }
}
