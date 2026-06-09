package com.pup.byod.javabyodbackend.model.report;

import java.time.OffsetDateTime;

/**
 * One row in the Pending Registration Report.
 * Represents a device awaiting admin approval, with its submission metadata.
 */
public class PendingRegistrationRow {

    private int            deviceId;
    private String         studentId;
    private String         studentName;
    private String         courseYearLevel;
    private String         deviceName;
    private String         brand;
    private String         model;
    private String         serialNumber;
    private String         deviceType;        // category value
    private String         devicePurpose;
    private String         imagePath;
    private OffsetDateTime submittedAt;       // devices.created_at
    private String         submittedBy;       // full_name of the guard who registered it

    // ── Constructors ──────────────────────────────────────────────────────────

    public PendingRegistrationRow() {}

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

    public String         getBrand()                 { return brand; }
    public void           setBrand(String v)         { this.brand = v; }

    public String         getModel()                 { return model; }
    public void           setModel(String v)         { this.model = v; }

    public String         getSerialNumber()          { return serialNumber; }
    public void           setSerialNumber(String v)  { this.serialNumber = v; }

    public String         getDeviceType()            { return deviceType; }
    public void           setDeviceType(String v)    { this.deviceType = v; }

    public String         getDevicePurpose()         { return devicePurpose; }
    public void           setDevicePurpose(String v) { this.devicePurpose = v; }

    public String         getImagePath()             { return imagePath; }
    public void           setImagePath(String v)     { this.imagePath = v; }

    public OffsetDateTime getSubmittedAt()            { return submittedAt; }
    public void           setSubmittedAt(OffsetDateTime v) { this.submittedAt = v; }

    public String         getSubmittedBy()           { return submittedBy; }
    public void           setSubmittedBy(String v)   { this.submittedBy = v; }
}
