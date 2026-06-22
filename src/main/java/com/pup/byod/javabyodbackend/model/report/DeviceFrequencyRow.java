package com.pup.byod.javabyodbackend.model.report;

import java.time.LocalDateTime;

/**
 * One row in the Device Frequency Report.
 * Aggregates how often a specific device has been brought on campus
 * within the requested date range.
 */
public class DeviceFrequencyRow {

    private int            deviceId;
    private String         deviceName;
    private String         serialNumber;
    private String         deviceType;        // category value
    private String         brand;
    private String         model;
    private String         studentId;
    private String         studentName;
    private String         courseYearLevel;
    private int            entryCount;
    private int            exitCount;
    private LocalDateTime firstSeen;         // earliest event_time in range
    private LocalDateTime lastSeen;          // latest event_time in range

    // ── Constructors ──────────────────────────────────────────────────────────

    public DeviceFrequencyRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int            getDeviceId()             { return deviceId; }
    public void           setDeviceId(int v)         { this.deviceId = v; }

    public String         getDeviceName()            { return deviceName; }
    public void           setDeviceName(String v)    { this.deviceName = v; }

    public String         getSerialNumber()          { return serialNumber; }
    public void           setSerialNumber(String v)  { this.serialNumber = v; }

    public String         getDeviceType()            { return deviceType; }
    public void           setDeviceType(String v)    { this.deviceType = v; }

    public String         getBrand()                 { return brand; }
    public void           setBrand(String v)         { this.brand = v; }

    public String         getModel()                 { return model; }
    public void           setModel(String v)         { this.model = v; }

    public String         getStudentId()             { return studentId; }
    public void           setStudentId(String v)     { this.studentId = v; }

    public String         getStudentName()           { return studentName; }
    public void           setStudentName(String v)   { this.studentName = v; }

    public String         getCourseYearLevel()           { return courseYearLevel; }
    public void           setCourseYearLevel(String v)   { this.courseYearLevel = v; }

    public int            getEntryCount()            { return entryCount; }
    public void           setEntryCount(int v)       { this.entryCount = v; }

    public int            getExitCount()             { return exitCount; }
    public void           setExitCount(int v)        { this.exitCount = v; }

    public LocalDateTime getFirstSeen()             { return firstSeen; }
    public void           setFirstSeen(LocalDateTime v) { this.firstSeen = v; }

    public LocalDateTime getLastSeen()              { return lastSeen; }
    public void           setLastSeen(LocalDateTime v)  { this.lastSeen = v; }
}
