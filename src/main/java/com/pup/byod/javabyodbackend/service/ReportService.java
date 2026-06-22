package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.AuditLogDAO;
import com.pup.byod.javabyodbackend.dao.DeviceTransactionDAO;
import com.pup.byod.javabyodbackend.model.report.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportService
 *
 * Produces all report types required by the BYOD business analysis.
 * Queries are delegated to DeviceTransactionDAO and AuditLogDAO.
 *
 * Report inventory:
 *   1. getDailyTrafficReport        — ingress/egress summary for a single day
 *   2. getMonthlyTrafficReport      — aggregated monthly traffic by category & student
 *   3. getPendingRequestsReport     — all requests in 'pending' status
 *   4. getActiveDevicesOnCampus     — real-time snapshot of devices currently inside
 *   5. getDeviceFrequencyReport     — historic bring-in frequency per device/student
 *   6. getIncidentOverrideReport    — admin overrides, rejections, dispute resolutions
 *   7. getMissedCheckoutReport      — transactions without egress (missed checkouts)
 *   8. getPurposeBreakdownReport    — analytics on request purposes
 */
@Service
public class ReportService {

    private final DeviceTransactionDAO deviceTransactionDAO;
    private final AuditLogDAO auditLogDAO;

    public ReportService(DeviceTransactionDAO deviceTransactionDAO,
                         AuditLogDAO auditLogDAO) {
        this.deviceTransactionDAO = deviceTransactionDAO;
        this.auditLogDAO = auditLogDAO;
    }

    // ── 1. Daily Traffic ────────────────────────────────────────────

    /**
     * Returns a summarised view of all device ingress and egress transactions
     * for the given day. Results can be filtered by student ID or device type.
     */
    public List<DailyTrafficRow> getDailyTrafficReport(LocalDate date,
                                                       String studentId,
                                                       String deviceType) {
        if (date == null) {
            date = LocalDate.now();
        }
        return deviceTransactionDAO.getDailyTraffic(date, studentId, deviceType);
    }

    // ── 2. Monthly Traffic ──────────────────────────────────────────

    /**
     * Returns aggregated ingress/egress counts grouped by month, device
     * category, and student for the specified calendar month.
     */
    public List<MonthlyTrafficRow> getMonthlyTrafficReport(int year, int month) {
        return deviceTransactionDAO.getMonthlyTraffic(year, month);
    }

    // ── 4. Active Devices on Campus ─────────────────────────────────

    /**
     * Real-time snapshot of all devices currently logged as inside campus.
     */
    public List<ActiveDeviceRow> getActiveDevicesOnCampus() {
        return deviceTransactionDAO.getActiveDevicesOnCampus();
    }

    // ── 5. Device Frequency ─────────────────────────────────────────

    /**
     * Historic data showing how frequently each device is brought onto campus
     * over a date range.
     */
    public List<DeviceFrequencyRow> getDeviceFrequencyReport(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return deviceTransactionDAO.getDeviceFrequency(from, to);
    }

    // ── 6. Incident / Override ──────────────────────────────────────

    /**
     * Summary of admin overrides, dispute resolutions, and rejected
     * requests for audit purposes, over a given date range.
     */
    public List<IncidentOverrideRow> getIncidentOverrideReport(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return auditLogDAO.getIncidentOverrides(from, to);
    }

    // ── 7. Missed Checkout ──────────────────────────────────────────

    /**
     * Lists all device transactions that were flagged as missed checkout
     * (no egress recorded).
     */
    public List<MissedCheckoutRow> getMissedCheckoutReport(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return deviceTransactionDAO.getMissedCheckouts(from, to);
    }

    // ── 8. Purpose Breakdown ────────────────────────────────────────

    /**
     * Analytics breakdown by request purpose/volume.
     */
    public List<PurposeBreakdownRow> getPurposeBreakdownReport() {
        return deviceTransactionDAO.getPurposeBreakdown();
    }

    // ── Private Helpers ─────────────────────────────────────────────

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