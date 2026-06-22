package com.pup.byod.javabyodbackend.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Report DTO: audit discrepancies in check-in/check-out times vs expected schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeDiscrepancyRow {
    private Integer requestId;
    private String studentId;
    private String studentName;
    private String deviceName;
    private String serialNumber;
    private LocalTime expectedTime;
    private LocalDateTime actualTime;
    private long discrepancyMinutes;
    private String type; // 'EARLY_ARRIVAL', 'LATE_DEPARTURE'
}
