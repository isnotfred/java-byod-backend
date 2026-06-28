package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.report.*;
import com.pup.byod.javabyodbackend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportController
 *
 * Exposes reporting endpoints backed by the new request-based system.
 * Queries device_transactions instead of old device_logs.
 *
 * Base path: /api/reports
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ── 1. Daily Traffic ────────────────────────────────────────────

    @GetMapping("/daily-traffic")
    public ResponseEntity<List<DailyTrafficRow>> getDailyTraffic(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String deviceType) {
        return ResponseEntity.ok(reportService.getDailyTrafficReport(date, studentId, deviceType));
    }

    // ── 2. Monthly Traffic ──────────────────────────────────────────

    @GetMapping("/monthly-traffic")
    public ResponseEntity<List<MonthlyTrafficRow>> getMonthlyTraffic(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(reportService.getMonthlyTrafficReport(year, month));
    }

    // ── 4. Active Devices on Campus ─────────────────────────────────

    @GetMapping("/active-devices")
    public ResponseEntity<List<ActiveDeviceRow>> getActiveDevicesOnCampus() {
        return ResponseEntity.ok(reportService.getActiveDevicesOnCampus());
    }

    // ── 5. Device Frequency ─────────────────────────────────────────

    @GetMapping("/device-frequency")
    public ResponseEntity<List<DeviceFrequencyRow>> getDeviceFrequency(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getDeviceFrequencyReport(from, to));
    }

    // ── 6. Incident / Override ──────────────────────────────────────

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentOverrideRow>> getIncidentOverrides(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getIncidentOverrideReport(from, to));
    }

    // ── 7. Missed Checkouts ─────────────────────────────────────────

    @GetMapping("/missed-checkouts")
    public ResponseEntity<List<MissedCheckoutRow>> getMissedCheckouts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getMissedCheckoutReport(from, to));
    }

    // ── Late Check-ins & Check-outs ──────────────────────────────────

    @GetMapping("/late-scans")
    public ResponseEntity<List<DailyTrafficRow>> getLateScansReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getLateScansReport(from, to));
    }

    // ── 8. Purpose Breakdown ────────────────────────────────────────

    @GetMapping("/purpose-breakdown")
    public ResponseEntity<List<PurposeBreakdownRow>> getPurposeBreakdown() {
        return ResponseEntity.ok(reportService.getPurposeBreakdownReport());
    }
}
