package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.ActiveRequest;
import com.pup.byod.javabyodbackend.model.Request;
import com.pup.byod.javabyodbackend.model.enums.RequestStatus;
import com.pup.byod.javabyodbackend.model.enums.RequestType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class RequestDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public RequestDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────

    private final RowMapper<Request> requestRowMapper = (rs, rowNum) -> Request.builder()
            .requestId(rs.getInt("request_id"))
            .requestType(RequestType.fromString(rs.getString("request_type")))
            .studentId(rs.getString("student_id"))
            .eventName(rs.getString("event_name"))
            .venue(rs.getString("venue"))
            .organization(rs.getString("organization"))
            .responsiblePerson(rs.getString("responsible_person"))
            .purpose(rs.getString("purpose"))
            .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
            .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
            .expectedIngressTime(rs.getTime("expected_ingress_time") != null ? rs.getTime("expected_ingress_time").toLocalTime() : null)
            .expectedEgressTime(rs.getTime("expected_egress_time") != null ? rs.getTime("expected_egress_time").toLocalTime() : null)
            .status(RequestStatus.fromString(rs.getString("status")))
            .isSubmitted(rs.getBoolean("is_submitted"))
            .isAccommodated(rs.getBoolean("is_accommodated"))
            .reviewedBy(rs.getObject("reviewed_by") != null ? rs.getInt("reviewed_by") : null)
            .reviewedAt(rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null)
            .remarks(rs.getString("remarks"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<ActiveRequest> activeRequestRowMapper = (rs, rowNum) -> ActiveRequest.builder()
            .requestId(rs.getInt("request_id"))
            .requestType(rs.getString("request_type"))
            .studentId(rs.getString("student_id"))
            .studentName(rs.getString("student_name"))
            .eventName(rs.getString("event_name"))
            .venue(rs.getString("venue"))
            .organization(rs.getString("organization"))
            .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
            .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
            .expectedIngressTime(rs.getTime("expected_ingress_time") != null ? rs.getTime("expected_ingress_time").toLocalTime() : null)
            .expectedEgressTime(rs.getTime("expected_egress_time") != null ? rs.getTime("expected_egress_time").toLocalTime() : null)
            .status(rs.getString("status"))
            .deviceCount(rs.getInt("device_count"))
            .build();

    // ── Queries ──────────────────────────────────────────────────────

    public List<Request> findAll() {
        String sql = "SELECT * FROM requests ORDER BY created_at DESC";
        return jdbc.query(sql, requestRowMapper);
    }

    public Optional<Request> findById(int requestId) {
        String sql = "SELECT * FROM requests WHERE request_id = :requestId";
        var params = new MapSqlParameterSource("requestId", requestId);
        return jdbc.query(sql, params, requestRowMapper).stream().findFirst();
    }

    public List<Request> findByStudentId(String studentId) {
        String sql = "SELECT * FROM requests WHERE student_id = :studentId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, requestRowMapper);
    }

    /**
     * Find approved requests for a student valid for a given date.
     */
    public List<Request> findActiveRequestsForStudent(String studentId, LocalDate date) {
        String sql = """
                SELECT * FROM requests
                WHERE student_id = :studentId
                  AND status = 'approved'
                  AND start_date <= :date
                  AND end_date >= :date
                ORDER BY created_at DESC
                """;
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("date", date);
        return jdbc.query(sql, params, requestRowMapper);
    }

    public List<Request> findPendingRequests() {
        String sql = "SELECT * FROM requests WHERE status = 'pending' ORDER BY created_at ASC";
        return jdbc.query(sql, requestRowMapper);
    }

    public List<ActiveRequest> findActiveRequests() {
        String sql = "SELECT * FROM v_active_requests ORDER BY start_date";
        return jdbc.query(sql, activeRequestRowMapper);
    }

    // ── Mutations ────────────────────────────────────────────────────

    public int insert(Request request) {
        String sql = """
                INSERT INTO requests (
                    request_type, student_id, event_name, venue, organization, responsible_person,
                    purpose, start_date, end_date,
                    expected_ingress_time, expected_egress_time, status,
                    is_submitted, is_accommodated, reviewed_by, reviewed_at, remarks
                ) VALUES (
                    :requestType, :studentId, :eventName, :venue, :organization, :responsiblePerson,
                    :purpose, :startDate, :endDate,
                    :expectedIngressTime, :expectedEgressTime, :status,
                    :isSubmitted, :isAccommodated, :reviewedBy, :reviewedAt, :remarks
                )
                """;

        var params = new MapSqlParameterSource()
                .addValue("requestType", request.getRequestType() != null ? request.getRequestType().name() : "normal")
                .addValue("studentId", request.getStudentId())
                .addValue("eventName", request.getEventName())
                .addValue("venue", request.getVenue())
                .addValue("organization", request.getOrganization())
                .addValue("responsiblePerson", request.getResponsiblePerson())
                .addValue("purpose", request.getPurpose())
                .addValue("startDate", request.getStartDate())
                .addValue("endDate", request.getEndDate())
                .addValue("expectedIngressTime", request.getExpectedIngressTime())
                .addValue("expectedEgressTime", request.getExpectedEgressTime())
                .addValue("status", request.getStatus() != null ? request.getStatus().name() : "pending")
                .addValue("isSubmitted", request.isSubmitted())
                .addValue("isAccommodated", request.isAccommodated())
                .addValue("reviewedBy", request.getReviewedBy())
                .addValue("reviewedAt", request.getReviewedAt())
                .addValue("remarks", request.getRemarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"request_id"});
        return keyHolder.getKey().intValue();
    }

    public int update(Request request) {
        String sql = """
                UPDATE requests
                SET request_type       = :requestType,
                    event_name         = :eventName,
                    venue              = :venue,
                    organization       = :organization,
                    responsible_person = :responsiblePerson,
                    purpose            = :purpose,
                    start_date         = :startDate,
                    end_date           = :endDate,
                    expected_ingress_time = :expectedIngressTime,
                    expected_egress_time  = :expectedEgressTime,
                    status             = :status,
                    is_submitted       = :isSubmitted,
                    is_accommodated    = :isAccommodated,
                    reviewed_by        = :reviewedBy,
                    reviewed_at        = :reviewedAt,
                    remarks            = :remarks
                WHERE request_id = :requestId
                """;

        var params = new MapSqlParameterSource()
                .addValue("requestType", request.getRequestType() != null ? request.getRequestType().name() : "normal")
                .addValue("eventName", request.getEventName())
                .addValue("venue", request.getVenue())
                .addValue("organization", request.getOrganization())
                .addValue("responsiblePerson", request.getResponsiblePerson())
                .addValue("purpose", request.getPurpose())
                .addValue("startDate", request.getStartDate())
                .addValue("endDate", request.getEndDate())
                .addValue("expectedIngressTime", request.getExpectedIngressTime())
                .addValue("expectedEgressTime", request.getExpectedEgressTime())
                .addValue("status", request.getStatus() != null ? request.getStatus().name() : "pending")
                .addValue("isSubmitted", request.isSubmitted())
                .addValue("isAccommodated", request.isAccommodated())
                .addValue("reviewedBy", request.getReviewedBy())
                .addValue("reviewedAt", request.getReviewedAt())
                .addValue("remarks", request.getRemarks())
                .addValue("requestId", request.getRequestId());

        return jdbc.update(sql, params);
    }

    public int setStatus(int requestId, String status) {
        String sql = "UPDATE requests SET status = :status WHERE request_id = :requestId";
        var params = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("requestId", requestId);
        return jdbc.update(sql, params);
    }
}
