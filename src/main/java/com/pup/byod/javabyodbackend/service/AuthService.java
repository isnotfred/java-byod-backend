package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.UserDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import com.pup.byod.javabyodbackend.util.PasswordUtil;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserDAO userDAO;
    private final AuditLogService auditLogService;
    private final ResendEmailService resendEmailService;

    public AuthService(UserDAO userDAO, AuditLogService auditLogService, ResendEmailService resendEmailService) {
        this.userDAO = userDAO;
        this.auditLogService = auditLogService;
        this.resendEmailService = resendEmailService;
    }

    public User login(String username, String password) {
        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(password, "Password");

        var userOpt = userDAO.findByUsername(username);

        // Unknown username — log failed attempt without a userId, then reject.
        // Return a generic message so we don't reveal which usernames exist.
        if (userOpt.isEmpty()) {
            auditLogService.writeAuditLog(null, "USER_LOGIN_FAILED", "users", null, null, null, null);
            throw new BusinessRuleException("Invalid username or password.");
        }

        User user = userOpt.get();

        // Check account status BEFORE verifying password.
        // Architecture doc maps inactive → HTTP 403.
        // GlobalExceptionHandler maps BusinessRuleException → 422, so we keep a
        // distinct message here; the controller can map it to 403 if needed,
        // or you can add a dedicated InactiveAccountException later.
        if ("inactive".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessRuleException("Account is inactive.");
        }

        // Wrong password — log the failed attempt against the known userId.
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            auditLogService.writeAuditLog(
                    user.getUserId(), "USER_LOGIN_FAILED", "users",
                    user.getUserId().toString(), null, null, null);
            throw new BusinessRuleException("Invalid username or password.");
        }

        auditLogService.writeAuditLog(
                user.getUserId(), "USER_LOGIN", "users",
                user.getUserId().toString(), null, null, null);

        return user;
    }

    public void logout(int userId) {
        var userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessRuleException("User not found.");
        }
        auditLogService.writeAuditLog(
                userId, "USER_LOGOUT", "users",
                String.valueOf(userId), null, null, null);
    }

    public boolean isSuperAdmin(Role role) {
        return role == Role.super_admin;
    }

    public boolean isAdminOrAbove(Role role) {
        return role == Role.admin || role == Role.super_admin;
    }

    public boolean isAnyStaff(Role role) {
        return role == Role.guard || role == Role.admin || role == Role.super_admin;
    }

    public void initiatePasswordReset(String email) {
        ValidationUtil.requireNonBlank(email, "Email");

        var userOpt = userDAO.findByUsernameOrEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        if ("inactive".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        userDAO.updatePasswordResetToken(user.getUserId(), token, expiresAt);

        String recipientEmail = user.getEmail() != null && !user.getEmail().isBlank()
                ? user.getEmail()
                : user.getUsername();

        resendEmailService.sendPasswordResetEmail(recipientEmail, user.getFullName(), token);
    }

    public void resetPassword(String token, String newPassword) {
        ValidationUtil.requireNonBlank(token, "Password reset token");
        ValidationUtil.requireValidPassword(newPassword);

        var userOpt = userDAO.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) {
            throw new BusinessRuleException("Invalid or expired password reset token.");
        }

        User user = userOpt.get();
        if ("inactive".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessRuleException("Account is inactive.");
        }

        LocalDateTime expiresAt = user.getPasswordResetExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Invalid or expired password reset token.");
        }

        String hashedPassword = PasswordUtil.hash(newPassword);
        userDAO.updatePassword(user.getUserId(), hashedPassword);
        userDAO.clearPasswordResetToken(user.getUserId());

        auditLogService.writeAuditLog(
                user.getUserId(), "USER_UPDATED", "users",
                user.getUserId().toString(), null, null, null);
    }
}