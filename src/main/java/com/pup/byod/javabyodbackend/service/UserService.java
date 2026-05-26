package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.UserDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import com.pup.byod.javabyodbackend.util.PasswordUtil;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserDAO userDAO;
    private final AuditLogService auditLogService;

    public UserService(UserDAO userDAO, AuditLogService auditLogService) {
        this.userDAO = userDAO;
        this.auditLogService = auditLogService;
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public User getUserById(int userId) {
        return userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    public User createUser(String username, String password, String fullName, Role role) {
        ValidationUtil.requireValidUsername(username);
        ValidationUtil.requireValidPassword(password);
        ValidationUtil.requireNonBlank(fullName, "Full name");
        if (role == null) {
            throw new BusinessRuleException("Role is required.");
        }

        if (userDAO.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Username already exists.");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(PasswordUtil.hash(password))
                .fullName(fullName)
                .role(role)
                .status("active")
                .build();

        int userId = userDAO.insert(user);
        User created = getUserById(userId);
        auditLogService.writeAuditLog(userId, "USER_CREATED", "users", String.valueOf(userId), null, toJson(created), null);
        return created;
    }

    public User updateUser(int userId, String fullName, Role role, String status) {
        User existing = getUserById(userId);
        ValidationUtil.requireNonBlank(fullName, "Full name");
        if (role == null) {
            throw new BusinessRuleException("Role is required.");
        }
        if (status == null || status.isBlank()) {
            throw new BusinessRuleException("Status is required.");
        }

        String oldPayload = toJson(existing);

        User updated = User.builder()
                .userId(existing.getUserId())
                .username(existing.getUsername())
                .passwordHash(existing.getPasswordHash())
                .fullName(fullName)
                .role(role)
                .status(status)
                .build();

        userDAO.update(updated);
        User saved = getUserById(userId);
        auditLogService.writeAuditLog(userId, "USER_UPDATED", "users", String.valueOf(userId), oldPayload, toJson(saved), null);
        return saved;
    }

    public void deactivateUser(int userId) {
        User existing = getUserById(userId);
        if (existing.getStatus() != null && existing.getStatus().equalsIgnoreCase("inactive")) {
            return;
        }
        userDAO.setStatus(userId, "inactive");
        auditLogService.writeAuditLog(userId, "USER_DEACTIVATED", "users", String.valueOf(userId), toJson(existing), "{\"status\":\"inactive\"}", null);
    }

    private String toJson(User user) {
        if (user == null) {
            return null;
        }
        return String.format(
                "{\"userId\":%d,\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"status\":\"%s\"}",
                user.getUserId(),
                escape(user.getUsername()),
                escape(user.getFullName()),
                user.getRole() != null ? user.getRole().name() : null,
                escape(user.getStatus())
        );
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}