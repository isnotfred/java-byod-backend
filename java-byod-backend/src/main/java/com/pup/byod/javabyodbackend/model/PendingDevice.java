package com.pup.byod.javabyodbackend.model;

import java.time.LocalDateTime;

import com.pup.byod.javabyodbackend.model.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDevice {
    private Integer deviceId;
    private String studentId;
    private String studentFullName;
    private String deviceName;
    private DeviceType deviceType;
    private String brand;
    private String model;
    private String serialNumber;
    private String devicePurpose;
    private String imagePath;
    private LocalDateTime createdAt;
}
