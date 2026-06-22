package com.pup.byod.javabyodbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for gate scan operations.
 * Contains the scan result status, message, and the scanned device details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceScanResponse {

    private String status;      // CHECK_IN_SUCCESS, CHECK_OUT_SUCCESS
    private String message;
    private RequestDevice device;
}
