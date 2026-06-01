package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.Device;
import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.PendingDevice;
import com.pup.byod.javabyodbackend.model.enums.DeviceType;
import com.pup.byod.javabyodbackend.model.enums.RegistrationStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DeviceDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public DeviceDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMappers ───────────────────────────────────────────────────

    private final RowMapper<Device> deviceRowMapper = (rs, rowNum) -> Device.builder()
            .deviceId(rs.getInt("device_id"))
            .studentId(rs.getString("student_id"))
            .deviceType(DeviceType.fromString(rs.getString("device_type")))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .serialNumber(rs.getString("serial_number"))
            .registrationStatus(RegistrationStatus.fromString(rs.getString("registration_status")))
            .deviceStatus(rs.getString("device_status"))
            .devicePurpose(rs.getString("device_purpose"))
            .remarks(rs.getString("remarks"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<PendingDevice> pendingDeviceRowMapper = (rs, rowNum) -> PendingDevice.builder()
            .deviceId(rs.getInt("device_id"))
            .studentId(rs.getString("student_id"))
            .studentFullName(rs.getString("student_full_name"))
            .deviceType(DeviceType.fromString(rs.getString("device_type")))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .serialNumber(rs.getString("serial_number"))
            .devicePurpose(rs.getString("device_purpose"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<DeviceCampusStatus> campusStatusRowMapper = (rs, rowNum) -> DeviceCampusStatus.builder()
            .deviceId(rs.getInt("device_id"))
            .studentId(rs.getString("student_id"))
            .serialNumber(rs.getString("serial_number"))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .deviceType(DeviceType.fromString(rs.getString("device_type")))
            .campusStatus(rs.getString("campus_status"))
            .lastEventTime(rs.getTimestamp("last_event_time") != null
                    ? rs.getTimestamp("last_event_time").toLocalDateTime() : null)
            .build();

    // ── Queries: devices table ───────────────────────────────────────

    public List<Device> findAll() {
        String sql = "SELECT * FROM devices ORDER BY created_at DESC";
        return jdbc.query(sql, deviceRowMapper);
    }

    public Optional<Device> findById(int deviceId) {
        String sql = "SELECT * FROM devices WHERE device_id = :deviceId";
        var params = new MapSqlParameterSource("deviceId", deviceId);
        return jdbc.query(sql, params, deviceRowMapper).stream().findFirst();
    }

    public Optional<Device> findBySerialNumber(String serialNumber) {
        String sql = "SELECT * FROM devices WHERE serial_number = :serialNumber";
        var params = new MapSqlParameterSource("serialNumber", serialNumber);
        return jdbc.query(sql, params, deviceRowMapper).stream().findFirst();
    }

    public List<Device> findByStudentId(String studentId) {
        String sql = "SELECT * FROM devices WHERE student_id = :studentId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, deviceRowMapper);
    }

    public List<Device> findByRegistrationStatus(RegistrationStatus status) {
        String sql = "SELECT * FROM devices WHERE registration_status = :status ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("status", status.name());
        return jdbc.query(sql, params, deviceRowMapper);
    }

    // ── Queries: views ───────────────────────────────────────────────

    public List<PendingDevice> findAllPending() {
        String sql = "SELECT * FROM v_pending_devices ORDER BY created_at ASC";
        return jdbc.query(sql, pendingDeviceRowMapper);
    }

    public List<DeviceCampusStatus> findCampusStatus() {
        String sql = "SELECT * FROM v_device_campus_status";
        return jdbc.query(sql, campusStatusRowMapper);
    }

    public Optional<DeviceCampusStatus> findCampusStatusBySerial(String serialNumber) {
        String sql = "SELECT * FROM v_device_campus_status WHERE serial_number = :serialNumber";
        var params = new MapSqlParameterSource("serialNumber", serialNumber);
        return jdbc.query(sql, params, campusStatusRowMapper).stream().findFirst();
    }

    // ── Mutations ────────────────────────────────────────────────────

    public int insert(Device device) {
        String sql = """
                INSERT INTO devices
                    (student_id, device_type, brand, model, serial_number,
                     registration_status, device_status, device_purpose, remarks)
                VALUES
                    (:studentId, :deviceType, :brand, :model, :serialNumber,
                     :registrationStatus, :deviceStatus, :devicePurpose, :remarks)
                """;

        var params = new MapSqlParameterSource()
                .addValue("studentId", device.getStudentId())
                .addValue("deviceType", device.getDeviceType().name())
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("serialNumber", device.getSerialNumber())
                .addValue("registrationStatus", device.getRegistrationStatus().name())
                .addValue("deviceStatus", device.getDeviceStatus())
                .addValue("devicePurpose", device.getDevicePurpose())
                .addValue("remarks", device.getRemarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"device_id"});
        return keyHolder.getKey().intValue();
    }

    public int updateRegistrationStatus(int deviceId, RegistrationStatus status) {
        String sql = """
                UPDATE devices
                SET registration_status = :status
                WHERE device_id = :deviceId
                """;
        var params = new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("deviceId", deviceId);
        return jdbc.update(sql, params);
    }

    public int updateReviewInfo(int deviceId, int reviewedBy, LocalDateTime reviewedAt) {
        String sql = """
            UPDATE devices
            SET reviewed_by = :reviewedBy,
                reviewed_at = :reviewedAt
            WHERE device_id = :deviceId
            """;
        var params = new MapSqlParameterSource()
                .addValue("reviewedBy", reviewedBy)
                .addValue("reviewedAt", Timestamp.valueOf(reviewedAt))
                .addValue("deviceId", deviceId);
        return jdbc.update(sql, params);
    }

    public int setDeviceStatus(int deviceId, String deviceStatus) {
        String sql = "UPDATE devices SET device_status = :deviceStatus WHERE device_id = :deviceId";
        var params = new MapSqlParameterSource()
                .addValue("deviceStatus", deviceStatus)
                .addValue("deviceId", deviceId);
        return jdbc.update(sql, params);
    }

    public int update(Device device) {
        String sql = """
                UPDATE devices
                SET brand          = :brand,
                    model          = :model,
                    device_purpose = :devicePurpose,
                    remarks        = :remarks
                WHERE device_id = :deviceId
                """;
        var params = new MapSqlParameterSource()
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("devicePurpose", device.getDevicePurpose())
                .addValue("remarks", device.getRemarks())
                .addValue("deviceId", device.getDeviceId());
        return jdbc.update(sql, params);
    }
}
