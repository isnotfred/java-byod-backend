package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.Student;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public StudentDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<Student> studentRowMapper = (rs, rowNum) -> Student.builder()
            .studentId(rs.getString("student_id"))
            .firstName(rs.getString("first_name"))
            .lastName(rs.getString("last_name"))
            .course(rs.getString("course"))
            .yearLevel(rs.getObject("year_level") != null ? rs.getInt("year_level") : null)
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    // ── Queries ──────────────────────────────────────────────────────

    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY last_name, first_name";
        return jdbc.query(sql, studentRowMapper);
    }

    public Optional<Student> findById(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = :studentId";
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, studentRowMapper).stream().findFirst();
    }

    /**
     * Case-insensitive search on student_id, first_name, or last_name.
     */
    public List<Student> search(String keyword) {
        String sql = """
                SELECT * FROM students
                WHERE lower(student_id) LIKE lower(:kw)
                   OR lower(first_name) LIKE lower(:kw)
                   OR lower(last_name)  LIKE lower(:kw)
                ORDER BY last_name, first_name
                """;
        var params = new MapSqlParameterSource("kw", "%" + keyword + "%");
        return jdbc.query(sql, params, studentRowMapper);
    }

    public List<Student> findByStatus(String status) {
        String sql = "SELECT * FROM students WHERE status = :status ORDER BY last_name, first_name";
        var params = new MapSqlParameterSource("status", status);
        return jdbc.query(sql, params, studentRowMapper);
    }

    // ── Mutations ────────────────────────────────────────────────────

    public int insert(Student student) {
        String sql = """
                INSERT INTO students (student_id, first_name, last_name, course, year_level, status)
                VALUES (:studentId, :firstName, :lastName, :course, :yearLevel, :status)
                """;

        var params = new MapSqlParameterSource()
                .addValue("studentId", student.getStudentId())
                .addValue("firstName", student.getFirstName())
                .addValue("lastName", student.getLastName())
                .addValue("course", student.getCourse())
                .addValue("yearLevel", student.getYearLevel())
                .addValue("status", student.getStatus());

        return jdbc.update(sql, params);
    }

    public int update(Student student) {
        String sql = """
                UPDATE students
                SET first_name  = :firstName,
                    last_name   = :lastName,
                    course      = :course,
                    year_level  = :yearLevel,
                    status      = :status
                WHERE student_id = :studentId
                """;

        var params = new MapSqlParameterSource()
                .addValue("firstName", student.getFirstName())
                .addValue("lastName", student.getLastName())
                .addValue("course", student.getCourse())
                .addValue("yearLevel", student.getYearLevel())
                .addValue("status", student.getStatus())
                .addValue("studentId", student.getStudentId());

        return jdbc.update(sql, params);
    }

    public int setStatus(String studentId, String status) {
        String sql = "UPDATE students SET status = :status WHERE student_id = :studentId";
        var params = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("studentId", studentId);
        return jdbc.update(sql, params);
    }
}