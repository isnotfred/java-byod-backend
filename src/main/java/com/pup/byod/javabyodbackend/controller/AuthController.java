package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.username, request.password);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Logout successful.");
        return ResponseEntity.ok(body);
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LogoutRequest {
        public Integer userId;
    }
}