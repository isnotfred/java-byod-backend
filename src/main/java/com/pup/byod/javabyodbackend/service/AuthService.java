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
        if (userOpt.isEmpty()) {
            auditLogService.writeAuditLog(null, "USER_LOGIN_FAILED", "users", null, null, null, null);
            throw new BusinessRuleException("Invalid username or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            auditLogService.writeAuditLog(user.getUserId(), "USER_LOGIN_FAILED", "users", user.getUserId().toString(), null, null, null);
            throw new BusinessRuleException("Invalid username or password.");
        }

        if (user.getStatus() != null && user.getStatus().equalsIgnoreCase("inactive")) {
            throw new BusinessRuleException("Account is inactive.");
        }

        auditLogService.writeAuditLog(user.getUserId(), "USER_LOGIN", "users", user.getUserId().toString(), null, null, null);
        return user;
    }

    public void logout(int userId) {
        var userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessRuleException("User not found.");
        }
        auditLogService.writeAuditLog(userId, "USER_LOGOUT", "users", String.valueOf(userId), null, null, null);
    }
}
