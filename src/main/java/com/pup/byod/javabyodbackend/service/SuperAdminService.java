package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.dao.UserDAO;
import com.pup.byod.javabyodbackend.dao.AuditLogDAO;
import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import com.pup.byod.javabyodbackend.exception.ResourceNotFoundException;
import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import com.pup.byod.javabyodbackend.model.AuditActionTypes;
import com.pup.byod.javabyodbackend.util.PasswordUtil;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminService {

    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;

    public SuperAdminService(UserDAO userDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.auditLogDAO = auditLogDAO;
    }

    private void requireSuperAdmin(int actingUserId) {
        User actor = userDAO.findById(actingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Acting user not found."));
        if (actor.getRole() != Role.super_admin) {
            throw new BusinessRuleException("Super admin access required.");
        }
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

    @Transactional
    public User createAdminAccount(int actingUserId, String username, String password, String fullName) {
        requireSuperAdmin(actingUserId);
        ValidationUtil.requireValidUsername(username);
        ValidationUtil.requireValidPassword(password);
        ValidationUtil.requireNonBlank(fullName, "Full name");

        if (userDAO.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Username already exists.");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(PasswordUtil.hash(password))
                .fullName(fullName)
                .role(Role.admin)
                .status("active")
                .build();

        int userId = userDAO.insert(user);
        User created = userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Created user not found."));

        auditLogDAO.writeAuditLog(actingUserId, AuditActionTypes.ADMIN_CREATED, "users", String.valueOf(userId), null, toJson(created), null);
        return created;
    }

    @Transactional
    public User createGuardAccount(int actingUserId, String username, String password, String fullName) {
        requireSuperAdmin(actingUserId);
        ValidationUtil.requireValidUsername(username);
        ValidationUtil.requireValidPassword(password);
        ValidationUtil.requireNonBlank(fullName, "Full name");

        if (userDAO.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Username already exists.");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(PasswordUtil.hash(password))
                .fullName(fullName)
                .role(Role.guard)
                .status("active")
                .build();

        int userId = userDAO.insert(user);
        User created = userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Created user not found."));

        auditLogDAO.writeAuditLog(actingUserId, AuditActionTypes.GUARD_CREATED, "users", String.valueOf(userId), null, toJson(created), null);
        return created;
    }

    @Transactional
    public User updateAccount(int actingUserId, int targetUserId, String fullName, String roleStr, String status) {
        requireSuperAdmin(actingUserId);
        User existing = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found."));

        ValidationUtil.requireNonBlank(fullName, "Full name");
        ValidationUtil.requireNonBlank(roleStr, "Role");
        ValidationUtil.requireNonBlank(status, "Status");

        Role role = Role.fromString(roleStr);
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
        User saved = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Updated user not found."));

        String actionType = (existing.getRole() == Role.admin) ? AuditActionTypes.ADMIN_UPDATED : AuditActionTypes.GUARD_UPDATED;
        if (existing.getRole() == Role.super_admin) {
            actionType = AuditActionTypes.ADMIN_UPDATED;
        }

        auditLogDAO.writeAuditLog(actingUserId, actionType, "users", String.valueOf(targetUserId), oldPayload, toJson(saved), null);
        return saved;
    }

    @Transactional
    public User deactivateAdmin(int actingUserId, int targetUserId) {
        requireSuperAdmin(actingUserId);
        User existing = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to deactivate not found."));

        if (existing.getRole() != Role.admin) {
            throw new BusinessRuleException("User is not an admin.");
        }

        if ("inactive".equalsIgnoreCase(existing.getStatus())) {
            return existing;
        }

        String oldPayload = toJson(existing);
        userDAO.setStatus(targetUserId, "inactive");
        User saved = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Deactivated user not found."));

        auditLogDAO.writeAuditLog(actingUserId, AuditActionTypes.ADMIN_DEACTIVATED, "users", String.valueOf(targetUserId), oldPayload, toJson(saved), null);
        return saved;
    }

    @Transactional
    public User deactivateGuard(int actingUserId, int targetUserId) {
        requireSuperAdmin(actingUserId);
        User existing = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to deactivate not found."));

        if (existing.getRole() != Role.guard) {
            throw new BusinessRuleException("User is not a guard.");
        }

        if ("inactive".equalsIgnoreCase(existing.getStatus())) {
            return existing;
        }

        String oldPayload = toJson(existing);
        userDAO.setStatus(targetUserId, "inactive");
        User saved = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Deactivated user not found."));

        auditLogDAO.writeAuditLog(actingUserId, AuditActionTypes.GUARD_DEACTIVATED_BY_SUPER, "users", String.valueOf(targetUserId), oldPayload, toJson(saved), null);
        return saved;
    }

    @Transactional
    public User deactivateUserBySuper(int actingUserId, int targetUserId) {
        requireSuperAdmin(actingUserId);
        User existing = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to deactivate not found."));

        if (existing.getRole() == Role.admin) {
            return deactivateAdmin(actingUserId, targetUserId);
        } else if (existing.getRole() == Role.guard) {
            return deactivateGuard(actingUserId, targetUserId);
        } else {
            throw new BusinessRuleException("Only admins and guards can be deactivated via this endpoint.");
        }
    }

    @Transactional
    public User changeUserRole(int actingUserId, int targetUserId, String newRoleStr) {
        requireSuperAdmin(actingUserId);
        User existing = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to change role not found."));

        ValidationUtil.requireNonBlank(newRoleStr, "Role");
        Role newRole = Role.fromString(newRoleStr);

        if (existing.getRole() == newRole) {
            return existing;
        }

        String oldPayload = toJson(existing);
        userDAO.setUserRole(targetUserId, newRole);
        User saved = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        auditLogDAO.writeAuditLog(actingUserId, AuditActionTypes.USER_ROLE_CHANGED, "users", String.valueOf(targetUserId), oldPayload, toJson(saved), null);
        return saved;
    }
}
