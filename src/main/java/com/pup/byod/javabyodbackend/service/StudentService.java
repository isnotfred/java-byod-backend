package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.StudentDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.Student;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Service
public class StudentService {

    private final StudentDAO studentRepository;
    private final AuditLogService auditLogService;

    public StudentService(StudentDAO studentRepository, AuditLogService auditLogService) {
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
    public Student createStudent(String studentId, String firstName, String lastName, String courseYearLevel, String contactNumber) {
        ValidationUtil.requireValidStudentId(studentId);
        ValidationUtil.requireValidName(firstName, "First name");
        ValidationUtil.requireValidName(lastName, "Last name");

        if (studentRepository.findById(studentId).isPresent()) {
            throw new BusinessRuleException("Student ID already exists.");
        }

        Student student = Student.builder()
                .studentId(studentId)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .courseYearLevel(courseYearLevel)
                .contactNumber(contactNumber != null ? contactNumber.trim() : null)
                .status("active")
                .build();

        studentRepository.insert(student);
        Student saved = getStudentById(studentId);
        auditLogService.writeAuditLog(null, "STUDENT_CREATED", "students", studentId, null, null, null);
        return saved;
    }

    @Transactional
    public Student updateStudent(String studentId, String firstName, String lastName, String courseYearLevel, String contactNumber, String status) {
        Student existing = getStudentById(studentId);
        ValidationUtil.requireValidName(firstName, "First name");
        ValidationUtil.requireValidName(lastName, "Last name");
        ValidationUtil.requireNonBlank(status, "Status");

        Student updated = Student.builder()
                .studentId(existing.getStudentId())
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .courseYearLevel(courseYearLevel)
                .contactNumber(contactNumber != null ? contactNumber.trim() : null)
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

    @Transactional
    public Map<String, Object> importFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("CSV file is required.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessRuleException("File must be a .csv file.");
        }

        int inserted = 0;
        int skipped = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream()));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader("student_id", "first_name", "last_name", "course_year_level", "contact_number")
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build())
        ) {
            for (CSVRecord record : csvParser) {
                int rowNum = (int) record.getRecordNumber();

                String studentId     = record.get("student_id");
                String firstName     = record.get("first_name");
                String lastName      = record.get("last_name");
                String courseYearLevel = record.get("course_year_level");
                String contactNumber = record.isMapped("contact_number") && record.isSet("contact_number") ? record.get("contact_number") : null;

                // Validate row
                List<String> rowErrors = new ArrayList<>();

                if (studentId == null || studentId.isBlank()) {
                    rowErrors.add("student_id is required.");
                }
                try {
                    ValidationUtil.requireValidName(firstName, "first_name");
                } catch (Exception e) {
                    rowErrors.add(e.getMessage());
                }
                try {
                    ValidationUtil.requireValidName(lastName, "last_name");
                } catch (Exception e) {
                    rowErrors.add(e.getMessage());
                }

                if (!rowErrors.isEmpty()) {
                    Map<String, Object> errorEntry = new LinkedHashMap<>();
                    errorEntry.put("row", rowNum);
                    errorEntry.put("studentId", studentId);
                    errorEntry.put("reasons", rowErrors);
                    errors.add(errorEntry);
                    continue;
                }

                // Skip duplicates
                if (studentRepository.findById(studentId).isPresent()) {
                    skipped++;
                    continue;
                }

                // Insert
                try {
                    Student student = Student.builder()
                            .studentId(studentId)
                            .firstName(firstName)
                            .lastName(lastName)
                            .courseYearLevel(courseYearLevel)
                            .contactNumber(contactNumber)
                            .status("active")
                            .build();

                    studentRepository.insert(student);
                    auditLogService.writeAuditLog(
                            null, "STUDENT_CREATED", "students", studentId, null, null, null);
                    inserted++;

                } catch (Exception e) {
                    Map<String, Object> errorEntry = new LinkedHashMap<>();
                    errorEntry.put("row", rowNum);
                    errorEntry.put("studentId", studentId);
                    errorEntry.put("reasons", List.of(e.getMessage()));
                    errors.add(errorEntry);
                }
            }

        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read CSV file: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }
}
