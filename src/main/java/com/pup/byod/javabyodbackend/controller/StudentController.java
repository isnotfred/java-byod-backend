package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.Student;
import com.pup.byod.javabyodbackend.service.StudentService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> listStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{studentId}")
    public Student getStudent(@PathVariable String studentId) {
        return studentService.getStudentById(studentId);
    }

    @GetMapping("/search")
    public List<Student> searchStudents(@RequestParam String keyword) {
        return studentService.searchStudents(keyword);
    }

    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody CreateStudentRequest request) {
        ValidationUtil.requireValidStudentId(request.studentId);
        ValidationUtil.requireValidName(request.firstName, "First name");
        ValidationUtil.requireValidName(request.lastName, "Last name");

        Student created = studentService.createStudent(
                request.studentId,
                request.firstName,
                request.lastName,
                request.courseYearLevel
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{studentId}")
    public Student updateStudent(@PathVariable String studentId, @RequestBody UpdateStudentRequest request) {
        ValidationUtil.requireValidName(request.firstName, "First name");
        ValidationUtil.requireValidName(request.lastName, "Last name");
        ValidationUtil.requireNonBlank(request.status, "Status");

        return studentService.updateStudent(
                studentId,
                request.firstName,
                request.lastName,
                request.courseYearLevel,
                request.status
        );
    }

    @PutMapping("/{studentId}/deactivate")
    public Map<String, Object> deactivateStudent(@PathVariable String studentId) {
        studentService.deactivateStudent(studentId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Student deactivated.");
        return body;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = studentService.importFromCsv(file);
        return ResponseEntity.ok(result);
    }

    public static class CreateStudentRequest {
        public String studentId;
        public String firstName;
        public String lastName;
        public String courseYearLevel;
    }

    public static class UpdateStudentRequest {
        public String firstName;
        public String lastName;
        public String courseYearLevel;
        public String status;
    }
}
