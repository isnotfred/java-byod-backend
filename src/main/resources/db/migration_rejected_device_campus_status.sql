-- ============================================================
-- Update v_device_campus_status view to allow egress for rejected devices
-- ============================================================

DROP VIEW IF EXISTS v_device_campus_status;

CREATE VIEW v_device_campus_status AS
SELECT
    d.device_id,
    d.student_id,
    d.device_name,
    d.serial_number,
    d.brand,
    d.model,
    d.device_type,
    d.registration_status,
    d.device_status,
    COALESCE(last_log.event_type, 'exit') AS campus_status,
    last_log.event_time                       AS last_event_time
FROM devices d
         LEFT JOIN LATERAL (
    SELECT event_type, event_time
    FROM   device_logs
    WHERE  device_id = d.device_id
    ORDER  BY event_time DESC
        LIMIT  1
    ) last_log ON TRUE
WHERE d.device_status = 'active'
  AND (
    d.registration_status IN ('approved', 'pending')
    OR (d.registration_status = 'rejected' AND last_log.event_type = 'entry')
  );

-- ============================================================
-- Update trigger function to allow exit logging for rejected devices
-- ============================================================

CREATE OR REPLACE FUNCTION fn_guard_device_log_approved_only()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_reg_status VARCHAR(10);
    v_dev_status VARCHAR(10);
BEGIN
    SELECT registration_status, device_status
    INTO   v_reg_status, v_dev_status
    FROM   devices
    WHERE  device_id = NEW.device_id;

    IF v_reg_status NOT IN ('approved', 'pending') AND NOT (v_reg_status = 'rejected' AND NEW.event_type = 'exit') THEN
        RAISE EXCEPTION
            'Device % is not approved or pending (status: ''%''). Cannot log entry/exit.',
            NEW.device_id, v_reg_status;
    END IF;

    IF v_dev_status = 'inactive' THEN
        RAISE EXCEPTION
            'Device % is inactive and cannot be logged.',
            NEW.device_id;
    END IF;

    RETURN NEW;
END;
$$;
