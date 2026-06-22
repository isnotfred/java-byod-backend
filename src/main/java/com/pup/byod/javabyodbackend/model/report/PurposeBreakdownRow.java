package com.pup.byod.javabyodbackend.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report DTO: analytics breakdown by request purpose/volume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurposeBreakdownRow {
    private String purpose;
    private int requestCount;
    private int totalDevicesApproved;
    private double percentage;
}
