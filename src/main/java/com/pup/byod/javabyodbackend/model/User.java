package com.pup.byod.javabyodbackend.model;

import com.pup.byod.javabyodbackend.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Maps the users table.
 * passwordHash is excluded from responses via Jackson's non_null / @JsonIgnore
 * — configure per-endpoint as needed in the Controller or a DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Integer userId;
    private String username;
    private String email;

    // Never send this in a response. Strip at the Controller / DTO layer.
    private String passwordHash;

    private String fullName;
    private Role role;
    private String status;          // active | inactive
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}