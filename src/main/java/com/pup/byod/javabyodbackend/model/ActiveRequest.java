package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Maps the v_active_requests view.
 * Represents an active approved request with device count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRequest {

    private Integer requestId;
    private String requestType;
    private String studentId;
    private String studentName;
    private String eventName;
    private String venue;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime expectedIngressTime;
    private LocalTime expectedEgressTime;
    private String status;
    private Integer deviceCount;
}
