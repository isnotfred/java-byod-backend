package com.pup.byod.javabyodbackend.model.report;

import java.time.LocalDateTime;

/**
 * One row in the Daily Device Traffic Summary report.
 * Represents a single entry or exit event for a device on a given day.
 */
public class DailyTrafficRow {

    // Log event fields
    private int            logId;
    private String         eventType;          // "entry" | "exit"
    private LocalDateTime eventTime;
    private boolean        autoExit;
    private String         logoutType;         // "manual" | "automatic" | null
    private String         notes;
    private LocalDateTime ingressTime;
    private LocalDateTime egressTime;

    // Device fields
    private int    deviceId;
    private String deviceName;
    private String serialNumber;
    private String deviceType;                 // category value
    private String registrationStatus;

    // Student fields
    private String studentId;
    private String studentName;
    private String courseYearLevel;

    // Guard fields
    private String handledByName;              // null for auto-exit rows

    // ── Constructors ──────────────────────────────────────────────────────────

    public DailyTrafficRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int            getLogId()               { return logId; }
    public void           setLogId(int v)           { this.logId = v; }

    public String         getEventType()            { return eventType; }
    public void           setEventType(String v)    { this.eventType = v; }

    public LocalDateTime getEventTime()            { return eventTime; }
    public void           setEventTime(LocalDateTime v) { this.eventTime = v; }

    public boolean        isAutoExit()              { return autoExit; }
    public void           setAutoExit(boolean v)    { this.autoExit = v; }

    public String         getLogoutType()           { return logoutType; }
    public void           setLogoutType(String v)   { this.logoutType = v; }

    public String         getNotes()                { return notes; }
    public void           setNotes(String v)        { this.notes = v; }

    public int            getDeviceId()             { return deviceId; }
    public void           setDeviceId(int v)        { this.deviceId = v; }

    public String         getDeviceName()           { return deviceName; }
    public void           setDeviceName(String v)   { this.deviceName = v; }

    public String         getSerialNumber()         { return serialNumber; }
    public void           setSerialNumber(String v) { this.serialNumber = v; }

    public String         getDeviceType()           { return deviceType; }
    public void           setDeviceType(String v)   { this.deviceType = v; }

    public String         getRegistrationStatus()          { return registrationStatus; }
    public void           setRegistrationStatus(String v)  { this.registrationStatus = v; }

    public String         getStudentId()            { return studentId; }
    public void           setStudentId(String v)    { this.studentId = v; }

    public String         getStudentName()          { return studentName; }
    public void           setStudentName(String v)  { this.studentName = v; }

    public String         getCourseYearLevel()           { return courseYearLevel; }
    public void           setCourseYearLevel(String v)   { this.courseYearLevel = v; }

    public String         getHandledByName()        { return handledByName; }
    public void           setHandledByName(String v){ this.handledByName = v; }

    public LocalDateTime getIngressTime()            { return ingressTime; }
    public void           setIngressTime(LocalDateTime v) { this.ingressTime = v; }

    public LocalDateTime getEgressTime()             { return egressTime; }
    public void           setEgressTime(LocalDateTime v)  { this.egressTime = v; }
}
