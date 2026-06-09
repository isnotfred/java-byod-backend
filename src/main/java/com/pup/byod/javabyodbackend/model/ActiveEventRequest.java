package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveEventRequest {
    private Integer eventRequestId;
    private String studentId;
    private String studentName;
    private String eventName;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer deviceCount;
}
