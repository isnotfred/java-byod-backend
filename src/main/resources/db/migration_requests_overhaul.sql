-- ============================================================
-- BYOD Device Management System — Migration: Request-Based System Overhaul
-- ============================================================

-- ── 1. DROP OLD VIEWS ────────────────────────────────────────
DROP VIEW IF EXISTS v_device_campus_status CASCADE;
DROP VIEW IF EXISTS v_pending_devices CASCADE;
DROP VIEW IF EXISTS v_active_event_requests CASCADE;
DROP VIEW IF EXISTS v_event_device_status CASCADE;

-- ── 2. DROP OLD TRIGGERS & FUNCTIONS ─────────────────────────
DROP TRIGGER IF EXISTS trg_devices_updated_at ON devices;
DROP TRIGGER IF EXISTS trg_event_requests_updated_at ON event_requests;
DROP TRIGGER IF EXISTS trg_event_request_devices_updated_at ON event_request_devices;
DROP TRIGGER IF EXISTS trg_device_logs_force_created_at ON device_logs;
DROP TRIGGER IF EXISTS trg_event_device_logs_force_created_at ON event_device_logs;
DROP TRIGGER IF EXISTS trg_devices_registration_transition ON devices;
DROP TRIGGER IF EXISTS trg_device_logs_approved_only ON device_logs;
DROP TRIGGER IF EXISTS trg_device_logs_consecutive_events ON device_logs;
DROP TRIGGER IF EXISTS trg_event_device_logs_consecutive_events ON event_device_logs;
DROP TRIGGER IF EXISTS trg_protect_device_delete ON devices;

DROP FUNCTION IF EXISTS fn_guard_registration_transition();
DROP FUNCTION IF EXISTS fn_guard_device_log_approved_only();
DROP FUNCTION IF EXISTS fn_guard_consecutive_events();
DROP FUNCTION IF EXISTS fn_guard_consecutive_event_device_events();
DROP FUNCTION IF EXISTS fn_protect_device_delete();

-- ── 3. DROP OLD TABLES ───────────────────────────────────────
DROP TABLE IF EXISTS event_device_logs CASCADE;
DROP TABLE IF EXISTS device_logs CASCADE;
DROP TABLE IF EXISTS event_request_devices CASCADE;
DROP TABLE IF EXISTS event_requests CASCADE;
DROP TABLE IF EXISTS devices CASCADE;


-- ── 4. CREATE NEW TABLES ─────────────────────────────────────

