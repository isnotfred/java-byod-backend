-- ============================================================
-- BYOD Device Management System
-- PostgreSQL Schema — JavaFX Desktop Application
-- ============================================================


-- ============================================================
-- SECTION 1: TABLES
-- ============================================================

-- ── users ────────────────────────────────────────────────────
-- Admin and guard accounts. No student logins.

CREATE TABLE users (
    user_id                   SERIAL          PRIMARY KEY,
    username                  VARCHAR(100)    NOT NULL UNIQUE,
    email                     VARCHAR(255)    UNIQUE,
    password_hash             TEXT            NOT NULL,
    full_name                 VARCHAR(255),
    role                      VARCHAR(20)     NOT NULL,
    status                    VARCHAR(10)     NOT NULL DEFAULT 'active',
    password_reset_token      VARCHAR(255)    UNIQUE,
    password_reset_expires_at TIMESTAMPTZ,
    created_at                TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ── students ─────────────────────────────────────────────────
-- Student registry. Never hard-delete; set status = 'inactive'.

CREATE TABLE students (
    student_id        VARCHAR(50)   PRIMARY KEY,
    first_name        VARCHAR(100)  NOT NULL,
    last_name         VARCHAR(100)  NOT NULL,
    course_year_level VARCHAR(100),
    contact_number    VARCHAR(20),
    status            VARCHAR(10)   NOT NULL DEFAULT 'active',
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ── requests ─────────────────────────────────────────────────
-- Unified header for individual (normal) and group (event) device requests.
-- Consolidates old event requests and permanent device bypass registrations.

CREATE TABLE requests (
    request_id            SERIAL          PRIMARY KEY,
    request_type          VARCHAR(20)     NOT NULL, -- 'normal', 'event'
    student_id            VARCHAR(50)     NOT NULL,
    
    -- Event-specific fields (nullable for normal requests)
    event_name            VARCHAR(255),
    organization          VARCHAR(255),
    responsible_person    VARCHAR(255),
    
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
    FOREIGN KEY (reviewed_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE
);


-- ── request_devices ──────────────────────────────────────────
-- Individual devices attached to a request.
-- Replaces old devices and event_request_devices tables.

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
    UNIQUE (request_id, serial_number)
);


-- ── device_transactions ──────────────────────────────────────
-- Daily ingress/egress transactions per device.
-- Replaces old device_logs and event_device_logs tables.
-- Rows are updated only to log egress_time or to mark no_egress_marked.

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
    CONSTRAINT uq_device_transactions_date UNIQUE (request_device_id, log_date)
);


-- ── audit_logs ───────────────────────────────────────────────
-- Immutable system-wide audit trail.
-- Write via fn_write_audit_log() only — never INSERT directly.

CREATE TABLE audit_logs (
    audit_id     SERIAL          PRIMARY KEY,
    user_id      INT,
    action_type  VARCHAR(100)    NOT NULL,
    target_table VARCHAR(100)    NOT NULL,
    target_id    VARCHAR(100),
    old_values   JSONB,
    new_values   JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- SET NULL: keep audit history even if the user account is deleted
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL ON UPDATE CASCADE
);


-- ── system_settings ─────────────────────────────────────────
-- System settings and policy parameters.

CREATE TABLE system_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   TEXT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- SECTION 2: INDEXES
-- ============================================================

-- students
CREATE INDEX idx_students_name             ON students (last_name, first_name);
CREATE INDEX idx_students_status           ON students (status);

-- requests
CREATE INDEX idx_requests_student          ON requests (student_id);
CREATE INDEX idx_requests_status           ON requests (status);
CREATE INDEX idx_requests_dates            ON requests (start_date, end_date);
CREATE INDEX idx_requests_type             ON requests (request_type);

-- request_devices
CREATE INDEX idx_request_devices_req       ON request_devices (request_id);
CREATE INDEX idx_request_devices_serial    ON request_devices (serial_number);
CREATE INDEX idx_request_devices_status    ON request_devices (device_status);

-- device_transactions
CREATE INDEX idx_device_transactions_dev   ON device_transactions (request_device_id);
CREATE INDEX idx_device_transactions_date  ON device_transactions (log_date);
CREATE INDEX idx_device_transactions_state ON device_transactions (request_device_id, log_date, egress_time) 
    WHERE egress_time IS NULL AND no_egress_marked = FALSE;

-- audit_logs
CREATE INDEX idx_audit_logs_user_time      ON audit_logs (user_id, created_at DESC);
CREATE INDEX idx_audit_logs_target         ON audit_logs (target_table, target_id);
CREATE INDEX idx_audit_logs_created_at     ON audit_logs (created_at DESC);


-- ============================================================
-- SECTION 3: CHECK CONSTRAINTS
-- ============================================================

-- ── users ────────────────────────────────────────────────────
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('admin', 'guard', 'super_admin')),
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('active', 'inactive', 'pending')),
    ADD CONSTRAINT chk_users_username_length
        CHECK (char_length(username) >= 3),
    -- Minimum 20 chars ensures no plaintext password was stored.
    -- bcrypt = 60 chars, argon2 = longer.
    ADD CONSTRAINT chk_users_password_hash_length
        CHECK (char_length(password_hash) >= 20);


