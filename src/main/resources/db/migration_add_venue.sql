-- Migration: Add venue column to requests table and update active requests view
ALTER TABLE requests ADD COLUMN venue VARCHAR(255);

-- Recreate v_active_requests view to include venue
CREATE OR REPLACE VIEW v_active_requests AS
SELECT
    r.request_id,
    r.request_type,
    r.student_id,
    s.first_name || ' ' || s.last_name AS student_name,
    r.event_name,
    r.venue,
    r.organization,
    r.start_date,
    r.end_date,
    r.expected_ingress_time,
    r.expected_egress_time,
    r.status,
    COUNT(rd.request_device_id) AS device_count
FROM requests r
JOIN students s ON s.student_id = r.student_id
LEFT JOIN request_devices rd ON rd.request_id = r.request_id
WHERE r.status = 'approved'
GROUP BY r.request_id, s.first_name, s.last_name, r.venue;

COMMENT ON VIEW v_active_requests IS 'Active approved access requests in system.';
