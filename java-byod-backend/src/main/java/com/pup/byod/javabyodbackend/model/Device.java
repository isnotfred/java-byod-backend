package com.pup.byod.javabyodbackend.model;

import java.time.LocalDateTime;

import com.pup.byod.javabyodbackend.model.enums.DeviceType;
import com.pup.byod.javabyodbackend.model.enums.RegistrationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    private Integer deviceId;
    private String studentId;
    private String deviceName;
    private String brand;
    private String model;
    private String serialNumber;
    private DeviceType deviceType;
    private String devicePurpose;
    private RegistrationStatus registrationStatus;
    private String deviceStatus;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String remarks;
    private String imagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
