package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps the device_transactions table.
 * Daily ingress/egress transactions per device — max 1 transaction per device per day.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTransaction {

    private Integer transactionId;
    private Integer requestDeviceId;
    private LocalDate logDate;

    private LocalDateTime ingressTime;
    private Integer ingressHandledBy;

    private LocalDateTime egressTime;
    private Integer egressHandledBy;

    private boolean noEgressMarked;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
