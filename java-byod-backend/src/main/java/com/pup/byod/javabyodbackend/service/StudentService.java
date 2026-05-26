package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.StudentRepository;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.Student;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    public StudentService(StudentRepository studentRepository, AuditLogService auditLogService) {
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    public List<Student> searchStudents(String keyword) {
        ValidationUtil.requireNonBlank(keyword, "Keyword");
        return studentRepository.search(keyword);
    }

    @Transactional
    public Student createStudent(String studentId, String firstName, String lastName, String courseYearLevel) {
        ValidationUtil.requireValidStudentId(studentId);
        ValidationUtil.requireNonBlank(firstName, "First name");
        ValidationUtil.requireNonBlank(lastName, "Last name");

        if (studentRepository.findById(studentId).isPresent()) {
            throw new BusinessRuleException("Student ID already exists.");
        }

        Student student = Student.builder()
                .studentId(studentId)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .courseYearLevel(courseYearLevel)
                .status("active")
                .build();

        studentRepository.insert(student);
        Student saved = getStudentById(studentId);
        auditLogService.writeAuditLog(null, "STUDENT_CREATED", "students", studentId, null, null, null);
        return saved;
    }

    @Transactional
    public Student updateStudent(String studentId, String firstName, String lastName, String courseYearLevel, String status) {
        Student existing = getStudentById(studentId);
        ValidationUtil.requireNonBlank(firstName, "First name");
        ValidationUtil.requireNonBlank(lastName, "Last name");
        ValidationUtil.requireNonBlank(status, "Status");

        Student updated = Student.builder()
                .studentId(existing.getStudentId())
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .courseYearLevel(courseYearLevel)
                .status(status)
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        studentRepository.update(updated);
        Student saved = getStudentById(studentId);
        auditLogService.writeAuditLog(null, "STUDENT_UPDATED", "students", studentId, null, null, null);
        return saved;
    }

    @Transactional
    public void deactivateStudent(String studentId) {
        Student existing = getStudentById(studentId);
        if ("inactive".equalsIgnoreCase(existing.getStatus())) {
            return;
        }

        studentRepository.setStatus(studentId, "inactive");
        auditLogService.writeAuditLog(null, "STUDENT_DEACTIVATED", "students", studentId, null, null, null);
    }
}
