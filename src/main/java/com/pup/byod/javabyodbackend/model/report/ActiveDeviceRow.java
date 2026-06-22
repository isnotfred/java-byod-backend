package com.pup.byod.javabyodbackend.model.report;

import java.time.LocalDateTime;

/**
 * One row in the Active Devices on Campus report.
 * Represents a device currently logged as inside campus (campus_status = 'inside').
 */
public class ActiveDeviceRow {

    private int            deviceId;
    private String         studentId;
    private String         studentName;
    private String         courseYearLevel;
    private String         deviceName;
    private String         serialNumber;
    private String         deviceType;        // category value
    private String         brand;
    private String         model;
    private LocalDateTime enteredAt;         // timestamp of the latest ingress

    // ── Constructors ──────────────────────────────────────────────────────────

    public ActiveDeviceRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int            getDeviceId()             { return deviceId; }
    public void           setDeviceId(int v)         { this.deviceId = v; }

    public String         getStudentId()             { return studentId; }
    public void           setStudentId(String v)     { this.studentId = v; }

    public String         getStudentName()           { return studentName; }
    public void           setStudentName(String v)   { this.studentName = v; }

    public String         getCourseYearLevel()           { return courseYearLevel; }
    public void           setCourseYearLevel(String v)   { this.courseYearLevel = v; }

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

    public LocalDateTime getEnteredAt()             { return enteredAt; }
    public void           setEnteredAt(LocalDateTime v) { this.enteredAt = v; }
}
