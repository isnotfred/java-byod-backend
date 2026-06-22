package com.pup.byod.javabyodbackend.model;

import com.pup.byod.javabyodbackend.model.enums.DeviceVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Maps the request_devices table.
 * Individual devices listed under a request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDevice {

    private Integer requestDeviceId;
    private Integer requestId;
    private String deviceName;
    private String brand;
    private String model;
    private String deviceType;
    private String serialNumber;
    private int quantity;
    private String imagePath;

    private DeviceVerificationStatus deviceStatus;
    private Integer verifiedBy;
    private LocalDateTime verifiedAt;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
