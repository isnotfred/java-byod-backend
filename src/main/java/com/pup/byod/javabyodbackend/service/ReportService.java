package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.AuditLogDAO;
import com.pup.byod.javabyodbackend.dao.DeviceDAO;
import com.pup.byod.javabyodbackend.dao.DeviceLogDAO;
import com.pup.byod.javabyodbackend.model.report.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportService
 *
 * Produces all six report types required by the BYOD business analysis.
 * Each method delegates its query to the appropriate DAO — no SQL lives here.
 *
 * Report inventory:
 *   1. getDailyTrafficReport      — entry/exit summary for a single day
 *   2. getMonthlyTrafficReport    — aggregated monthly entry/exit by category & student
 *   3. getPendingRegistrationReport — all devices in 'pending' status
 *   4. getActiveDevicesOnCampus   — real-time snapshot of devices currently inside
 *   5. getDeviceFrequencyReport   — historic bring-in frequency per device/student
 *   6. getIncidentOverrideReport  — admin overrides, rejections, dispute resolutions
 */
@Service
public class ReportService {

    private final DeviceLogDAO deviceLogDAO;
    private final DeviceDAO    deviceDAO;
    private final AuditLogDAO  auditLogDAO;

    public ReportService(DeviceLogDAO deviceLogDAO,
                         DeviceDAO    deviceDAO,
                         AuditLogDAO  auditLogDAO) {
        this.deviceLogDAO = deviceLogDAO;
        this.deviceDAO    = deviceDAO;
        this.auditLogDAO  = auditLogDAO;
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 1. Daily Device Traffic Summary
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a summarised view of all device entry and exit events for the
     * given day. Results can be filtered by student ID, device category, or
     * registration status — pass null to skip a filter.
     *
     * Maps to: DeviceLogDAO.getDailyTraffic(date, studentId, deviceType, status)
     *
     * SQL (add to DeviceLogDAO):
     *
     *   SELECT
     *       dl.log_id,
     *       dl.event_type,
     *       dl.event_time,
     *       dl.auto_exit,
     *       dl.logout_type,
     *       dl.notes,
     *       dl.device_id,
     *       d.device_name,
     *       d.serial_number,
     *       d.device_type,
     *       d.registration_status,
     *       dl.student_id,
     *       s.first_name || ' ' || s.last_name AS student_name,
     *       s.course_year_level,
     *       u.full_name AS handled_by_name
     *   FROM   device_logs dl
     *   JOIN   devices  d ON d.device_id  = dl.device_id
     *   JOIN   students s ON s.student_id = dl.student_id
     *   LEFT   JOIN users u ON u.user_id  = dl.handled_by
     *   WHERE  dl.event_time::date = :date
     *     AND  (:studentId   IS NULL OR dl.student_id       = :studentId)
     *     AND  (:deviceType  IS NULL OR d.device_type       = :deviceType)
     *     AND  (:status      IS NULL OR d.registration_status = :status)
     *   ORDER  BY dl.event_time DESC;
     *
     * @param date       The calendar day to report on.
     * @param studentId  Optional filter — exact student_id match.
     * @param deviceType Optional filter — category string (e.g. "Personal Computers").
     * @param status     Optional filter — registration_status (e.g. "approved").
     */
    public List<DailyTrafficRow> getDailyTrafficReport(LocalDate date,
                                                       String    studentId,
                                                       String    deviceType,
                                                       String    status) {
        return deviceLogDAO.getDailyTraffic(date, studentId, deviceType, status);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 2. Monthly Device Traffic Summary
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns aggregated entry and exit counts grouped by month, device
     * category, and student for the specified calendar month.
     *
     * Maps to: DeviceLogDAO.getMonthlyTraffic(year, month)
     *
     * SQL (add to DeviceLogDAO):
     *
     *   SELECT
     *       DATE_TRUNC('month', dl.event_time)::date        AS report_month,
     *       d.device_type                                    AS device_category,
     *       dl.student_id,
     *       s.first_name || ' ' || s.last_name              AS student_name,
     *       s.course_year_level,
     *       COUNT(*) FILTER (WHERE dl.event_type = 'entry') AS entry_count,
     *       COUNT(*) FILTER (WHERE dl.event_type = 'exit')  AS exit_count,
     *       COUNT(*)                                         AS total_events
     *   FROM   device_logs dl
     *   JOIN   devices  d ON d.device_id  = dl.device_id
     *   JOIN   students s ON s.student_id = dl.student_id
     *   WHERE  EXTRACT(YEAR  FROM dl.event_time) = :year
     *     AND  EXTRACT(MONTH FROM dl.event_time) = :month
     *   GROUP  BY report_month, d.device_type,
     *             dl.student_id, s.first_name, s.last_name, s.course_year_level
     *   ORDER  BY d.device_type, student_name;
     *
     * @param year  The 4-digit year (e.g. 2025).
     * @param month The month number 1–12.
     */
    public List<MonthlyTrafficRow> getMonthlyTrafficReport(int year, int month) {
        return deviceLogDAO.getMonthlyTraffic(year, month);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 3. Pending Registration Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lists all devices currently in 'pending' registration status, including
     * the submission date and the guard who submitted the registration.
     *
     * Leverages the existing v_pending_devices view and joins audit_logs to
     * surface the submitting guard (the user who logged DEVICE_REGISTERED).
     *
     * Maps to: DeviceDAO.getPendingRegistrations()
     *
     * SQL (add to DeviceDAO):
     *
     *   SELECT
     *       vp.device_id,
     *       vp.student_id,
     *       vp.student_name,
     *       vp.course_year_level,
     *       vp.device_name,
     *       vp.brand,
     *       vp.model,
     *       vp.serial_number,
     *       vp.device_type,
     *       vp.device_purpose,
     *       vp.image_path,
     *       vp.created_at                AS submitted_at,
     *       u.full_name                  AS submitted_by
     *   FROM  v_pending_devices vp
     *   LEFT  JOIN audit_logs al
     *         ON  al.target_table = 'devices'
     *         AND al.target_id    = vp.device_id::text
     *         AND al.action_type  = 'DEVICE_REGISTERED'
     *   LEFT  JOIN users u ON u.user_id = al.user_id
     *   ORDER BY vp.created_at;
     *
     * Note: if a device has multiple DEVICE_REGISTERED audit rows (re-submissions),
     * the JOIN will return duplicates — add DISTINCT ON (vp.device_id) or use a
     * lateral subquery to take the earliest row only.
     */
    public List<PendingRegistrationRow> getPendingRegistrationReport() {
        return deviceDAO.getPendingRegistrations();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 4. Active Devices on Campus
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Real-time snapshot of all devices currently logged as inside campus.
     * Derived from v_device_campus_status — no date parameter needed.
     *
     * Maps to: DeviceDAO.getActiveDevicesOnCampus()
     *
     * SQL (add to DeviceDAO):
     *
     *   SELECT
     *       vcs.device_id,
     *       vcs.student_id,
     *       s.first_name || ' ' || s.last_name AS student_name,
     *       s.course_year_level,
     *       vcs.device_name,
     *       vcs.serial_number,
     *       d.device_type,
     *       d.brand,
     *       d.model,
     *       vcs.last_event_time              AS entered_at
     *   FROM   v_device_campus_status vcs
     *   JOIN   devices  d ON d.device_id  = vcs.device_id
     *   JOIN   students s ON s.student_id = vcs.student_id
     *   WHERE  vcs.campus_status = 'inside'
     *   ORDER  BY vcs.last_event_time DESC;
     */
    public List<ActiveDeviceRow> getActiveDevicesOnCampus() {
        return deviceDAO.getActiveDevicesOnCampus();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 5. Device Frequency Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Historic data showing how frequently each device is brought onto campus
     * over a date range. Supports future resource planning.
     *
     * Maps to: DeviceLogDAO.getDeviceFrequency(from, to)
     *
     * SQL (add to DeviceLogDAO):
     *
     *   SELECT
     *       dl.device_id,
     *       d.device_name,
     *       d.serial_number,
     *       d.device_type,
     *       d.brand,
     *       d.model,
     *       dl.student_id,
     *       s.first_name || ' ' || s.last_name              AS student_name,
     *       s.course_year_level,
     *       COUNT(*) FILTER (WHERE dl.event_type = 'entry') AS entry_count,
     *       COUNT(*) FILTER (WHERE dl.event_type = 'exit')  AS exit_count,
     *       MIN(dl.event_time)                              AS first_seen,
     *       MAX(dl.event_time)                              AS last_seen
     *   FROM   device_logs dl
     *   JOIN   devices  d ON d.device_id  = dl.device_id
     *   JOIN   students s ON s.student_id = dl.student_id
     *   WHERE  dl.event_time >= :from
     *     AND  dl.event_time <  :to::date + INTERVAL '1 day'
     *   GROUP  BY dl.device_id, d.device_name, d.serial_number,
     *             d.device_type, d.brand, d.model,
     *             dl.student_id, s.first_name, s.last_name, s.course_year_level
     *   ORDER  BY entry_count DESC;
     *
     * @param from Start of date range (inclusive).
     * @param to   End of date range (inclusive).
     */
    public List<DeviceFrequencyRow> getDeviceFrequencyReport(LocalDate from,
                                                             LocalDate to) {
        validateDateRange(from, to);
        return deviceLogDAO.getDeviceFrequency(from, to);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 6. Incident / Override Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Summary of admin overrides, dispute resolutions, and rejected
     * registrations for audit purposes, over a given date range.
     *
     * Maps to: AuditLogDAO.getIncidentOverrides(from, to)
     *
     * SQL (add to AuditLogDAO):
     *
     *   SELECT
     *       al.audit_id,
     *       al.action_type,
     *       al.target_table,
     *       al.target_id,
     *       al.old_values,
     *       al.new_values,
     *       al.ip_address,
     *       al.created_at,
     *       u.full_name  AS performed_by,
     *       u.role       AS performer_role
     *   FROM   audit_logs al
     *   LEFT   JOIN users u ON u.user_id = al.user_id
     *   WHERE  al.action_type IN (
     *              'DEVICE_REJECTED',
     *              'DEVICE_DEACTIVATED',
     *              'ADMIN_DEACTIVATED',
     *              'GUARD_DEACTIVATED_BY_SUPER',
     *              'USER_ROLE_CHANGED',
     *              'DEVICE_AUTO_EXIT',
     *              'EVENT_REQUEST_REJECTED',
     *              'EVENT_REQUEST_RETURNED'
     *          )
     *     AND  al.created_at >= :from
     *     AND  al.created_at <  :to::date + INTERVAL '1 day'
     *   ORDER  BY al.created_at DESC;
     *
     * @param from Start of date range (inclusive).
     * @param to   End of date range (inclusive).
     */
    public List<IncidentOverrideRow> getIncidentOverrideReport(LocalDate from,
                                                               LocalDate to) {
        validateDateRange(from, to);
        return auditLogDAO.getIncidentOverrides(from, to);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Date range must not be null.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                "'from' date must be on or before 'to' date. Got: " + from + " → " + to);
        }
    }
}