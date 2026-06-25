-- 1. Truncate target tables and reset their auto-increment sequences
TRUNCATE TABLE device_transactions, request_devices, requests, students RESTART IDENTITY CASCADE;

-- 2. Populate Students, Requests, Request Devices, and one week of Gate Transactions
DO $$
DECLARE
    v_user_id INT;
    v_req1_id INT;
    v_req2_id INT;
    v_req3_id INT;
    v_dev1_id INT;
    v_dev2_id INT;
    v_dev3_id INT;
    v_dev4_id INT;
BEGIN
    -- Resolve acting user (use existing admin, or insert a default if table is empty)
    SELECT user_id INTO v_user_id FROM users WHERE role = 'admin' OR role = 'super_admin' LIMIT 1;
    IF v_user_id IS NULL THEN
        INSERT INTO users (username, email, password_hash, full_name, role, status)
        VALUES ('admin', 'admin@byod.com', '$2a$10$w821jXkexF8M21mXw7bIcuq0F.d4Uf.bC1.y9/lq/J5uL5c/1/b7u', 'System Admin', 'admin', 'active')
        RETURNING user_id INTO v_user_id;
    END IF;

    -- A. Populate Students
    INSERT INTO students (student_id, first_name, last_name, course_year_level, contact_number, status) VALUES
    ('2021-10001', 'John', 'Doe', 'BSCS - 4th Year', '09123456789', 'active'),
    ('2021-10002', 'Jane', 'Smith', 'BSIT - 3rd Year', '09187654321', 'active'),
    ('2021-10003', 'Bob', 'Johnson', 'BSCPE - 2nd Year', '09223334444', 'active');

    -- B. Populate Requests
    -- Request 1: Academic BYOD for Student 1
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2021-10001', 'Academic BYOD Classes', '2026-06-15', '2026-06-22', '07:30:00', '18:30:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req1_id;

    -- Request 2: Academic BYOD for Student 2
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2021-10002', 'Academic BYOD Lab Use', '2026-06-15', '2026-06-22', '08:00:00', '17:00:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req2_id;

    -- Request 3: Event Request for Student 3
    INSERT INTO requests (request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('event', '2021-10003', 'Innovation Tech Expo 2026', 'Computer Society', 'Dr. Garcia', 'Project Exhibition', '2026-06-15', '2026-06-20', '08:00:00', '17:00:00', 'approved', TRUE, TRUE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req3_id;

    -- C. Populate Request Devices
    -- Devices for Request 1 (John Doe)
    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req1_id, 'MacBook Air', 'Apple', 'M2 13-inch', 'Personal Computers', 'SN-APPLE-M2-9908', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev1_id;

    -- Devices for Request 2 (Jane Smith)
    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req2_id, 'Dell XPS 15', 'Dell', 'XPS 9520', 'Personal Computers', 'SN-DELL-XPS-8877', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev2_id;

    -- Devices for Request 3 (Bob Johnson - Event)
    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req3_id, 'Raspberry Pi 4 Model B', 'Raspberry Pi', '4GB', 'Project Prototypes (Optional SN)', 'SN-RPI-4B-012A', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev3_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req3_id, 'Soldering Station', 'Hakko', 'FX-888D', 'Appliances (TLE)', 'SN-HAKKO-888D', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev4_id;

    -- D. Populate 1 Week Worth of Gate Transactions (Monday June 15, 2026 to Saturday June 20, 2026)
    
    -- Monday June 15, 2026
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-15', '2026-06-15 07:45:00+08', v_user_id, '2026-06-15 18:15:00+08', v_user_id, FALSE),
    (v_dev2_id, '2026-06-15', '2026-06-15 08:10:00+08', v_user_id, '2026-06-15 17:05:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-15', '2026-06-15 08:30:00+08', v_user_id, '2026-06-15 16:50:00+08', v_user_id, FALSE);

    -- Tuesday June 16, 2026
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-16', '2026-06-16 07:35:00+08', v_user_id, '2026-06-16 18:20:00+08', v_user_id, FALSE),
    (v_dev2_id, '2026-06-16', '2026-06-16 08:05:00+08', v_user_id, '2026-06-16 17:15:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-16', '2026-06-16 08:25:00+08', v_user_id, '2026-06-16 16:45:00+08', v_user_id, FALSE),
    (v_dev4_id, '2026-06-16', '2026-06-16 08:40:00+08', v_user_id, '2026-06-16 16:30:00+08', v_user_id, FALSE);

    -- Wednesday June 17, 2026 (Simulate a missed checkout for Device 3)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-17', '2026-06-17 07:50:00+08', v_user_id, '2026-06-17 18:05:00+08', v_user_id, FALSE),
    (v_dev2_id, '2026-06-17', '2026-06-17 08:15:00+08', v_user_id, '2026-06-17 17:10:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-17', '2026-06-17 08:20:00+08', v_user_id, NULL,                  NULL,      TRUE);

    -- Thursday June 18, 2026
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-18', '2026-06-18 07:40:00+08', v_user_id, '2026-06-18 18:25:00+08', v_user_id, FALSE),
    (v_dev2_id, '2026-06-18', '2026-06-18 08:00:00+08', v_user_id, '2026-06-18 17:02:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-18', '2026-06-18 08:35:00+08', v_user_id, '2026-06-18 16:55:00+08', v_user_id, FALSE),
    (v_dev4_id, '2026-06-18', '2026-06-18 08:45:00+08', v_user_id, '2026-06-18 16:40:00+08', v_user_id, FALSE);

    -- Friday June 19, 2026
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-19', '2026-06-19 07:48:00+08', v_user_id, '2026-06-19 18:10:00+08', v_user_id, FALSE),
    (v_dev2_id, '2026-06-19', '2026-06-19 08:12:00+08', v_user_id, '2026-06-19 17:18:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-19', '2026-06-19 08:18:00+08', v_user_id, '2026-06-19 16:58:00+08', v_user_id, FALSE);

    -- Saturday June 20, 2026
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-20', '2026-06-20 08:30:00+08', v_user_id, '2026-06-20 14:15:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-20', '2026-06-20 09:00:00+08', v_user_id, '2026-06-20 15:00:00+08', v_user_id, FALSE);
    
END $$;
