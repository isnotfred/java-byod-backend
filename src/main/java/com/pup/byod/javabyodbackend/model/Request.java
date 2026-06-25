package com.pup.byod.javabyodbackend.model;

import com.pup.byod.javabyodbackend.model.enums.RequestStatus;
import com.pup.byod.javabyodbackend.model.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Maps the requests table.
 * Unified model for both individual (normal) and group (event) device requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    private Integer requestId;
    private RequestType requestType;
    private String studentId;

    // Event-specific details (nullable for normal requests)
    private String eventName;
    private String organization;
    private String responsiblePerson;

    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime expectedIngressTime;
    private LocalTime expectedEgressTime;

    private RequestStatus status;
    private boolean isSubmitted;
    private boolean isAccommodated;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
