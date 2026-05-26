package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import com.pup.byod.javabyodbackend.service.UserService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> listUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(user -> user.setPasswordHash(null));
        return users;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") int id) {
        User user = userService.getUserById(id);
        user.setPasswordHash(null);
        return user;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        ValidationUtil.requireNonBlank(request.username, "Username");
        ValidationUtil.requireNonBlank(request.password, "Password");
        ValidationUtil.requireNonBlank(request.fullName, "Full name");
        ValidationUtil.requireNonBlank(request.role, "Role");

        Role role = Role.fromString(request.role);
        User created = userService.createUser(request.username, request.password, request.fullName, role);
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable("id") int id, @RequestBody UpdateUserRequest request) {
        ValidationUtil.requireNonBlank(request.fullName, "Full name");
        ValidationUtil.requireNonBlank(request.role, "Role");
        ValidationUtil.requireNonBlank(request.status, "Status");

        Role role = Role.fromString(request.role);
        User updated = userService.updateUser(id, request.fullName, role, request.status);
        updated.setPasswordHash(null);
        return updated;
    }

    @PutMapping("/{id}/deactivate")
    public Map<String, Object> deactivateUser(@PathVariable("id") int id) {
        userService.deactivateUser(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "User deactivated.");
        return body;
    }

    public static class CreateUserRequest {
        public String username;
        public String password;
        public String fullName;
        public String role;
    }

    public static class UpdateUserRequest {
        public String fullName;
        public String role;
        public String status;
    }
}
