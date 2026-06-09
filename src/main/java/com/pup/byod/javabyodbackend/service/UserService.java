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

    public User updateProfilePassword(int userId, String currentPassword, String newPassword) {
        User existing = getUserById(userId);
        ValidationUtil.requireNonBlank(currentPassword, "Current password");
        ValidationUtil.requireValidPassword(newPassword);

        if (!PasswordUtil.verify(currentPassword, existing.getPasswordHash())) {
            throw new BusinessRuleException("Incorrect current password.");
        }

        String oldPayload = toJson(existing);
        userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
        
        if ("pending".equalsIgnoreCase(existing.getStatus())) {
            userDAO.setStatus(userId, "active");
        }
        
        User saved = getUserById(userId);
        auditLogService.writeAuditLog(userId, "USER_UPDATED", "users", String.valueOf(userId), oldPayload, toJson(saved), null);
        return saved;
    }

    private String toJson(User user) {
        if (user == null) {
            return null;
        }
        return String.format(
                "{\"userId\":%d,\"username\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"status\":\"%s\"}",
                user.getUserId(),
                escape(user.getUsername()),
                escape(user.getEmail()),
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