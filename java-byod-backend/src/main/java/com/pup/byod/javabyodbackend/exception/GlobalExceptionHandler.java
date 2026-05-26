package com.pup.byod.javabyodbackend.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 404 Not Found ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- 422 Unprocessable Entity (business rule violations) ---
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // --- 409 Conflict (duplicate unique key) ---
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DuplicateKeyException ex) {
        // Sanitise the raw JDBC message before surfacing it
        String msg = "A record with that value already exists.";
        if (ex.getMessage() != null && ex.getMessage().contains("serial_number")) {
            msg = "Serial number already exists.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("username")) {
            msg = "Username already exists.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("student_id")) {
            msg = "Student ID already exists.";
        }
        return buildResponse(HttpStatus.CONFLICT, msg);
    }

    // --- 400 Bad Request (DB trigger blocks, validation failures) ---
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        // Surface trigger error messages (PostgreSQL raises them via RAISE EXCEPTION)
        String raw = ex.getMostSpecificCause().getMessage();
        String msg = (raw != null) ? raw : "Database error occurred.";
        return buildResponse(HttpStatus.BAD_REQUEST, msg);
    }

    // --- 400 Bad Request (manual validation) ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- 500 Fallback ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // ---------------------------------------------------------------
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}