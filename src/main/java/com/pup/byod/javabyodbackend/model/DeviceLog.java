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
public class DeviceLog {
    private Integer logId;
    private Integer deviceId;
    private String studentId;
    private String eventType;
    private LocalDateTime eventTime;
    private Integer handledBy;
    private String logoutType;
    private Boolean autoExit;
    private String notes;
    private LocalDateTime createdAt;
}
