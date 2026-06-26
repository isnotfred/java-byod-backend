package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.DeviceTransaction;
import com.pup.byod.javabyodbackend.model.report.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DeviceTransactionDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public DeviceTransactionDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<DeviceTransaction> transactionRowMapper = (rs, rowNum) -> DeviceTransaction.builder()
            .transactionId(rs.getInt("transaction_id"))
            .requestDeviceId(rs.getInt("request_device_id"))
            .logDate(rs.getDate("log_date") != null ? rs.getDate("log_date").toLocalDate() : null)
            .ingressTime(rs.getTimestamp("ingress_time") != null ? rs.getTimestamp("ingress_time").toLocalDateTime() : null)
            .ingressHandledBy(rs.getObject("ingress_handled_by") != null ? rs.getInt("ingress_handled_by") : null)
            .egressTime(rs.getTimestamp("egress_time") != null ? rs.getTimestamp("egress_time").toLocalDateTime() : null)
            .egressHandledBy(rs.getObject("egress_handled_by") != null ? rs.getInt("egress_handled_by") : null)
            .noEgressMarked(rs.getBoolean("no_egress_marked"))
            .notes(rs.getString("notes"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    // ── Queries ──────────────────────────────────────────────────────

    /**
     * Find today's transaction for a given request device.
     * Returns Optional.empty() if no transaction exists for today.
     */
    public Optional<DeviceTransaction> findTodayTransaction(int requestDeviceId) {
        String sql = """
                SELECT * FROM device_transactions
                WHERE request_device_id = :requestDeviceId
                  AND log_date = CURRENT_DATE
                """;
        var params = new MapSqlParameterSource("requestDeviceId", requestDeviceId);
        return jdbc.query(sql, params, transactionRowMapper).stream().findFirst();
    }

    public Optional<DeviceTransaction> findOpenTransaction(int requestDeviceId) {
        String sql = """
                SELECT * FROM device_transactions
                WHERE request_device_id = :requestDeviceId
                  AND egress_time IS NULL
                  AND no_egress_marked = FALSE
                ORDER BY log_date ASC
                LIMIT 1
                """;
        var params = new MapSqlParameterSource("requestDeviceId", requestDeviceId);
        return jdbc.query(sql, params, transactionRowMapper).stream().findFirst();
    }

    /**
     * Find transactions for a specific device within a date range.
     */
    public List<DeviceTransaction> findByRequestDeviceId(int requestDeviceId) {
        String sql = """
                SELECT * FROM device_transactions
                WHERE request_device_id = :requestDeviceId
                ORDER BY log_date DESC
                """;
        var params = new MapSqlParameterSource("requestDeviceId", requestDeviceId);
        return jdbc.query(sql, params, transactionRowMapper);
    }

    /**
     * Find all transactions for a given date.
     */
    public List<DeviceTransaction> findByDate(LocalDate date) {
        String sql = "SELECT * FROM device_transactions WHERE log_date = :date ORDER BY ingress_time DESC";
        var params = new MapSqlParameterSource("date", date);
        return jdbc.query(sql, params, transactionRowMapper);
    }

    public Optional<DeviceTransaction> findById(int transactionId) {
        String sql = "SELECT * FROM device_transactions WHERE transaction_id = :transactionId";
        var params = new MapSqlParameterSource("transactionId", transactionId);
        return jdbc.query(sql, params, transactionRowMapper).stream().findFirst();
    }

    /**
     * Find all open transactions (checked in but not checked out) that are NOT today.
     * These are candidates for being marked as "missed egress".
     */
    public List<DeviceTransaction> findUnclosedTransactionsBeforeToday() {
        String sql = """
                SELECT * FROM device_transactions
                WHERE egress_time IS NULL
                  AND no_egress_marked = FALSE
                  AND log_date < CURRENT_DATE
                ORDER BY log_date ASC
                """;
        return jdbc.query(sql, transactionRowMapper);
    }

    /**
     * Mark unclosed past transactions as missed egress (Requirement 7).
     * Returns the count of updated rows.
     */
    public int markUnclosedTransactionsAsMissed() {
        String sql = """
                UPDATE device_transactions dt
                SET no_egress_marked = TRUE,
                    notes = COALESCE(notes, '') || ' [auto: missed egress]'
                WHERE dt.egress_time IS NULL
                  AND dt.no_egress_marked = FALSE
                  AND dt.log_date < CURRENT_DATE
                  AND dt.request_device_id NOT IN (
                      SELECT rd.request_device_id
                      FROM request_devices rd
                      JOIN requests r ON r.request_id = rd.request_id
                      WHERE r.request_type = 'event'
                        AND r.end_date >= CURRENT_DATE
                  )
                """;
        return jdbc.update(sql, new MapSqlParameterSource());
    }

    /**
     * Close an unclosed transaction from a past day as a missed checkout, logging the actual egress time.
     */
    public void closeMissedTransaction(int transactionId, int handledBy, String notes) {
        String sql = """
                UPDATE device_transactions
                SET egress_time = CURRENT_TIMESTAMP,
                    egress_handled_by = :handledBy,
                    no_egress_marked = TRUE,
                    notes = COALESCE(notes, '') || ' ' || :notes
                WHERE transaction_id = :transactionId
                """;
        var params = new MapSqlParameterSource()
                .addValue("transactionId", transactionId)
                .addValue("handledBy", handledBy)
                .addValue("notes", notes);
        jdbc.update(sql, params);
    }

    // ── Report Queries ──────────────────────────────────────────────

    /**
     * Daily traffic report: all transactions for a given date with device + student details.
     */
    public List<DailyTrafficRow> getDailyTraffic(LocalDate date, String studentId, String deviceType) {
        String sql = """
                SELECT
                    dt.transaction_id,
                    dt.log_date,
                    dt.ingress_time,
                    dt.egress_time,
                    dt.no_egress_marked,
                    dt.notes,
                    rd.request_device_id,
                    rd.device_name,
                    rd.serial_number,
                    rd.device_type,
                    rd.device_status AS registration_status,
                    r.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    ui.full_name AS ingress_handled_by_name,
                    ue.full_name AS egress_handled_by_name
                FROM   device_transactions dt
                JOIN   request_devices rd ON rd.request_device_id = dt.request_device_id
                JOIN   requests r ON r.request_id = rd.request_id
                JOIN   students s ON s.student_id = r.student_id
                LEFT   JOIN users ui ON ui.user_id = dt.ingress_handled_by
                LEFT   JOIN users ue ON ue.user_id = dt.egress_handled_by
                WHERE  dt.log_date = :date
                  AND  (CAST(:studentId AS VARCHAR) IS NULL OR r.student_id = :studentId)
                  AND  (CAST(:deviceType AS VARCHAR) IS NULL OR rd.device_type = :deviceType)
                ORDER  BY dt.ingress_time DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("date", date)
                .addValue("studentId", studentId)
                .addValue("deviceType", deviceType);
        return jdbc.query(sql, params, dailyTrafficRowMapper);
    }

    /**
     * Monthly traffic report: aggregated entry/exit counts per month.
     */
    public List<MonthlyTrafficRow> getMonthlyTraffic(int year, int month) {
        String sql = """
                SELECT
                    DATE_TRUNC('month', dt.log_date)::date  AS report_month,
                    rd.device_type                           AS device_category,
                    r.student_id,
                    s.first_name || ' ' || s.last_name       AS student_name,
                    s.course_year_level,
                    COUNT(*)                                  AS total_events,
                    COUNT(*) FILTER (WHERE dt.egress_time IS NOT NULL) AS exit_count,
                    COUNT(*) FILTER (WHERE dt.no_egress_marked = TRUE) AS missed_count
                FROM   device_transactions dt
                JOIN   request_devices rd ON rd.request_device_id = dt.request_device_id
                JOIN   requests r ON r.request_id = rd.request_id
                JOIN   students s ON s.student_id = r.student_id
                WHERE  EXTRACT(YEAR  FROM dt.log_date) = :year
                  AND  EXTRACT(MONTH FROM dt.log_date) = :month
                GROUP  BY report_month, rd.device_type,
                          r.student_id, s.first_name, s.last_name, s.course_year_level
                ORDER  BY rd.device_type, student_name
                """;
        var params = new MapSqlParameterSource()
                .addValue("year", year)
                .addValue("month", month);
        return jdbc.query(sql, params, monthlyTrafficRowMapper);
    }

    /**
     * Device frequency report: how often each device is brought on campus.
     */
    public List<DeviceFrequencyRow> getDeviceFrequency(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    rd.request_device_id,
                    rd.device_name,
                    rd.serial_number,
                    rd.device_type,
                    rd.brand,
                    rd.model,
                    r.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    COUNT(*) AS entry_count,
                    COUNT(*) FILTER (WHERE dt.egress_time IS NOT NULL) AS exit_count,
                    MIN(dt.ingress_time) AS first_seen,
                    MAX(COALESCE(dt.egress_time, dt.ingress_time)) AS last_seen
                FROM   device_transactions dt
                JOIN   request_devices rd ON rd.request_device_id = dt.request_device_id
                JOIN   requests r ON r.request_id = rd.request_id
                JOIN   students s ON s.student_id = r.student_id
                WHERE  dt.log_date >= :from
                  AND  dt.log_date <= :to
                GROUP  BY rd.request_device_id, rd.device_name, rd.serial_number,
                          rd.device_type, rd.brand, rd.model,
                          r.student_id, s.first_name, s.last_name, s.course_year_level
                ORDER  BY entry_count DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(sql, params, deviceFrequencyRowMapper);
    }

    /**
     * Missed checkout report: transactions without egress that were marked.
     */
    public List<MissedCheckoutRow> getMissedCheckouts(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    dt.transaction_id,
                    r.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    rd.device_name,
                    rd.serial_number,
                    dt.log_date,
                    dt.ingress_time,
                    dt.no_egress_marked,
                    dt.notes
                FROM   device_transactions dt
                JOIN   request_devices rd ON rd.request_device_id = dt.request_device_id
                JOIN   requests r ON r.request_id = rd.request_id
                JOIN   students s ON s.student_id = r.student_id
                WHERE  dt.no_egress_marked = TRUE
                  AND  dt.log_date >= :from
                  AND  dt.log_date <= :to
                ORDER  BY dt.log_date DESC, dt.ingress_time DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(sql, params, missedCheckoutRowMapper);
    }

    /**
     * Active devices on campus: currently checked in and not checked out.
     */
    public List<ActiveDeviceRow> getActiveDevicesOnCampus() {
        String sql = """
                SELECT
                    vcs.request_device_id,
                    vcs.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    s.course_year_level,
                    vcs.device_name,
                    vcs.serial_number,
                    vcs.device_type,
                    vcs.brand,
                    vcs.model,
                    vcs.last_event_time AS entered_at
                FROM   v_device_campus_status vcs
                JOIN   students s ON s.student_id = vcs.student_id
                WHERE  vcs.campus_status = 'entry'
                ORDER  BY vcs.last_event_time DESC
                """;
        return jdbc.query(sql, activeDeviceRowMapper);
    }

    /**
     * Purpose breakdown: analytics on request purposes.
     */
    public List<PurposeBreakdownRow> getPurposeBreakdown() {
        String sql = """
                SELECT
                    r.purpose,
                    COUNT(DISTINCT r.request_id) AS request_count,
                    COUNT(rd.request_device_id) FILTER (WHERE rd.device_status = 'approved') AS total_devices_approved,
                    ROUND(
                        COUNT(DISTINCT r.request_id) * 100.0 / NULLIF(
                            (SELECT COUNT(*) FROM requests WHERE status = 'approved'), 0
                        ), 2
                    ) AS percentage
                FROM   requests r
                LEFT   JOIN request_devices rd ON rd.request_id = r.request_id
                WHERE  r.status = 'approved'
                GROUP  BY r.purpose
                ORDER  BY request_count DESC
                """;
        return jdbc.query(sql, purposeBreakdownRowMapper);
    }



    // ── Mutations ────────────────────────────────────────────────────

    /**
     * Insert a new ingress transaction. Egress fields are null initially.
     */
    public int insertIngress(int requestDeviceId, int handledBy, String notes) {
        String sql = """
                INSERT INTO device_transactions (
                    request_device_id, log_date, ingress_time, ingress_handled_by, notes
                ) VALUES (
                    :requestDeviceId, CURRENT_DATE, CURRENT_TIMESTAMP, :handledBy, :notes
                )
                """;
        var params = new MapSqlParameterSource()
                .addValue("requestDeviceId", requestDeviceId)
                .addValue("handledBy", handledBy)
                .addValue("notes", notes);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"transaction_id"});
        return keyHolder.getKey().intValue();
    }

    /**
     * Record egress on an existing transaction.
     */
    public int updateEgress(int transactionId, int handledBy, String notes) {
        String sql = """
                UPDATE device_transactions
                SET egress_time       = CURRENT_TIMESTAMP,
                    egress_handled_by = :handledBy,
                    notes             = COALESCE(CAST(:notes AS VARCHAR), notes)
                WHERE transaction_id = :transactionId
                """;
        var params = new MapSqlParameterSource()
                .addValue("handledBy", handledBy)
                .addValue("notes", notes)
                .addValue("transactionId", transactionId);
        return jdbc.update(sql, params);
    }

    // ── Report RowMappers ───────────────────────────────────────────

    private final RowMapper<DailyTrafficRow> dailyTrafficRowMapper = (rs, rowNum) -> {
        var row = new DailyTrafficRow();
        row.setLogId(rs.getInt("transaction_id"));
        row.setEventType(rs.getTimestamp("egress_time") != null ? "exit" : "entry");
        row.setEventTime(rs.getTimestamp("ingress_time") != null
                ? rs.getTimestamp("ingress_time").toLocalDateTime() : null);
        row.setAutoExit(rs.getBoolean("no_egress_marked"));
        row.setLogoutType(rs.getBoolean("no_egress_marked") ? "missed" : "manual");
        row.setNotes(rs.getString("notes"));
        row.setDeviceId(rs.getInt("request_device_id"));
        row.setDeviceName(rs.getString("device_name"));
        row.setSerialNumber(rs.getString("serial_number"));
        row.setDeviceType(rs.getString("device_type"));
        row.setRegistrationStatus(rs.getString("registration_status"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setHandledByName(rs.getString("ingress_handled_by_name"));
        row.setIngressTime(rs.getTimestamp("ingress_time") != null ? rs.getTimestamp("ingress_time").toLocalDateTime() : null);
        row.setEgressTime(rs.getTimestamp("egress_time") != null ? rs.getTimestamp("egress_time").toLocalDateTime() : null);
        return row;
    };

    private final RowMapper<MonthlyTrafficRow> monthlyTrafficRowMapper = (rs, rowNum) -> {
        var row = new MonthlyTrafficRow();
        row.setReportMonth(rs.getDate("report_month") != null ? rs.getDate("report_month").toLocalDate() : null);
        row.setDeviceCategory(rs.getString("device_category"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setEntryCount(rs.getInt("total_events"));
        row.setExitCount(rs.getInt("exit_count"));
        row.setTotalEvents(rs.getInt("total_events"));
        return row;
    };

    private final RowMapper<DeviceFrequencyRow> deviceFrequencyRowMapper = (rs, rowNum) -> {
        var row = new DeviceFrequencyRow();
        row.setDeviceId(rs.getInt("request_device_id"));
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
        row.setFirstSeen(rs.getTimestamp("first_seen") != null ? rs.getTimestamp("first_seen").toLocalDateTime() : null);
        row.setLastSeen(rs.getTimestamp("last_seen") != null ? rs.getTimestamp("last_seen").toLocalDateTime() : null);
        return row;
    };

    private final RowMapper<MissedCheckoutRow> missedCheckoutRowMapper = (rs, rowNum) -> MissedCheckoutRow.builder()
            .transactionId(rs.getInt("transaction_id"))
            .studentId(rs.getString("student_id"))
            .studentName(rs.getString("student_name"))
            .deviceName(rs.getString("device_name"))
            .serialNumber(rs.getString("serial_number"))
            .logDate(rs.getDate("log_date") != null ? rs.getDate("log_date").toLocalDate() : null)
            .ingressTime(rs.getTimestamp("ingress_time") != null ? rs.getTimestamp("ingress_time").toLocalDateTime() : null)
            .noEgressMarked(rs.getBoolean("no_egress_marked"))
            .notes(rs.getString("notes"))
            .build();

    private final RowMapper<ActiveDeviceRow> activeDeviceRowMapper = (rs, rowNum) -> {
        var row = new ActiveDeviceRow();
        row.setDeviceId(rs.getInt("request_device_id"));
        row.setStudentId(rs.getString("student_id"));
        row.setStudentName(rs.getString("student_name"));
        row.setCourseYearLevel(rs.getString("course_year_level"));
        row.setDeviceName(rs.getString("device_name"));
        row.setSerialNumber(rs.getString("serial_number"));
        row.setDeviceType(rs.getString("device_type"));
        row.setBrand(rs.getString("brand"));
        row.setModel(rs.getString("model"));
        row.setEnteredAt(rs.getTimestamp("entered_at") != null ? rs.getTimestamp("entered_at").toLocalDateTime() : null);
        return row;
    };


    private final RowMapper<PurposeBreakdownRow> purposeBreakdownRowMapper = (rs, rowNum) -> PurposeBreakdownRow.builder()
            .purpose(rs.getString("purpose"))
            .requestCount(rs.getInt("request_count"))
            .totalDevicesApproved(rs.getInt("total_devices_approved"))
            .percentage(rs.getDouble("percentage"))
            .build();
}
