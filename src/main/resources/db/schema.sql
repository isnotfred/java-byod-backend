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
    status            VARCHAR(10)   NOT NULL DEFAULT 'active',
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ── devices ──────────────────────────────────────────────────
-- Permanent BYOD registered devices.
-- Whether a device is currently inside/outside campus is
-- derived from device_logs — not stored here.

CREATE TABLE devices (
    device_id           SERIAL          PRIMARY KEY,
    student_id          VARCHAR(50)     NOT NULL,
    device_name         VARCHAR(255),
    brand               VARCHAR(100),
    model               VARCHAR(100),
    serial_number       VARCHAR(255)    NOT NULL UNIQUE,
    device_type         VARCHAR(50),
    device_purpose      VARCHAR(100),
    registration_status VARCHAR(10)     NOT NULL DEFAULT 'pending',
    device_status       VARCHAR(10)     NOT NULL DEFAULT 'active',
    reviewed_by         INT,
    reviewed_at         TIMESTAMPTZ,
    remarks             TEXT,
    image_path          VARCHAR(500),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (student_id)  REFERENCES students (student_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE
);


-- ── event_requests ───────────────────────────────────────────
-- Header for a temporary device access request
-- (school events, org activities, etc.).

CREATE TABLE event_requests (
    event_request_id  SERIAL          PRIMARY KEY,
    student_id        VARCHAR(50)     NOT NULL,
    responsible_person VARCHAR(255),
    organization      VARCHAR(255),
    event_name        VARCHAR(255)    NOT NULL,
    event_purpose     VARCHAR(255),
    approval_doc_type VARCHAR(20),
    approval_doc_ref  VARCHAR(255),
    start_date        DATE,
    end_date          DATE,
    status            VARCHAR(10)     NOT NULL DEFAULT 'pending',
    is_submitted      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_accommodated   BOOLEAN         NOT NULL DEFAULT FALSE,
    reviewed_by       INT,
    reviewed_at       TIMESTAMPTZ,
    remarks           TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (student_id)  REFERENCES students (student_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE
);


-- ── event_request_devices ────────────────────────────────────
-- Individual devices listed under an event request.

CREATE TABLE event_request_devices (
    event_device_id  SERIAL         PRIMARY KEY,
    event_request_id INT            NOT NULL,
    device_name      VARCHAR(255),
    brand            VARCHAR(100),
    model            VARCHAR(100),
    device_type      VARCHAR(50),
    serial_number    VARCHAR(255),
    quantity         INT            NOT NULL DEFAULT 1,
    verified_by      INT,
    verified_at      TIMESTAMPTZ,
    device_status    VARCHAR(10)    NOT NULL DEFAULT 'pending',
    remarks          TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (event_request_id, serial_number),

    FOREIGN KEY (event_request_id) REFERENCES event_requests (event_request_id) ON DELETE CASCADE  ON UPDATE CASCADE,
    FOREIGN KEY (verified_by)      REFERENCES users           (user_id)          ON DELETE RESTRICT ON UPDATE CASCADE
);


-- ── device_logs ──────────────────────────────────────────────
-- Immutable gate event log. One row per entry or exit scan.
-- Rows are never updated or deleted (enforced by trigger).

CREATE TABLE device_logs (
    log_id       SERIAL         PRIMARY KEY,
    device_id    INT            NOT NULL,
    student_id   VARCHAR(50)    NOT NULL,
    event_type   VARCHAR(10)    NOT NULL,
    event_time   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_by   INT,
    logout_type  VARCHAR(10),
    auto_exit    BOOLEAN        NOT NULL DEFAULT FALSE,
    notes        TEXT,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (device_id)  REFERENCES devices  (device_id)  ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (handled_by) REFERENCES users    (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE
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
CREATE INDEX idx_students_name   ON students (last_name, first_name);
CREATE INDEX idx_students_status ON students (status);

-- devices
CREATE INDEX idx_devices_student            ON devices (student_id);
CREATE INDEX idx_devices_serial             ON devices (serial_number);
CREATE INDEX idx_devices_registration       ON devices (registration_status);
CREATE INDEX idx_devices_student_status     ON devices (student_id, registration_status, device_status);
-- Partial index: admin approval queue (only indexes pending rows)
CREATE INDEX idx_devices_pending_queue      ON devices (created_at DESC)
    WHERE registration_status = 'pending';

-- event_requests
CREATE INDEX idx_event_requests_student     ON event_requests (student_id);
CREATE INDEX idx_event_requests_status      ON event_requests (status);
CREATE INDEX idx_event_requests_dates       ON event_requests (start_date, end_date);

-- event_request_devices
CREATE INDEX idx_event_request_devices_req  ON event_request_devices (event_request_id);

-- device_logs
-- Hottest query: last event per device (runs on every gate scan)
CREATE INDEX idx_device_logs_last_event     ON device_logs (device_id, event_time DESC) INCLUDE (event_type);
CREATE INDEX idx_device_logs_student        ON device_logs (student_id);
CREATE INDEX idx_device_logs_event_type     ON device_logs (event_type);
-- Nightly auto-exit batch: find all devices still inside
CREATE INDEX idx_device_logs_open_entries   ON device_logs (event_type, event_time DESC)
    INCLUDE (device_id, student_id)
    WHERE event_type = 'entry';

-- audit_logs
CREATE INDEX idx_audit_logs_user_time       ON audit_logs (user_id, created_at DESC);
CREATE INDEX idx_audit_logs_target          ON audit_logs (target_table, target_id);
CREATE INDEX idx_audit_logs_created_at      ON audit_logs (created_at DESC);


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


-- ── devices ──────────────────────────────────────────────────
ALTER TABLE devices
    ADD CONSTRAINT chk_devices_type
        CHECK (device_type IN (
            'Personal Computers',
            'Components & Peripherals',
            'Display & Projection',
            'Project Prototypes (Optional SN)',
            'Appliances (TLE)'
        )),
    ADD CONSTRAINT chk_devices_purpose
        CHECK (device_purpose IN (
            'Academic BYOD',
            'School Event',
            'Organization Activity',
            'Temporary Equipment',
            'Other Approved Purpose',
            'PROTOTYPE',
            'APPLIANCE'
        )),
    ADD CONSTRAINT chk_devices_registration_status
        CHECK (registration_status IN ('pending', 'approved', 'rejected')),
    ADD CONSTRAINT chk_devices_device_status
        CHECK (device_status IN ('active', 'inactive')),
    -- reviewed_by and reviewed_at must appear together
    ADD CONSTRAINT chk_devices_review_consistency
        CHECK (
            (reviewed_by IS NULL AND reviewed_at IS NULL)
            OR
            (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        ),
    -- A rejected device must always have a remark
    ADD CONSTRAINT chk_devices_rejection_requires_remark
        CHECK (
            registration_status <> 'rejected'
            OR (remarks IS NOT NULL AND char_length(trim(remarks)) > 0)
        );


-- ── event_requests ───────────────────────────────────────────
ALTER TABLE event_requests
    ADD CONSTRAINT chk_event_requests_status
        CHECK (status IN ('pending', 'approved', 'returned', 'rejected')),
    ADD CONSTRAINT chk_event_requests_approval_doc_type
        CHECK (approval_doc_type IN ('Paper Approval', 'Signed GPOA')),
    ADD CONSTRAINT chk_event_requests_event_name_nonempty
        CHECK (char_length(trim(event_name)) > 0),
    -- End date must be on or after start date
    ADD CONSTRAINT chk_event_requests_date_range
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    ADD CONSTRAINT chk_event_requests_review_consistency
        CHECK (
            (reviewed_by IS NULL AND reviewed_at IS NULL)
            OR
            (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        );


-- ── event_request_devices ────────────────────────────────────
ALTER TABLE event_request_devices
    ADD CONSTRAINT chk_event_request_devices_quantity
        CHECK (quantity > 0),
    ADD CONSTRAINT chk_event_request_devices_type
        CHECK (device_type IN (
            'Personal Computers',
            'Components & Peripherals',
            'Display & Projection',
            'Project Prototypes (Optional SN)',
            'Appliances (TLE)',
            'Other'
        )),
    ADD CONSTRAINT chk_event_request_devices_status
        CHECK (device_status IN ('pending', 'approved', 'returned'));


-- ── device_logs ──────────────────────────────────────────────
ALTER TABLE device_logs
    ADD CONSTRAINT chk_device_logs_event_type
        CHECK (event_type IN ('entry', 'exit')),
    ADD CONSTRAINT chk_device_logs_logout_type
        CHECK (logout_type IN ('manual', 'automatic')),
    -- Auto-exit rows are system-generated; no human actor
    ADD CONSTRAINT chk_device_logs_auto_exit_consistency
        CHECK (
            (auto_exit = TRUE  AND handled_by IS NULL)
            OR
            (auto_exit = FALSE AND handled_by IS NOT NULL)
        ),
    -- Auto-exits are always exits, never entries
    ADD CONSTRAINT chk_device_logs_auto_exit_is_exit
        CHECK (auto_exit = FALSE OR event_type = 'exit'),
    ADD CONSTRAINT chk_device_logs_logout_type_consistency
        CHECK (
            (auto_exit = TRUE  AND logout_type = 'automatic')
            OR
            (auto_exit = FALSE AND logout_type = 'manual')
            OR logout_type IS NULL
        );


-- ── audit_logs ───────────────────────────────────────────────
ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_action_type_nonempty
        CHECK (char_length(trim(action_type)) > 0),
    ADD CONSTRAINT chk_audit_logs_target_table_nonempty
        CHECK (char_length(trim(target_table)) > 0),
    ADD CONSTRAINT chk_audit_logs_ip_length
        CHECK (ip_address IS NULL OR char_length(ip_address) BETWEEN 7 AND 45),
    -- Standardised vocabulary prevents free-text inconsistencies
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
            'SYSTEM_CONFIG_UPDATED'
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

CREATE TRIGGER trg_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_event_requests_updated_at
    BEFORE UPDATE ON event_requests
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_event_request_devices_updated_at
    BEFORE UPDATE ON event_request_devices
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

CREATE TRIGGER trg_device_logs_force_created_at
    BEFORE INSERT ON device_logs
    FOR EACH ROW EXECUTE FUNCTION fn_force_created_at();

CREATE TRIGGER trg_audit_logs_force_created_at
    BEFORE INSERT ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION fn_force_created_at();


-- ── 4.3 Guard registration_status state machine ──────────────
-- Allowed:  pending  → approved
--           pending  → rejected
--           rejected → pending   (re-submission)
-- Denied:   approved → rejected  (deactivate instead)
--           rejected → approved  (must go through pending)

CREATE OR REPLACE FUNCTION fn_guard_registration_transition()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.registration_status = 'approved'
       AND NEW.registration_status = 'rejected' THEN
        RAISE EXCEPTION
            'Cannot go directly from approved to rejected. '
            'Set device_status to inactive instead.';
END IF;

    IF OLD.registration_status = 'rejected'
       AND NEW.registration_status = 'approved' THEN
        RAISE EXCEPTION
            'Cannot go directly from rejected to approved. '
            'Reset to pending first.';
END IF;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_devices_registration_transition
    BEFORE UPDATE OF registration_status ON devices
    FOR EACH ROW EXECUTE FUNCTION fn_guard_registration_transition();


-- ── 4.4 Block gate log on unapproved or inactive devices ─────

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

IF v_reg_status <> 'approved' THEN
        RAISE EXCEPTION
            'Device % is not approved (status: ''%''). Cannot log entry/exit.',
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

CREATE TRIGGER trg_device_logs_approved_only
    BEFORE INSERT ON device_logs
    FOR EACH ROW EXECUTE FUNCTION fn_guard_device_log_approved_only();


-- ── 4.5 Block consecutive same-type events ───────────────────
-- Two consecutive 'entry' rows = missed exit scan.
-- Auto-exit rows are exempt (they are reconciliation rows).

CREATE OR REPLACE FUNCTION fn_guard_consecutive_events()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
v_last_event VARCHAR(10);
BEGIN
    IF NEW.auto_exit = TRUE THEN
        RETURN NEW;
END IF;

SELECT event_type
INTO   v_last_event
FROM   device_logs
WHERE  device_id = NEW.device_id
ORDER  BY event_time DESC
    LIMIT  1;

IF v_last_event IS NOT NULL AND v_last_event = NEW.event_type THEN
        RAISE EXCEPTION
            'Device % already has a consecutive ''%'' event. '
            'Log the opposite event first or reconcile.',
            NEW.device_id, NEW.event_type;
END IF;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_device_logs_consecutive_events
    BEFORE INSERT ON device_logs
    FOR EACH ROW EXECUTE FUNCTION fn_guard_consecutive_events();


-- ── 4.6 Immutable audit_logs ──────────────────────────────────

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


-- ── 4.7 Immutable device_logs ─────────────────────────────────

CREATE OR REPLACE FUNCTION fn_device_log_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'device_logs rows are immutable. Hard-delete is not permitted.';
END;
$$;

CREATE TRIGGER trg_device_log_no_delete
    BEFORE DELETE ON device_logs
    FOR EACH ROW EXECUTE FUNCTION fn_device_log_immutable();


-- ── 4.8 Deletion protection: students ────────────────────────

CREATE OR REPLACE FUNCTION fn_protect_student_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
v_device_count INT;
    v_log_count    INT;
BEGIN
SELECT COUNT(*) INTO v_device_count FROM devices     WHERE student_id = OLD.student_id;
SELECT COUNT(*) INTO v_log_count    FROM device_logs WHERE student_id = OLD.student_id;

IF v_device_count > 0 OR v_log_count > 0 THEN
        RAISE EXCEPTION
            'Cannot delete student ''%''. They have % device(s) and % log entry(ies). '
            'Set status to ''inactive'' instead.',
            OLD.student_id, v_device_count, v_log_count;
END IF;

RETURN OLD;
END;
$$;

CREATE TRIGGER trg_protect_student_delete
    BEFORE DELETE ON students
    FOR EACH ROW EXECUTE FUNCTION fn_protect_student_delete();


-- ── 4.9 Deletion protection: devices ─────────────────────────

CREATE OR REPLACE FUNCTION fn_protect_device_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
v_log_count INT;
BEGIN
SELECT COUNT(*) INTO v_log_count FROM device_logs WHERE device_id = OLD.device_id;

IF v_log_count > 0 THEN
        RAISE EXCEPTION
            'Cannot delete device %. It has % log entry(ies). '
            'Set device_status to ''inactive'' instead.',
            OLD.device_id, v_log_count;
END IF;

RETURN OLD;
END;
$$;

CREATE TRIGGER trg_protect_device_delete
    BEFORE DELETE ON devices
    FOR EACH ROW EXECUTE FUNCTION fn_protect_device_delete();


-- ── 4.10 Deletion protection: users ──────────────────────────

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


-- ── 4.11 Secure audit log writer ─────────────────────────────
-- Call this from Java instead of INSERT-ing into audit_logs directly.
-- Keeps audit writes consistent and prevents accidental schema mismatches.

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
-- 'inside'  = last log event was 'entry'
-- 'outside' = last log event was 'exit', or no log exists yet
CREATE OR REPLACE VIEW v_device_campus_status AS
SELECT
    d.device_id,
    d.student_id,
    d.device_name,
    d.serial_number,
    d.registration_status,
    d.device_status,
    COALESCE(last_log.event_type, 'outside') AS campus_status,
    last_log.event_time                       AS last_event_time
FROM devices d
         LEFT JOIN LATERAL (
    SELECT event_type, event_time
    FROM   device_logs
    WHERE  device_id = d.device_id
    ORDER  BY event_time DESC
        LIMIT  1
    ) last_log ON TRUE
WHERE d.registration_status = 'approved'
  AND d.device_status       = 'active';


-- Admin approval queue: pending devices with student name.
CREATE OR REPLACE VIEW v_pending_devices AS
SELECT
    d.device_id,
    d.student_id,
    s.first_name || ' ' || s.last_name AS student_name,
    s.course_year_level,
    d.device_name,
    d.brand,
    d.model,
    d.serial_number,
    d.device_type,
    d.device_purpose,
    d.image_path,
    d.created_at
FROM   devices d
           JOIN   students s ON s.student_id = d.student_id
WHERE  d.registration_status = 'pending'
ORDER  BY d.created_at;


-- Active event requests with device count.
CREATE OR REPLACE VIEW v_active_event_requests AS
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
WHERE  er.status IN ('pending', 'approved')
GROUP  BY
    er.event_request_id, er.student_id,
    s.first_name, s.last_name,
    er.event_name, er.organization,
    er.start_date, er.end_date, er.status;


-- ============================================================
-- SECTION 6: AUTOVACUUM TUNING
-- ============================================================
-- device_logs and audit_logs receive the highest INSERT rate.
-- Lower the scale factors so autovacuum runs more frequently
-- and prevents table bloat.

ALTER TABLE device_logs SET (
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
COMMENT ON TABLE  devices                     IS 'Permanent BYOD registered devices.';
COMMENT ON COLUMN devices.registration_status IS 'State machine: pending → approved | pending → rejected | rejected → pending.';
COMMENT ON COLUMN devices.device_status       IS 'active or inactive. Inactive devices cannot log entry/exit.';

COMMENT ON TABLE  event_requests              IS 'Header for a temporary device access request.';
COMMENT ON TABLE  event_request_devices       IS 'Individual devices listed under an event request.';

COMMENT ON TABLE  device_logs                 IS 'Immutable gate event log. Never update or delete rows.';
COMMENT ON COLUMN device_logs.auto_exit       IS 'TRUE = generated by nightly auto-exit batch, not a human guard.';

COMMENT ON TABLE  audit_logs                  IS 'Immutable audit trail. Write via fn_write_audit_log() only.';

COMMENT ON VIEW   v_device_campus_status      IS 'Derives inside/outside per device from the latest device_log row.';
COMMENT ON VIEW   v_pending_devices           IS 'Pending device registrations for the admin approval queue.';
COMMENT ON VIEW   v_active_event_requests     IS 'Pending and approved event requests with device counts.';

COMMENT ON FUNCTION fn_write_audit_log        IS 'Preferred way to write to audit_logs from Java. Keeps inserts consistent.';
COMMENT ON FUNCTION fn_set_updated_at         IS 'Auto-refreshes updated_at on every UPDATE.';

COMMENT ON TABLE  system_settings             IS 'System settings and policy parameters.';


-- ============================================================
-- SECTION 8: SEED DATA (SYSTEM SETTINGS)
-- ============================================================

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('max_devices_per_student', '3', 'Maximum number of active registered devices allowed per student'),
('allow_unregistered_devices', 'false', 'Whether unapproved devices can be checked in by guards');


-- ============================================================
-- END OF SCHEMA
-- ============================================================