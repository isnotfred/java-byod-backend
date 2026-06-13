package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.EventRequest;
import com.pup.byod.javabyodbackend.model.ActiveEventRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRequestDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public EventRequestDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<EventRequest> rowMapper = (rs, rowNum) -> EventRequest.builder()
            .eventRequestId(rs.getInt("event_request_id"))
            .studentId(rs.getString("student_id"))
            .responsiblePerson(rs.getString("responsible_person"))
            .organization(rs.getString("organization"))
            .eventName(rs.getString("event_name"))
            .eventPurpose(rs.getString("event_purpose"))
            .approvalDocType(rs.getString("approval_doc_type"))
            .approvalDocRef(rs.getString("approval_doc_ref"))
            .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
            .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
            .status(rs.getString("status"))
            .isSubmitted(rs.getBoolean("is_submitted"))
            .isAccommodated(rs.getBoolean("is_accommodated"))
            .reviewedBy(rs.getObject("reviewed_by") != null ? rs.getInt("reviewed_by") : null)
            .reviewedAt(rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null)
            .remarks(rs.getString("remarks"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<ActiveEventRequest> activeEventRequestRowMapper = (rs, rowNum) -> ActiveEventRequest.builder()
            .eventRequestId(rs.getInt("event_request_id"))
            .studentId(rs.getString("student_id"))
            .studentName(rs.getString("student_name"))
            .eventName(rs.getString("event_name"))
            .organization(rs.getString("organization"))
            .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
            .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
            .status(rs.getString("status"))
            .deviceCount(rs.getInt("device_count"))
            .build();

    public List<EventRequest> findAll() {
        String sql = "SELECT * FROM event_requests ORDER BY created_at DESC";
        return jdbc.query(sql, rowMapper);
    }

    public Optional<EventRequest> findById(int eventRequestId) {
        String sql = "SELECT * FROM event_requests WHERE event_request_id = :eventRequestId";
        var params = new MapSqlParameterSource("eventRequestId", eventRequestId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<EventRequest> findByStudentId(String studentId) {
        String sql = "SELECT * FROM event_requests WHERE student_id = :studentId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, rowMapper);
    }

    public List<ActiveEventRequest> findActiveRequests() {
        String sql = "SELECT * FROM v_active_event_requests ORDER BY event_request_id DESC";
        return jdbc.query(sql, activeEventRequestRowMapper);
    }

    public List<ActiveEventRequest> findApprovedActiveRequestsForGuard() {
        String sql = """
                SELECT
                    er.event_request_id,
                    er.student_id,
                    s.first_name || ' ' || s.last_name AS student_name,
                    er.event_name,
                    er.organization,
                    er.start_date,
                    er.end_date,
                    er.status,
                    COUNT(erd.event_device_id) AS device_count
                FROM   event_requests er
                JOIN   students s ON s.student_id = er.student_id
                LEFT   JOIN event_request_devices erd
                            ON erd.event_request_id = er.event_request_id
                WHERE  er.status = 'approved'
                  AND  CURRENT_DATE BETWEEN er.start_date AND er.end_date
                GROUP  BY
                    er.event_request_id, er.student_id,
                    s.first_name, s.last_name,
                    er.event_name, er.organization,
                    er.start_date, er.end_date, er.status
                ORDER  BY er.event_request_id DESC""";
        return jdbc.query(sql, activeEventRequestRowMapper);
    }


    public int insert(EventRequest request) {
        String sql = """
                INSERT INTO event_requests (
                    student_id,
                    responsible_person,
                    organization,
                    event_name,
                    event_purpose,
                    approval_doc_type,
                    approval_doc_ref,
                    start_date,
                    end_date,
                    status,
                    is_submitted,
                    is_accommodated,
                    reviewed_by,
                    reviewed_at,
                    remarks
                ) VALUES (
                    :studentId,
                    :responsiblePerson,
                    :organization,
                    :eventName,
                    :eventPurpose,
                    :approvalDocType,
                    :approvalDocRef,
                    :startDate,
                    :endDate,
                    :status,
                    :isSubmitted,
                    :isAccommodated,
                    :reviewedBy,
                    :reviewedAt,
                    :remarks
                )""";

        var params = new MapSqlParameterSource()
                .addValue("studentId", request.getStudentId())
                .addValue("responsiblePerson", request.getResponsiblePerson())
                .addValue("organization", request.getOrganization())
                .addValue("eventName", request.getEventName())
                .addValue("eventPurpose", request.getEventPurpose())
                .addValue("approvalDocType", request.getApprovalDocType())
                .addValue("approvalDocRef", request.getApprovalDocRef())
                .addValue("startDate", request.getStartDate() != null ? Date.valueOf(request.getStartDate()) : null)
                .addValue("endDate", request.getEndDate() != null ? Date.valueOf(request.getEndDate()) : null)
                .addValue("status", request.getStatus())
                .addValue("isSubmitted", request.getIsSubmitted())
                .addValue("isAccommodated", request.getIsAccommodated())
                .addValue("reviewedBy", request.getReviewedBy())
                .addValue("reviewedAt", request.getReviewedAt() != null ? Timestamp.valueOf(request.getReviewedAt()) : null)
                .addValue("remarks", request.getRemarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"event_request_id"});
        return keyHolder.getKey().intValue();
    }

    public int update(EventRequest request) {
        String sql = """
                UPDATE event_requests
                SET responsible_person = :responsiblePerson,
                    organization       = :organization,
                    event_name         = :eventName,
                    event_purpose      = :eventPurpose,
                    approval_doc_type  = :approvalDocType,
                    approval_doc_ref   = :approvalDocRef,
                    start_date         = :startDate,
                    end_date           = :endDate,
                    status             = :status,
                    is_submitted       = :isSubmitted,
                    is_accommodated    = :isAccommodated,
                    reviewed_by        = :reviewedBy,
                    reviewed_at        = :reviewedAt,
                    remarks            = :remarks
                WHERE event_request_id = :eventRequestId""";

        var params = new MapSqlParameterSource()
                .addValue("responsiblePerson", request.getResponsiblePerson())
                .addValue("organization", request.getOrganization())
                .addValue("eventName", request.getEventName())
                .addValue("eventPurpose", request.getEventPurpose())
                .addValue("approvalDocType", request.getApprovalDocType())
                .addValue("approvalDocRef", request.getApprovalDocRef())
                .addValue("startDate", request.getStartDate() != null ? Date.valueOf(request.getStartDate()) : null)
                .addValue("endDate", request.getEndDate() != null ? Date.valueOf(request.getEndDate()) : null)
                .addValue("status", request.getStatus())
                .addValue("isSubmitted", request.getIsSubmitted())
                .addValue("isAccommodated", request.getIsAccommodated())
                .addValue("reviewedBy", request.getReviewedBy())
                .addValue("reviewedAt", request.getReviewedAt() != null ? Timestamp.valueOf(request.getReviewedAt()) : null)
                .addValue("remarks", request.getRemarks())
                .addValue("eventRequestId", request.getEventRequestId());

        return jdbc.update(sql, params);
    }
}