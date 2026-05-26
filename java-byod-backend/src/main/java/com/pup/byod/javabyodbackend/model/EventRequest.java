package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    private Integer eventRequestId;
    private String studentId;
    private String responsiblePerson;
    private String organization;
    private String eventName;
    private String eventPurpose;
    private String approvalDocType;
    private String approvalDocRef;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean isSubmitted;
    private Boolean isAccommodated;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