-- Unified requests table (merges normal and event requests)
CREATE TABLE requests (
    request_id            SERIAL          PRIMARY KEY,
    request_type          VARCHAR(20)     NOT NULL, -- 'normal', 'event'
    student_id            VARCHAR(50)     NOT NULL,
    
    -- Event-specific fields (nullable for normal requests)
    event_name            VARCHAR(255),
    organization          VARCHAR(255),
    responsible_person    VARCHAR(255),
    approval_doc_type     VARCHAR(20),
    approval_doc_ref      VARCHAR(255),
    
    -- Purpose (applies to both, e.g., 'Academic BYOD' or 'Event Participation')
    purpose               VARCHAR(255)    NOT NULL,
    
    -- Range date and times
    start_date            DATE            NOT NULL,
    end_date              DATE            NOT NULL,
    expected_ingress_time TIME            NOT NULL,
    expected_egress_time  TIME            NOT NULL,
    
    -- Status and review
    status                VARCHAR(20)     NOT NULL DEFAULT 'pending', -- 'pending', 'approved', 'rejected', 'returned'
    is_submitted          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_accommodated       BOOLEAN         NOT NULL DEFAULT FALSE,
    reviewed_by           INT,
    reviewed_at           TIMESTAMPTZ,
    remarks               TEXT,
    
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (student_id)  REFERENCES students (student_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE,
    
    CONSTRAINT chk_requests_type CHECK (request_type IN ('normal', 'event')),
    CONSTRAINT chk_requests_status CHECK (status IN ('pending', 'approved', 'rejected', 'returned')),
    CONSTRAINT chk_requests_date_range CHECK (end_date >= start_date)
);

-- Individual devices listed under a request
CREATE TABLE request_devices (
    request_device_id     SERIAL          PRIMARY KEY,
    request_id            INT             NOT NULL,
    device_name           VARCHAR(255)    NOT NULL,
    brand                 VARCHAR(100),
    model                 VARCHAR(100),
    device_type           VARCHAR(50)     NOT NULL,
    serial_number         VARCHAR(255)    NOT NULL,
    quantity              INT             NOT NULL DEFAULT 1,
    image_path            VARCHAR(500), -- For normal requests to show photo of the device
    
    -- Verification status per device (guards or admins can verify)
    device_status         VARCHAR(20)     NOT NULL DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
    verified_by           INT,
    verified_at           TIMESTAMPTZ,
    remarks               TEXT,
    
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (request_id)  REFERENCES requests (request_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Prevent duplicate serial numbers within the SAME request, but allow serial reuse across requests over time
    UNIQUE (request_id, serial_number),
    
    CONSTRAINT chk_request_devices_status CHECK (device_status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT chk_request_devices_quantity CHECK (quantity > 0)
);

-- Daily ingress/egress transactions per device
CREATE TABLE device_transactions (
    transaction_id        SERIAL          PRIMARY KEY,
    request_device_id     INT             NOT NULL,
    log_date              DATE            NOT NULL,
    
    -- Ingress details
    ingress_time          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ingress_handled_by    INT             NOT NULL,
    
    -- Egress details (nullable until egress occurs)
    egress_time           TIMESTAMPTZ,
    egress_handled_by     INT,
    
    -- Missed egress marker (Requirement 7)
    no_egress_marked      BOOLEAN         NOT NULL DEFAULT FALSE,
    
    notes                 TEXT,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (request_device_id) REFERENCES request_devices (request_device_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (ingress_handled_by) REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (egress_handled_by)  REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Enforce Requirement 6: Max 1 ingress/egress transaction per device per day
    CONSTRAINT uq_device_transactions_date UNIQUE (request_device_id, log_date),
    
    -- Check constraints
    CONSTRAINT chk_device_transactions_times CHECK (egress_time IS NULL OR egress_time >= ingress_time),
    CONSTRAINT chk_device_transactions_egress_handled CHECK (
        (egress_time IS NULL AND egress_handled_by IS NULL)
        OR
        (egress_time IS NOT NULL AND egress_handled_by IS NOT NULL)
    )
);


-- ── 5. NEW TRIGGERS & FUNCTIONS ─────────────────────────────

-- Trigger: auto-refresh updated_at on UPDATE
CREATE TRIGGER trg_requests_updated_at
    BEFORE UPDATE ON requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_request_devices_updated_at
    BEFORE UPDATE ON request_devices
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_device_transactions_updated_at
    BEFORE UPDATE ON device_transactions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Trigger: force server-side created_at on INSERT
CREATE TRIGGER trg_device_transactions_force_created_at
    BEFORE INSERT ON device_transactions
    FOR EACH ROW EXECUTE FUNCTION fn_force_created_at();

-- Guard: restrict transactions to approved devices and approved requests only
CREATE OR REPLACE FUNCTION fn_guard_device_transaction_approved_only()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_req_status VARCHAR(20);
    v_dev_status VARCHAR(20);
BEGIN
    SELECT r.status, rd.device_status
    INTO   v_req_status, v_dev_status
    FROM   request_devices rd
    JOIN   requests r ON r.request_id = rd.request_id
    WHERE  rd.request_device_id = NEW.request_device_id;

    IF v_req_status <> 'approved' THEN
        RAISE EXCEPTION 'Parent request is not approved (status: %). Cannot log transaction.', v_req_status;
    END IF;

    IF v_dev_status <> 'approved' THEN
        RAISE EXCEPTION 'Device status is not approved (status: %). Cannot log transaction.', v_dev_status;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_device_transactions_approved_only
    BEFORE INSERT OR UPDATE ON device_transactions
    FOR EACH ROW EXECUTE FUNCTION fn_guard_device_transaction_approved_only();

-- Deletion protection: prevent deleting request devices if they have logs
CREATE OR REPLACE FUNCTION fn_protect_request_device_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_tx_count INT;
BEGIN
    SELECT COUNT(*) INTO v_tx_count FROM device_transactions WHERE request_device_id = OLD.request_device_id;
    IF v_tx_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete device. It has % gate transaction(s). Set device status to rejected or deactivate parent request instead.', v_tx_count;
    END IF;
    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_protect_request_device_delete
    BEFORE DELETE ON request_devices
    FOR EACH ROW EXECUTE FUNCTION fn_protect_request_device_delete();

-- Deletion protection: prevent deleting requests if they have active transactions
CREATE OR REPLACE FUNCTION fn_protect_request_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_tx_count INT;
BEGIN
    SELECT COUNT(*) INTO v_tx_count
    FROM device_transactions dt
    JOIN request_devices rd ON rd.request_device_id = dt.request_device_id
    WHERE rd.request_id = OLD.request_id;

    IF v_tx_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete request. It has % associated gate transaction(s). Archive or reject the request instead.', v_tx_count;
    END IF;
    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_protect_request_delete
    BEFORE DELETE ON requests
    FOR EACH ROW EXECUTE FUNCTION fn_protect_request_delete();

-- Deletion protection: update student deletion protection to look at requests
CREATE OR REPLACE FUNCTION fn_protect_student_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_req_count INT;
    v_tx_count  INT;
BEGIN
    SELECT COUNT(*) INTO v_req_count FROM requests WHERE student_id = OLD.student_id;
    
    SELECT COUNT(*) INTO v_tx_count 
    FROM device_transactions dt
    JOIN request_devices rd ON rd.request_device_id = dt.request_device_id
    JOIN requests r ON r.request_id = rd.request_id
    WHERE r.student_id = OLD.student_id;

    IF v_req_count > 0 OR v_tx_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete student %. They have % request(s) and % transaction(s). Set status to inactive instead.', OLD.student_id, v_req_count, v_tx_count;
    END IF;
    RETURN OLD;
END;
$$;


-- ── 6. CREATE NEW VIEWS ──────────────────────────────────────

-- View: current campus status per request device
-- 'entry' = checked in and has not exited
-- 'exit'  = checked out, or has no record, or missed check-out (marked no egress)
CREATE OR REPLACE VIEW v_device_campus_status AS
SELECT
    rd.request_device_id,
    rd.request_id,
    r.student_id,
    rd.device_name,
    rd.serial_number,
    rd.brand,
    rd.model,
    rd.device_type,
    rd.device_status,
    r.request_type,
    COALESCE(t.status, 'exit') AS campus_status,
    t.event_time AS last_event_time,
    COALESCE(t.no_egress_marked, FALSE) AS no_egress_marked
FROM request_devices rd
JOIN requests r ON r.request_id = rd.request_id
LEFT JOIN LATERAL (
    SELECT 
        CASE 
            WHEN dt.egress_time IS NULL AND dt.no_egress_marked = FALSE THEN 'entry'
            ELSE 'exit'
        END AS status,
        COALESCE(dt.egress_time, dt.ingress_time) AS event_time,
        dt.no_egress_marked
    FROM device_transactions dt
    WHERE dt.request_device_id = rd.request_device_id
    ORDER BY dt.log_date DESC, dt.ingress_time DESC
    LIMIT 1
) t ON TRUE
WHERE rd.device_status = 'approved'
  AND r.status = 'approved';



-- View: active/approved requests
CREATE OR REPLACE VIEW v_active_requests AS
SELECT
    r.request_id,
    r.request_type,
    r.student_id,
    s.first_name || ' ' || s.last_name AS student_name,
    r.event_name,
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
GROUP BY r.request_id, s.first_name, s.last_name;


-- ── 7. AUTOVACUUM TUNING ─────────────────────────────────────
ALTER TABLE device_transactions SET (
    autovacuum_vacuum_scale_factor  = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);


-- ── 8. COMMENTS ──────────────────────────────────────────────
COMMENT ON TABLE  requests                       IS 'Unified header for individual (normal) and group (event) device requests.';
COMMENT ON COLUMN requests.request_type          IS 'normal or event.';
COMMENT ON COLUMN requests.status                IS 'pending, approved, rejected, returned.';

COMMENT ON TABLE  request_devices                IS 'Individual device specifications attached to a request.';
COMMENT ON COLUMN request_devices.device_status  IS 'pending, approved, rejected.';

COMMENT ON TABLE  device_transactions            IS 'Daily ingress/egress transactions. Max 1 transaction per day per device.';
COMMENT ON COLUMN device_transactions.no_egress_marked IS 'TRUE = student exited campus without scanning out on this day.';

COMMENT ON VIEW   v_device_campus_status         IS 'Real-time campus presence state derived from the latest daily transaction.';

COMMENT ON VIEW   v_active_requests              IS 'Active approved access requests in system.';

-- ── 9. UPDATE AUDIT LOG ACTIONS CONSTRAINT ───────────────────
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action_type_known;

ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_action_type_known CHECK (action_type IN (
    'DEVICE_REGISTERED',
    'DEVICE_APPROVED',
    'DEVICE_REJECTED',
    'DEVICE_DEACTIVATED',
    'DEVICE_UPDATED',
    'DEVICE_ENTRY',
    'DEVICE_EXIT',
    'DEVICE_AUTO_EXIT',
    'STUDENT_CREATED',
    'STUDENT_UPDATED',
    'STUDENT_DEACTIVATED',
    'USER_CREATED',
    'USER_UPDATED',
    'USER_DEACTIVATED',
    'USER_LOGIN',
    'USER_LOGOUT',
    'USER_LOGIN_FAILED',
    'EVENT_REQUEST_CREATED',
    'EVENT_REQUEST_APPROVED',
    'EVENT_REQUEST_RETURNED',
    'EVENT_REQUEST_REJECTED',
    'SYSTEM_AUTO_EXIT_BATCH',
    'ADMIN_CREATED',
    'ADMIN_UPDATED',
    'ADMIN_DEACTIVATED',
    'GUARD_CREATED',
    'GUARD_UPDATED',
    'GUARD_DEACTIVATED_BY_SUPER',
    'USER_ROLE_CHANGED',
    'SYSTEM_CONFIG_UPDATED',
    
    -- New request overhaul actions
    'REQUEST_CREATED',
    'REQUEST_APPROVED',
    'REQUEST_REJECTED',
    'REQUEST_RETURNED',
    'REQUEST_UPDATED',
    'DEVICE_VERIFIED',
    'DEVICE_CHECK_IN',
    'DEVICE_CHECK_OUT',
    'MISSED_EGRESS_BATCH'
));

