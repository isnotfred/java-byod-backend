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
public class EventDeviceLog {
    private Integer eventLogId;
    private Integer eventDeviceId;
    private String eventType;
    private LocalDateTime eventTime;
    private Integer handledBy;
    private String notes;
    private LocalDateTime createdAt;
}
