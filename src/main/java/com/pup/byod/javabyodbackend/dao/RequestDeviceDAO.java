package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.DeviceCampusStatus;
import com.pup.byod.javabyodbackend.model.RequestDevice;
import com.pup.byod.javabyodbackend.model.enums.DeviceVerificationStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RequestDeviceDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public RequestDeviceDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<RequestDevice> deviceRowMapper = (rs, rowNum) -> RequestDevice.builder()
            .requestDeviceId(rs.getInt("request_device_id"))
            .requestId(rs.getInt("request_id"))
            .deviceName(rs.getString("device_name"))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .deviceType(rs.getString("device_type"))
            .serialNumber(rs.getString("serial_number"))
            .quantity(rs.getInt("quantity"))
            .imagePath(rs.getString("image_path"))
            .deviceStatus(DeviceVerificationStatus.fromString(rs.getString("device_status")))
            .verifiedBy(rs.getObject("verified_by") != null ? rs.getInt("verified_by") : null)
            .verifiedAt(rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toLocalDateTime() : null)
            .remarks(rs.getString("remarks"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<DeviceCampusStatus> campusStatusRowMapper = (rs, rowNum) -> DeviceCampusStatus.builder()
            .requestDeviceId(rs.getInt("request_device_id"))
            .requestId(rs.getInt("request_id"))
            .studentId(rs.getString("student_id"))
            .deviceName(rs.getString("device_name"))
            .serialNumber(rs.getString("serial_number"))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .deviceType(rs.getString("device_type"))
            .deviceStatus(rs.getString("device_status"))
            .requestType(rs.getString("request_type"))
            .campusStatus(rs.getString("campus_status"))
            .lastEventTime(rs.getTimestamp("last_event_time") != null ? rs.getTimestamp("last_event_time").toLocalDateTime() : null)
            .noEgressMarked(rs.getBoolean("no_egress_marked"))
            .build();

    // ── Queries ──────────────────────────────────────────────────────

    public List<RequestDevice> findByRequestId(int requestId) {
        String sql = "SELECT * FROM request_devices WHERE request_id = :requestId ORDER BY created_at ASC";
        var params = new MapSqlParameterSource("requestId", requestId);
        return jdbc.query(sql, params, deviceRowMapper);
    }

    public Optional<RequestDevice> findById(int requestDeviceId) {
        String sql = "SELECT * FROM request_devices WHERE request_device_id = :requestDeviceId";
        var params = new MapSqlParameterSource("requestDeviceId", requestDeviceId);
        return jdbc.query(sql, params, deviceRowMapper).stream().findFirst();
    }

    /**
     * Find a device by serial number within an approved request valid for the given date.
     * Used by gate scan to resolve a serial number to the correct request device.
     */
    public Optional<RequestDevice> findBySerialNumberForActiveRequest(String serialNumber) {
        String sql = """
                SELECT rd.* FROM request_devices rd
                JOIN requests r ON r.request_id = rd.request_id
                LEFT JOIN device_transactions dt ON dt.request_device_id = rd.request_device_id AND dt.egress_time IS NULL
                WHERE rd.serial_number = :serialNumber
                  AND rd.device_status = 'approved'
                  AND r.status = 'approved'
                  AND (
                    (r.start_date <= CURRENT_DATE AND r.end_date >= CURRENT_DATE)
                    OR
                    (dt.transaction_id IS NOT NULL)
                  )
                ORDER BY r.created_at DESC
                LIMIT 1
                """;
        var params = new MapSqlParameterSource("serialNumber", serialNumber);
        return jdbc.query(sql, params, deviceRowMapper).stream().findFirst();
    }

    /**
     * Find all campus status records from the v_device_campus_status view.
     */
    public List<DeviceCampusStatus> findCampusStatus() {
        String sql = "SELECT * FROM v_device_campus_status ORDER BY last_event_time DESC NULLS LAST";
        return jdbc.query(sql, campusStatusRowMapper);
    }

    /**
     * Find campus status by serial number.
     */
    public Optional<DeviceCampusStatus> findCampusStatusBySerial(String serialNumber) {
        String sql = "SELECT * FROM v_device_campus_status WHERE serial_number = :serialNumber";
        var params = new MapSqlParameterSource("serialNumber", serialNumber);
        return jdbc.query(sql, params, campusStatusRowMapper).stream().findFirst();
    }

    /**
     * Count approved devices for pending/approved requests by student.
     */
    public int countApprovedDevicesForStudent(String studentId) {
        String sql = """
                SELECT COUNT(*)
                FROM request_devices rd
                JOIN requests r ON r.request_id = rd.request_id
                WHERE r.student_id = :studentId
                  AND r.status IN ('pending', 'approved')
                  AND rd.device_status IN ('pending', 'approved')
                """;
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.queryForObject(sql, params, Integer.class);
    }

    // ── Mutations ────────────────────────────────────────────────────

    public int insert(RequestDevice device) {
        String sql = """
                INSERT INTO request_devices (
                    request_id, device_name, brand, model, device_type, serial_number,
                    quantity, image_path, device_status, verified_by, verified_at, remarks
                ) VALUES (
                    :requestId, :deviceName, :brand, :model, :deviceType, :serialNumber,
                    :quantity, :imagePath, :deviceStatus, :verifiedBy, :verifiedAt, :remarks
                )
                """;

        var params = new MapSqlParameterSource()
                .addValue("requestId", device.getRequestId())
                .addValue("deviceName", device.getDeviceName())
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("deviceType", device.getDeviceType())
                .addValue("serialNumber", device.getSerialNumber())
                .addValue("quantity", device.getQuantity())
                .addValue("imagePath", device.getImagePath())
                .addValue("deviceStatus", device.getDeviceStatus() != null ? device.getDeviceStatus().name() : "pending")
                .addValue("verifiedBy", device.getVerifiedBy())
                .addValue("verifiedAt", device.getVerifiedAt())
                .addValue("remarks", device.getRemarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"request_device_id"});
        return keyHolder.getKey().intValue();
    }

    public int update(RequestDevice device) {
        String sql = """
                UPDATE request_devices
                SET device_name   = :deviceName,
                    brand         = :brand,
                    model         = :model,
                    device_type   = :deviceType,
                    serial_number = :serialNumber,
                    quantity      = :quantity,
                    image_path    = :imagePath,
                    device_status = :deviceStatus,
                    verified_by   = :verifiedBy,
                    verified_at   = :verifiedAt,
                    remarks       = :remarks
                WHERE request_device_id = :requestDeviceId
                """;

        var params = new MapSqlParameterSource()
                .addValue("deviceName", device.getDeviceName())
                .addValue("brand", device.getBrand())
                .addValue("model", device.getModel())
                .addValue("deviceType", device.getDeviceType())
                .addValue("serialNumber", device.getSerialNumber())
                .addValue("quantity", device.getQuantity())
                .addValue("imagePath", device.getImagePath())
                .addValue("deviceStatus", device.getDeviceStatus() != null ? device.getDeviceStatus().name() : "pending")
                .addValue("verifiedBy", device.getVerifiedBy())
                .addValue("verifiedAt", device.getVerifiedAt())
                .addValue("remarks", device.getRemarks())
                .addValue("requestDeviceId", device.getRequestDeviceId());

        return jdbc.update(sql, params);
    }

    public int setDeviceStatus(int requestDeviceId, String status) {
        String sql = "UPDATE request_devices SET device_status = :status WHERE request_device_id = :requestDeviceId";
        var params = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("requestDeviceId", requestDeviceId);
        return jdbc.update(sql, params);
    }

    public int deleteByRequestId(int requestId) {
        String sql = "DELETE FROM request_devices WHERE request_id = :requestId";
        var params = new MapSqlParameterSource("requestId", requestId);
        return jdbc.update(sql, params);
    }

    public int delete(int requestDeviceId) {
        String sql = "DELETE FROM request_devices WHERE request_device_id = :requestDeviceId";
        var params = new MapSqlParameterSource("requestDeviceId", requestDeviceId);
        return jdbc.update(sql, params);
    }
}
