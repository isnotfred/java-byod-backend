package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.service.SuperAdminService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admins")
    public ResponseEntity<User> createAdmin(@RequestBody CreateAccountRequest request) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");
        ValidationUtil.requireNonBlank(request.username, "Username");
        ValidationUtil.requireNonBlank(request.password, "Password");
        ValidationUtil.requireNonBlank(request.fullName, "Full name");

        User created = superAdminService.createAdminAccount(
                request.actingUserId,
                request.username,
                request.password,
                request.fullName
        );
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/guards")
    public ResponseEntity<User> createGuard(@RequestBody CreateAccountRequest request) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");
        ValidationUtil.requireNonBlank(request.username, "Username");
        ValidationUtil.requireNonBlank(request.password, "Password");
        ValidationUtil.requireNonBlank(request.fullName, "Full name");

        User created = superAdminService.createGuardAccount(
                request.actingUserId,
                request.username,
                request.password,
                request.fullName
        );
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}")
    public User updateAccount(@PathVariable int userId, @RequestBody UpdateAccountRequest request) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");
        ValidationUtil.requireNonBlank(request.fullName, "Full name");
        ValidationUtil.requireNonBlank(request.role, "Role");
        ValidationUtil.requireNonBlank(request.status, "Status");

        User updated = superAdminService.updateAccount(
                request.actingUserId,
                userId,
                request.fullName,
                request.role,
                request.status
        );
        updated.setPasswordHash(null);
        return updated;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/deactivate")
    public User deactivateAccount(@PathVariable int userId, @RequestBody DeactivateAccountRequest request) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");

        User deactivated = superAdminService.deactivateUserBySuper(request.actingUserId, userId);
        deactivated.setPasswordHash(null);
        return deactivated;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/users/{userId}/role")
    public User changeUserRole(@PathVariable int userId, @RequestBody ChangeRoleRequest request) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");
        ValidationUtil.requireNonBlank(request.role, "Role");

        User updated = superAdminService.changeUserRole(request.actingUserId, userId, request.role);
        updated.setPasswordHash(null);
        return updated;
    }

    public static class CreateAccountRequest {
        public Integer actingUserId;
        public String username;
        public String password;
        public String fullName;
    }

    public static class UpdateAccountRequest {
        public Integer actingUserId;
        public String fullName;
        public String role;
        public String status;
    }

    public static class DeactivateAccountRequest {
        public Integer actingUserId;
    }

    public static class ChangeRoleRequest {
        public Integer actingUserId;
        public String role;
    }
}
