package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.Student;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StudentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Student> rowMapper = (rs, rowNum) -> Student.builder()
            .studentId(rs.getString("student_id"))
            .firstName(rs.getString("first_name"))
            .lastName(rs.getString("last_name"))
            .courseYearLevel(rs.getString("course_year_level"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY last_name, first_name";
        return jdbc.query(sql, rowMapper);
    }

    public Optional<Student> findById(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = :studentId";
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<Student> search(String keyword) {
        String sql = """
                SELECT * FROM students
                WHERE lower(student_id) LIKE lower(:keyword)
                   OR lower(first_name) LIKE lower(:keyword)
                   OR lower(last_name) LIKE lower(:keyword)
                ORDER BY last_name, first_name
                """;
        var params = new MapSqlParameterSource("keyword", "%" + keyword + "%");
        return jdbc.query(sql, params, rowMapper);
    }

    public int insert(Student student) {
        String sql = """
                INSERT INTO students (
                    student_id,
                    first_name,
                    last_name,
                    course_year_level,
                    status
                ) VALUES (
                    :studentId,
                    :firstName,
                    :lastName,
                    :courseYearLevel,
                    :status
                )
                """;

        var params = new MapSqlParameterSource()
                .addValue("studentId", student.getStudentId())
                .addValue("firstName", student.getFirstName())
                .addValue("lastName", student.getLastName())
                .addValue("courseYearLevel", student.getCourseYearLevel())
                .addValue("status", student.getStatus());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"student_id"});
        return keyHolder.getKey().intValue();
    }

    public int update(Student student) {
        String sql = """
                UPDATE students
                SET first_name = :firstName,
                    last_name = :lastName,
                    course_year_level = :courseYearLevel,
                    status = :status
                WHERE student_id = :studentId
                """;

        var params = new MapSqlParameterSource()
                .addValue("firstName", student.getFirstName())
                .addValue("lastName", student.getLastName())
                .addValue("courseYearLevel", student.getCourseYearLevel())
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
