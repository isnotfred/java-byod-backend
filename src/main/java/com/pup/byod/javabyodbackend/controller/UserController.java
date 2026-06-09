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

    @PutMapping("/{id}/profile/password")
    public User changePassword(
            @PathVariable("id") int id,
            @RequestBody ChangePasswordRequest request
    ) {
        User updated = userService.updateProfilePassword(
                id,
                request.currentPassword,
                request.newPassword
        );
        updated.setPasswordHash(null);
        return updated;
    }

    @PutMapping("/{id}/profile/username")
    public User changeUsername(
            @PathVariable("id") int id,
            @RequestBody ChangeUsernameRequest request
    ) {
        User updated = userService.updateProfileUsername(id, request.username);
        updated.setPasswordHash(null);
        return updated;
    }

    public static class ChangeUsernameRequest {
        public String username;
    }

    public static class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}