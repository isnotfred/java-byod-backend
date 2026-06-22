package com.pup.byod.javabyodbackend.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the v_device_campus_status view (new request-based schema).
 * Real-time campus presence status derived from the latest daily transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCampusStatus {
    private Integer requestDeviceId;
    private Integer requestId;
    private String studentId;
    private String deviceName;
    private String serialNumber;
    private String brand;
    private String model;
    private String deviceType;
    private String deviceStatus;
    private String requestType;
    private String campusStatus;
    private LocalDateTime lastEventTime;
    private boolean noEgressMarked;
}
