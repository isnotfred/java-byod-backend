package com.pup.byod.javabyodbackend.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Report DTO: lists device transaction logs flagged with missed egress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissedCheckoutRow {
    private Integer transactionId;
    private String studentId;
    private String studentName;
    private String deviceName;
    private String serialNumber;
    private LocalDate logDate;
    private LocalDateTime ingressTime;
    private boolean noEgressMarked;
    private String notes;
}
