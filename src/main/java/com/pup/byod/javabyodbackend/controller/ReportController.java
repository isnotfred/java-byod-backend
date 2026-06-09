package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.report.*;
import com.pup.byod.javabyodbackend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportController
 *
 * Exposes all six report types as GET endpoints under /reports.
 * All endpoints are restricted to ADMIN and SUPER_ADMIN roles.
 * Guards do not have access to reports.
 *
 * Base path: /reports
 *
 * Endpoints:
 *   GET /reports/daily-traffic
 *   GET /reports/monthly-traffic
 *   GET /reports/pending-registrations
 *   GET /reports/active-devices
 *   GET /reports/device-frequency
 *   GET /reports/incidents
 */
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 1. Daily Device Traffic Summary
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/daily-traffic?date=2025-06-01
     *                           &studentId=2021-12345   (optional)
     *                           &deviceType=Personal+Computers  (optional)
     *                           &status=approved        (optional)
     *
     * Returns all entry/exit events for the given day.
     * All filter params are optional — omit to return unfiltered results.
     *
     * @param date        The calendar day (ISO format: yyyy-MM-dd). Defaults to today.
     * @param studentId   Optional — filter to a specific student.
     * @param deviceType  Optional — filter by device category.
     * @param status      Optional — filter by registration_status.
     */
    @GetMapping("/daily-traffic")
    public ResponseEntity<List<DailyTrafficRow>> getDailyTraffic(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status) {

        LocalDate reportDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(
            reportService.getDailyTrafficReport(reportDate, studentId, deviceType, status)
        );
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 2. Monthly Device Traffic Summary
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/monthly-traffic?year=2025&month=6
     *
     * Returns aggregated entry/exit counts grouped by device category and
     * student for the given calendar month.
     *
     * @param year  4-digit year. Defaults to current year.
     * @param month Month number 1–12. Defaults to current month.
     */
    @GetMapping("/monthly-traffic")
    public ResponseEntity<List<MonthlyTrafficRow>> getMonthlyTraffic(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        int reportYear  = (year  != null) ? year  : LocalDate.now().getYear();
        int reportMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(
            reportService.getMonthlyTrafficReport(reportYear, reportMonth)
        );
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 3. Pending Registration Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/pending-registrations
     *
     * Returns all devices currently in 'pending' registration status,
     * with submission date and the guard who submitted the registration.
     * No parameters — always reflects the live pending queue.
     */
    @GetMapping("/pending-registrations")
    public ResponseEntity<List<PendingRegistrationRow>> getPendingRegistrations() {
        return ResponseEntity.ok(reportService.getPendingRegistrationReport());
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 4. Active Devices on Campus
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/active-devices
     *
     * Real-time snapshot of all devices currently logged as inside campus.
     * No parameters — reflects live state derived from device_logs.
     */
    @GetMapping("/active-devices")
    public ResponseEntity<List<ActiveDeviceRow>> getActiveDevices() {
        return ResponseEntity.ok(reportService.getActiveDevicesOnCampus());
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 5. Device Frequency Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/device-frequency?from=2025-01-01&to=2025-06-30
     *
     * Returns historic bring-in frequency per device/student over the
     * given date range, ordered by entry count descending.
     *
     * @param from Start of date range (inclusive, ISO format: yyyy-MM-dd). Required.
     * @param to   End of date range (inclusive, ISO format: yyyy-MM-dd). Required.
     */
    @GetMapping("/device-frequency")
    public ResponseEntity<List<DeviceFrequencyRow>> getDeviceFrequency(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return ResponseEntity.ok(reportService.getDeviceFrequencyReport(from, to));
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 6. Incident / Override Report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /reports/incidents?from=2025-01-01&to=2025-06-30
     *
     * Returns admin overrides, dispute resolutions, and rejected registrations
     * from audit_logs over the given date range.
     *
     * @param from Start of date range (inclusive, ISO format: yyyy-MM-dd). Required.
     * @param to   End of date range (inclusive, ISO format: yyyy-MM-dd). Required.
     */
    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentOverrideRow>> getIncidents(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return ResponseEntity.ok(reportService.getIncidentOverrideReport(from, to));
    }
}
