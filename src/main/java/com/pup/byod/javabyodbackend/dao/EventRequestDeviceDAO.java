package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.EventRequestDevice;
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
public class EventRequestDeviceDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public EventRequestDeviceDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<EventRequestDevice> rowMapper = (rs, rowNum) -> EventRequestDevice.builder()
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
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public Optional<EventRequestDevice> findById(int eventDeviceId) {
        String sql = "SELECT * FROM event_request_devices WHERE event_device_id = :eventDeviceId";
        var params = new MapSqlParameterSource("eventDeviceId", eventDeviceId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<EventRequestDevice> findByEventRequestId(int eventRequestId) {
        String sql = "SELECT * FROM event_request_devices WHERE event_request_id = :eventRequestId ORDER BY created_at DESC";
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
}