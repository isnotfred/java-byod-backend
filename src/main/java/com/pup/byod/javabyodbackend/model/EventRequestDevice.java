package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDevice {
    private Integer eventDeviceId;
    private Integer eventRequestId;
    private String deviceName;
    private String brand;
    private String model;
    private String deviceType;
    private String serialNumber;
    private Integer quantity;
    private Integer verifiedBy;
    private LocalDateTime verifiedAt;
    private String deviceStatus;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Derived fields
    private String currentDayStatus;
    private LocalDateTime lastEventTime;
}

