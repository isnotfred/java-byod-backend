package com.pup.byod.javabyodbackend.model.report;

import java.time.LocalDate;

/**
 * One row in the Monthly Device Traffic Summary report.
 * Aggregated entry/exit counts per month, device category, and student.
 */
public class MonthlyTrafficRow {

    private LocalDate reportMonth;       // truncated to first of month
    private String    deviceCategory;    // device_type category value
    private String    studentId;
    private String    studentName;
    private String    courseYearLevel;
    private int       entryCount;
    private int       exitCount;
    private int       totalEvents;

    // ── Constructors ──────────────────────────────────────────────────────────

    public MonthlyTrafficRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public LocalDate getReportMonth()             { return reportMonth; }
    public void      setReportMonth(LocalDate v)  { this.reportMonth = v; }

    public String    getDeviceCategory()          { return deviceCategory; }
    public void      setDeviceCategory(String v)  { this.deviceCategory = v; }

    public String    getStudentId()               { return studentId; }
    public void      setStudentId(String v)       { this.studentId = v; }

    public String    getStudentName()             { return studentName; }
    public void      setStudentName(String v)     { this.studentName = v; }

    public String    getCourseYearLevel()         { return courseYearLevel; }
    public void      setCourseYearLevel(String v) { this.courseYearLevel = v; }

    public int       getEntryCount()              { return entryCount; }
    public void      setEntryCount(int v)         { this.entryCount = v; }

    public int       getExitCount()               { return exitCount; }
    public void      setExitCount(int v)          { this.exitCount = v; }

    public int       getTotalEvents()             { return totalEvents; }
    public void      setTotalEvents(int v)        { this.totalEvents = v; }
}