-- ── students ─────────────────────────────────────────────────
ALTER TABLE students
    ADD CONSTRAINT chk_students_status
        CHECK (status IN ('active', 'inactive')),
    ADD CONSTRAINT chk_students_id_nonempty
        CHECK (char_length(trim(student_id)) > 0),
    ADD CONSTRAINT chk_students_first_name_nonempty
        CHECK (char_length(trim(first_name)) > 0),
    ADD CONSTRAINT chk_students_last_name_nonempty
        CHECK (char_length(trim(last_name)) > 0);


-- ── requests ─────────────────────────────────────────────────
ALTER TABLE requests
    ADD CONSTRAINT chk_requests_type
        CHECK (request_type IN ('normal', 'event')),
    ADD CONSTRAINT chk_requests_status
        CHECK (status IN ('pending', 'approved', 'rejected', 'returned')),
    -- End date must be on or after start date
    ADD CONSTRAINT chk_requests_date_range
        CHECK (end_date >= start_date),
    ADD CONSTRAINT chk_requests_review_consistency
        CHECK (
            (reviewed_by IS NULL AND reviewed_at IS NULL)
            OR
            (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        );


-- ── request_devices ──────────────────────────────────────────
ALTER TABLE request_devices
    ADD CONSTRAINT chk_request_devices_quantity
        CHECK (quantity > 0),
    ADD CONSTRAINT chk_request_devices_type
        CHECK (device_type IN (
            'Personal Computers',
            'Components & Peripherals',
            'Display & Projection',
            'Project Prototypes (Optional SN)',
            'Appliances (TLE)',
            'Other'
        )),
    ADD CONSTRAINT chk_request_devices_status
        CHECK (device_status IN ('pending', 'approved', 'rejected')),
    ADD CONSTRAINT chk_request_devices_review_consistency
        CHECK (
            (verified_by IS NULL AND verified_at IS NULL)
            OR
            (verified_by IS NOT NULL AND verified_at IS NOT NULL)
        );


-- ── device_transactions ──────────────────────────────────────
ALTER TABLE device_transactions
    ADD CONSTRAINT chk_device_transactions_times 
        CHECK (egress_time IS NULL OR egress_time >= ingress_time),
    ADD CONSTRAINT chk_device_transactions_egress_handled 
        CHECK (
            (egress_time IS NULL AND egress_handled_by IS NULL)
            OR
            (egress_time IS NOT NULL AND egress_handled_by IS NOT NULL)
        );


-- ── audit_logs ───────────────────────────────────────────────
ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_action_type_nonempty
        CHECK (char_length(trim(action_type)) > 0),
    ADD CONSTRAINT chk_audit_logs_target_table_nonempty
        CHECK (char_length(trim(target_table)) > 0),
    ADD CONSTRAINT chk_audit_logs_ip_length
        CHECK (ip_address IS NULL OR char_length(ip_address) BETWEEN 7 AND 45),
    ADD CONSTRAINT chk_audit_logs_action_type_known
        CHECK (action_type IN (
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
            'DEVICE_VERIFIED',
            'DEVICE_CHECK_IN',
            'DEVICE_CHECK_OUT',
            'MISSED_EGRESS_BATCH'
        ));


-- ============================================================
-- SECTION 4: FUNCTIONS & TRIGGERS
-- ============================================================

-- ── 4.1 Auto-refresh updated_at on every UPDATE ──────────────

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_students_updated_at
    BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_requests_updated_at
    BEFORE UPDATE ON requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_request_devices_updated_at
    BEFORE UPDATE ON request_devices
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_device_transactions_updated_at
    BEFORE UPDATE ON device_transactions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_system_settings_updated_at
    BEFORE UPDATE ON system_settings
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ── 4.2 Force server-side created_at (prevent backdating) ────

CREATE OR REPLACE FUNCTION fn_force_created_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.created_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_device_transactions_force_created_at
    BEFORE INSERT ON device_transactions
    FOR EACH ROW EXECUTE FUNCTION fn_force_created_at();

CREATE TRIGGER trg_audit_logs_force_created_at
    BEFORE INSERT ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION fn_force_created_at();


-- ── 4.3 Block gate transactions on unapproved devices/requests ────

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


-- ── 4.4 Immutable audit_logs ──────────────────────────────────

CREATE OR REPLACE FUNCTION fn_audit_log_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs rows are immutable. UPDATE and DELETE are not permitted.';
END;
$$;

CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION fn_audit_log_immutable();

CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION fn_audit_log_immutable();


-- ── 4.5 Deletion protection: request_devices ──────────────────

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


-- ── 4.6 Deletion protection: requests ─────────────────────────

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


-- ── 4.7 Deletion protection: students ────────────────────────

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

CREATE TRIGGER trg_protect_student_delete
    BEFORE DELETE ON students
    FOR EACH ROW EXECUTE FUNCTION fn_protect_student_delete();


-- ── 4.8 Deletion protection: users ──────────────────────────

CREATE OR REPLACE FUNCTION fn_protect_user_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_audit_count INT;
BEGIN
    SELECT COUNT(*) INTO v_audit_count FROM audit_logs WHERE user_id = OLD.user_id;

    IF v_audit_count > 0 THEN
        RAISE EXCEPTION
            'Cannot delete user %. They have % audit log entries. '
            'Set status to ''inactive'' instead.',
            OLD.user_id, v_audit_count;
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_protect_user_delete
    BEFORE DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_protect_user_delete();


-- ── 4.9 Secure audit log writer ─────────────────────────────
-- Call this from Java instead of INSERT-ing into audit_logs directly.

CREATE OR REPLACE FUNCTION fn_write_audit_log(
    p_user_id      INT,
    p_action_type  VARCHAR(100),
    p_target_table VARCHAR(100),
    p_target_id    VARCHAR(100),
    p_old_values   JSONB,
    p_new_values   JSONB,
    p_ip_address   VARCHAR(45)
)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO audit_logs (
        user_id, action_type, target_table, target_id,
        old_values, new_values, ip_address
    ) VALUES (
        p_user_id, p_action_type, p_target_table, p_target_id,
        p_old_values, p_new_values, p_ip_address
    );
END;
$$;


-- ============================================================
-- SECTION 5: VIEWS
-- ============================================================

-- Current campus status per approved active device.
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





-- Active / Approved requests
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


-- ============================================================
-- SECTION 6: AUTOVACUUM TUNING
-- ============================================================
-- device_transactions and audit_logs receive the highest INSERT/UPDATE rate.

ALTER TABLE device_transactions SET (
    autovacuum_vacuum_scale_factor  = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);

ALTER TABLE audit_logs SET (
    autovacuum_vacuum_scale_factor  = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);


-- ============================================================
-- SECTION 7: COMMENTS
-- ============================================================

COMMENT ON TABLE  users                       IS 'Admin and guard accounts. No student logins.';
COMMENT ON COLUMN users.role                  IS 'admin, guard, or super_admin.';
COMMENT ON COLUMN users.password_hash         IS 'Store bcrypt or argon2 hash only. Never plaintext.';
COMMENT ON COLUMN users.status                IS 'active or inactive. Never hard-delete a user.';

COMMENT ON TABLE  students                    IS 'Registered students. Never hard-delete; set status = inactive.';

COMMENT ON TABLE  requests                    IS 'Unified header for individual (normal) and group (event) device requests.';
COMMENT ON COLUMN requests.request_type          IS 'normal or event.';
COMMENT ON COLUMN requests.status                IS 'pending, approved, rejected, returned.';

COMMENT ON TABLE  request_devices                IS 'Individual device specifications attached to a request.';
COMMENT ON COLUMN request_devices.device_status  IS 'pending, approved, rejected.';

COMMENT ON TABLE  device_transactions            IS 'Daily ingress/egress transactions. Max 1 transaction per day per device.';
COMMENT ON COLUMN device_transactions.no_egress_marked IS 'TRUE = student exited campus without scanning out on this day.';

COMMENT ON TABLE  audit_logs                  IS 'Immutable audit trail. Write via fn_write_audit_log() only.';

COMMENT ON VIEW   v_device_campus_status         IS 'Real-time campus presence state derived from the latest daily transaction.';

COMMENT ON VIEW   v_active_requests              IS 'Active approved access requests in system.';

COMMENT ON FUNCTION fn_write_audit_log        IS 'Preferred way to write to audit_logs from Java. Keeps inserts consistent.';
COMMENT ON FUNCTION fn_set_updated_at         IS 'Auto-refreshes updated_at on every UPDATE.';

COMMENT ON TABLE  system_settings             IS 'System settings and policy parameters.';


-- ============================================================
-- SECTION 8: SEED DATA (SYSTEM SETTINGS)
-- ============================================================

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('max_devices_per_student', '5', 'Maximum number of active registered devices allowed per student'),
('event_request_max_duration_days', '7', 'Maximum duration in days for an event request');


-- ============================================================
-- END OF SCHEMA
-- ============================================================