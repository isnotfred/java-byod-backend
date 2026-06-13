package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.EventRequestDevice;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRequestDeviceDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public EventRequestDeviceDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<EventRequestDevice> rowMapper = (rs, rowNum) -> {
        var deviceBuilder = EventRequestDevice.builder()
                .eventDeviceId(rs.getInt("event_device_id"))
                .eventRequestId(rs.getInt("event_request_id"))
                .deviceName(rs.getString("device_name"))
                .brand(rs.getString("brand"))
                .model(rs.getString("model"))
                .deviceType(rs.getString("device_type"))
                .serialNumber(rs.getString("serial_number"))
                .quantity(rs.getInt("quantity"))
                .verifiedBy(rs.getObject("verified_by") != null ? rs.getInt("verified_by") : null)
                .verifiedAt(rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toLocalDateTime() : null)
                .deviceStatus(rs.getString("device_status"))
                .remarks(rs.getString("remarks"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);

        try {
            deviceBuilder.currentDayStatus(rs.getString("current_day_status"));
        } catch (SQLException e) {
            deviceBuilder.currentDayStatus("exit");
        }

        try {
            deviceBuilder.lastEventTime(rs.getTimestamp("last_event_time") != null ? rs.getTimestamp("last_event_time").toLocalDateTime() : null);
        } catch (SQLException e) {
            deviceBuilder.lastEventTime(null);
        }

        return deviceBuilder.build();
    };

    public Optional<EventRequestDevice> findById(int eventDeviceId) {
        String sql = """
                SELECT erd.*,
                       COALESCE(v.current_day_status, 'exit') AS current_day_status,
                       v.last_event_time
                FROM event_request_devices erd
                LEFT JOIN v_event_device_status v ON erd.event_device_id = v.event_device_id
                WHERE erd.event_device_id = :eventDeviceId""";
        var params = new MapSqlParameterSource("eventDeviceId", eventDeviceId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<EventRequestDevice> findByEventRequestId(int eventRequestId) {
        String sql = """
                SELECT erd.*,
                       COALESCE(v.current_day_status, 'exit') AS current_day_status,
                       v.last_event_time
                FROM event_request_devices erd
                LEFT JOIN v_event_device_status v ON erd.event_device_id = v.event_device_id
                WHERE erd.event_request_id = :eventRequestId
                ORDER BY erd.created_at DESC""";
        var params = new MapSqlParameterSource("eventRequestId", eventRequestId);
        return jdbc.query(sql, params, rowMapper);
    }


    public int insert(EventRequestDevice device) {
        String sql = """
                INSERT INTO event_request_devices (
                    event_request_id,
                    device_name,
                    brand,
                    model,
                    device_type,
                    serial_number,
                    quantity,
                    verified_by,
                    verified_at,
                    device_status,
                    remarks
                ) VALUES (
                    :eventRequestId,
                    :deviceName,
                    :brand,
                    :model,
                    :deviceType::event_device_type,
                    :serialNumber,
                    :quantity,
                    :verifiedBy,
                    :verifiedAt,
                    :deviceStatus,
                    :remarks
                )""";

        var params = new MapSqlParameterSource()
                .addValue("eventRequestId", device.getEventRequestId())
                .addValue("deviceName", device.getDeviceName())
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("deviceType", device.getDeviceType())
                .addValue("serialNumber", device.getSerialNumber())
                .addValue("quantity", device.getQuantity())
                .addValue("verifiedBy", device.getVerifiedBy())
                .addValue("verifiedAt", device.getVerifiedAt() != null ? Timestamp.valueOf(device.getVerifiedAt()) : null)
                .addValue("deviceStatus", device.getDeviceStatus())
                .addValue("remarks", device.getRemarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"event_device_id"});
        return keyHolder.getKey().intValue();
    }

    public int update(EventRequestDevice device) {
        String sql = """
                UPDATE event_request_devices
                SET device_name   = :deviceName,
                    brand         = :brand,
                    model         = :model,
                    device_type   = :deviceType::event_device_type,
                    serial_number = :serialNumber,
                    quantity      = :quantity,
                    verified_by   = :verifiedBy,
                    verified_at   = :verifiedAt,
                    device_status = :deviceStatus,
                    remarks       = :remarks
                WHERE event_device_id = :eventDeviceId""";

        var params = new MapSqlParameterSource()
                .addValue("deviceName", device.getDeviceName())
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("deviceType", device.getDeviceType())
                .addValue("serialNumber", device.getSerialNumber())
                .addValue("quantity", device.getQuantity())
                .addValue("verifiedBy", device.getVerifiedBy())
                .addValue("verifiedAt", device.getVerifiedAt() != null ? Timestamp.valueOf(device.getVerifiedAt()) : null)
                .addValue("deviceStatus", device.getDeviceStatus())
                .addValue("remarks", device.getRemarks())
                .addValue("eventDeviceId", device.getEventDeviceId());

        return jdbc.update(sql, params);
    }

    public List<EventRequestDevice> findReconciliationReport() {
        String sql = """
                SELECT erd.*,
                       COALESCE(v.current_day_status, 'exit') AS current_day_status,
                       v.last_event_time
                FROM event_request_devices erd
                JOIN event_requests er ON er.event_request_id = erd.event_request_id
                JOIN v_event_device_status v ON v.event_device_id = erd.event_device_id
                WHERE er.end_date < CURRENT_DATE
                  AND v.current_day_status = 'entry'
                ORDER BY er.end_date DESC, erd.event_device_id ASC""";
        return jdbc.query(sql, new MapSqlParameterSource(), rowMapper);
    }
}