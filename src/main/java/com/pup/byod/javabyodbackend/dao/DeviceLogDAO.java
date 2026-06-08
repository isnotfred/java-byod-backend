package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.DeviceLog;
import com.pup.byod.javabyodbackend.model.report.DailyTrafficRow;
import com.pup.byod.javabyodbackend.model.report.DeviceFrequencyRow;
import com.pup.byod.javabyodbackend.model.report.MonthlyTrafficRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    private final RowMapper<DailyTrafficRow> dailyTrafficRowMapper = (rs, rowNum) -> {
        var row = new DailyTrafficRow();
        row.setLogId(rs.getInt("log_id"));
        row.setEventType(rs.getString("event_type"));
        row.setEventTime(getOffsetDateTime(rs, "event_time"));
        row.setAutoExit(rs.getBoolean("auto_exit"));
        row.setLogoutType(rs.getString("logout_type"));
        row.setNotes(rs.getString("notes"));
        row.setDeviceId(rs.getInt("device_id"));
        row.setDeviceName(rs.getString("device_name"));
        row.setSerialNumber(rs.getString("serial_number"));
        row.setDeviceType(rs.getString("device_type"));
        row.setRegistrationStatus(rs.getString("registration_status"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setHandledByName(rs.getString("handled_by_name"));
        return row;
    };

    private final RowMapper<MonthlyTrafficRow> monthlyTrafficRowMapper = (rs, rowNum) -> {
        var row = new MonthlyTrafficRow();
        row.setReportMonth(rs.getObject("report_month", LocalDate.class));
        row.setDeviceCategory(rs.getString("device_category"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setEntryCount(rs.getInt("entry_count"));
        row.setExitCount(rs.getInt("exit_count"));
        row.setTotalEvents(rs.getInt("total_events"));
        return row;
    };

    private final RowMapper<DeviceFrequencyRow> deviceFrequencyRowMapper = (rs, rowNum) -> {
        var row = new DeviceFrequencyRow();
        row.setDeviceId(rs.getInt("device_id"));
        row.setDeviceName(rs.getString("device_name"));
        row.setSerialNumber(rs.getString("serial_number"));
        row.setDeviceType(rs.getString("device_type"));
        row.setBrand(rs.getString("brand"));
        row.setModel(rs.getString("model"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setEntryCount(rs.getInt("entry_count"));
        row.setExitCount(rs.getInt("exit_count"));
        row.setFirstSeen(getOffsetDateTime(rs, "first_seen"));
        row.setLastSeen(getOffsetDateTime(rs, "last_seen"));
        return row;
    };

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

    public List<DailyTrafficRow> getDailyTraffic(LocalDate date, String studentId, String deviceType, String status) {
        String sql = """
                SELECT
                    dl.log_id,
                    dl.event_type,
                    dl.event_time,
                    dl.auto_exit,
                    dl.logout_type,
                    dl.notes,
                    dl.device_id,
                    d.device_name,
                    d.serial_number,
                    d.device_type,
                    d.registration_status,
                    dl.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    u.full_name AS handled_by_name
                FROM device_logs dl
                JOIN devices d ON d.device_id = dl.device_id
                JOIN students s ON s.student_id = dl.student_id
                LEFT JOIN users u ON u.user_id = dl.handled_by
                WHERE dl.event_time::date = :date
                  AND (:studentId IS NULL OR dl.student_id = :studentId)
                  AND (:deviceType IS NULL OR d.device_type = :deviceType)
                  AND (:status IS NULL OR d.registration_status = :status)
                ORDER BY dl.event_time DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("date", date)
                .addValue("studentId", studentId)
                .addValue("deviceType", deviceType)
                .addValue("status", status);
        return jdbc.query(sql, params, dailyTrafficRowMapper);
    }

    public List<MonthlyTrafficRow> getMonthlyTraffic(int year, int month) {
        String sql = """
                SELECT
                    DATE_TRUNC('month', dl.event_time)::date AS report_month,
                    d.device_type AS device_category,
                    dl.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    COUNT(*) FILTER (WHERE dl.event_type = 'entry') AS entry_count,
                    COUNT(*) FILTER (WHERE dl.event_type = 'exit') AS exit_count,
                    COUNT(*) AS total_events
                FROM device_logs dl
                JOIN devices d ON d.device_id = dl.device_id
                JOIN students s ON s.student_id = dl.student_id
                WHERE EXTRACT(YEAR FROM dl.event_time) = :year
                  AND EXTRACT(MONTH FROM dl.event_time) = :month
                GROUP BY report_month, d.device_type,
                         dl.student_id, s.first_name, s.last_name, s.course_year_level
                ORDER BY d.device_type, student_name
                """;
        var params = new MapSqlParameterSource()
                .addValue("year", year)
                .addValue("month", month);
        return jdbc.query(sql, params, monthlyTrafficRowMapper);
    }

    public List<DeviceFrequencyRow> getDeviceFrequency(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    dl.device_id,
                    d.device_name,
                    d.serial_number,
                    d.device_type,
                    d.brand,
                    d.model,
                    dl.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    COUNT(*) FILTER (WHERE dl.event_type = 'entry') AS entry_count,
                    COUNT(*) FILTER (WHERE dl.event_type = 'exit') AS exit_count,
                    MIN(dl.event_time) AS first_seen,
                    MAX(dl.event_time) AS last_seen
                FROM device_logs dl
                JOIN devices d ON d.device_id = dl.device_id
                JOIN students s ON s.student_id = dl.student_id
                WHERE dl.event_time >= :from
                  AND dl.event_time < CAST(:to AS date) + INTERVAL '1 day'
                GROUP BY dl.device_id, d.device_name, d.serial_number,
                         d.device_type, d.brand, d.model,
                         dl.student_id, s.first_name, s.last_name, s.course_year_level
                ORDER BY entry_count DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(sql, params, deviceFrequencyRowMapper);
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

    private OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class);
    }
}
