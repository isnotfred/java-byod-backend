package com.pup.byod.javabyodbackend.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private String studentId;
    private String firstName;
    private String lastName;
    private String courseYearLevel;
    private String contactNumber;
    private String course;
    private Integer yearLevel;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCourse() {
        return course != null ? course : courseYearLevel;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }
}
