package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.UserDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.util.PasswordUtil;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserDAO userDAO;
    private final AuditLogService auditLogService;

    public AuthService(UserDAO userDAO, AuditLogService auditLogService) {
        this.userDAO = userDAO;
        this.auditLogService = auditLogService;
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
}