package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.User;
import com.pup.byod.javabyodbackend.model.enums.Role;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public UserDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .userId(rs.getInt("user_id"))
            .username(rs.getString("username"))
            .email(rs.getString("email"))
            .passwordHash(rs.getString("password_hash"))
            .fullName(rs.getString("full_name"))
            .role(Role.fromString(rs.getString("role")))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    // ── Queries ──────────────────────────────────────────────────────

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        return jdbc.query(sql, userRowMapper);
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = :userId";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbc.query(sql, params, userRowMapper).stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = :username";
        var params = new MapSqlParameterSource("username", username);
        return jdbc.query(sql, params, userRowMapper).stream().findFirst();
    }

    // ── Mutations ────────────────────────────────────────────────────

    /**
     * Insert a new user and return the generated user_id.
     */
    public int insert(User user) {
        String sql = """
                INSERT INTO users (username, email, password_hash, full_name, role, status)
                VALUES (:username, :email, :passwordHash, :fullName, :role, :status)
                """;

        var params = new MapSqlParameterSource()
                .addValue("username", user.getUsername())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("fullName", user.getFullName())
                .addValue("role", user.getRole().name())
                .addValue("status", user.getStatus());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"user_id"});
        return keyHolder.getKey().intValue();
    }

    /**
     * Update mutable fields: full_name, role, status.
     * username and password_hash are updated via dedicated methods.
     */
    public int update(User user) {
        String sql = """
                UPDATE users
                SET full_name = :fullName,
                    email     = :email,
                    role      = :role,
                    status    = :status
                WHERE user_id = :userId
                """;

        var params = new MapSqlParameterSource()
                .addValue("fullName", user.getFullName())
                .addValue("email", user.getEmail())
                .addValue("role", user.getRole().name())
                .addValue("status", user.getStatus())
                .addValue("userId", user.getUserId());

        return jdbc.update(sql, params);
    }

    public int updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = :hash WHERE user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("hash", newPasswordHash)
                .addValue("userId", userId);
        return jdbc.update(sql, params);
    }

    public int setStatus(int userId, String status) {
        String sql = "UPDATE users SET status = :status WHERE user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("userId", userId);
        return jdbc.update(sql, params);
    }

    public int setUserRole(int userId, Role role) {
        String sql = "UPDATE users SET role = :role WHERE user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("role", role.name())
                .addValue("userId", userId);
        return jdbc.update(sql, params);
    }
}