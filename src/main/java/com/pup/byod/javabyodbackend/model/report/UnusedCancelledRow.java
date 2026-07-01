package com.pup.byod.javabyodbackend.model.report;

import java.time.LocalDate;

/**
 * Representing a row in the Unused & Cancelled Requests Report.
 */
public class UnusedCancelledRow {
    private int requestId;
    private String requestType;
    private String studentId;
    private String studentName;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // "cancelled" | "expired"
    private String remarks;

    public UnusedCancelledRow() {}

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
