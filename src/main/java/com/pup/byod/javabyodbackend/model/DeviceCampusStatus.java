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
public class DeviceCampusStatus {
    private Integer deviceId;
    private String studentId;
    private String deviceName;
    private String serialNumber;
    private String brand;
    private String model;
    private DeviceType deviceType;
    private String campusStatus;
    private LocalDateTime lastEventTime;
}
