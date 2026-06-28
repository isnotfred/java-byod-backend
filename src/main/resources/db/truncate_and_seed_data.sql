-- 1. Truncate target tables and reset their auto-increment sequences
TRUNCATE TABLE device_transactions, request_devices, requests, students RESTART IDENTITY CASCADE;

-- 2. Populate Students, Requests, Request Devices, and Gate Transactions
DO $$
DECLARE
    v_user_id INT;
    v_req1_id INT; v_req2_id INT;
    v_req3_id INT; v_req4_id INT;
    v_req5_id INT; v_req6_id INT;
    v_req7_id INT; v_req8_id INT;
    v_req9_id INT; v_req10_id INT;
    
    v_dev1_id INT; v_dev2_id INT;
    v_dev3_id INT; v_dev4_id INT;
    v_dev5_id INT; v_dev6_id INT;
    v_dev7_id INT; v_dev8_id INT;
    v_dev9_id INT; v_dev10_id INT;
BEGIN
    -- Resolve acting user (use existing admin, or insert a default if table is empty)
    SELECT user_id INTO v_user_id FROM users WHERE role = 'admin' OR role = 'super_admin' LIMIT 1;
    IF v_user_id IS NULL THEN
        INSERT INTO users (username, email, password_hash, full_name, role, status)
        VALUES ('admin', 'admin@byod.com', '$2a$10$w821jXkexF8M21mXw7bIcuq0F.d4Uf.bC1.y9/lq/J5uL5c/1/b7u', 'System Admin', 'admin', 'active')
        RETURNING user_id INTO v_user_id;
    END IF;

    -- A. Populate 5 Students (using 2###-######-SR-0 Student ID and 'Text #-#' Course Year Level format)
    INSERT INTO students (student_id, first_name, last_name, course_year_level, contact_number, status) VALUES
    ('2024-00481-SR-0', 'John', 'Doe', 'BSIT 2-2', '09123456789', 'active'),
    ('2024-00482-SR-0', 'Jane', 'Smith', 'BSECE 3-4', '09187654321', 'active'),
    ('2024-00483-SR-0', 'Bob', 'Johnson', 'BSCS 1-1', '09223334444', 'active'),
    ('2024-00484-SR-0', 'Alice', 'Williams', 'BSME 4-2', '09334445555', 'active'),
    ('2024-00485-SR-0', 'Charlie', 'Brown', 'BSCE 2-1', '09445556666', 'active');

    -- B. Populate 2 Requests per Student (mix of normal/event, total 10 requests)
    
    -- Student 1 Requests (both approved, one has ongoing logs today)
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00481-SR-0', 'Academic Classes', '2026-06-12', '2026-06-12', '08:00:00', '17:00:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req1_id;
    
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00481-SR-0', 'Lab Work', '2026-06-28', '2026-06-28', '08:00:00', '18:00:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req2_id;

    -- Student 2 Requests (both approved)
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00482-SR-0', 'Study Session', '2026-06-01', '2026-06-01', '08:00:00', '17:00:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req3_id;
    
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00482-SR-0', 'Thesis Defense', '2026-06-18', '2026-06-18', '08:30:00', '17:30:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req4_id;

    -- Student 3 Requests (both approved, event requests, one has ongoing logs today)
    INSERT INTO requests (request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('event', '2024-00483-SR-0', 'Tech Expo Stage 1', 'Computer Society', 'Dr. Garcia', 'Project Presentation', '2026-06-05', '2026-06-10', '08:00:00', '18:00:00', 'approved', TRUE, TRUE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req5_id;

    INSERT INTO requests (request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('event', '2024-00483-SR-0', 'Hackathon Finals', 'CS Dept', 'Prof. Miller', 'Code Sprint', '2026-06-25', '2026-06-30', '07:30:00', '19:30:00', 'approved', TRUE, TRUE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req6_id;

    -- Student 4 Requests (both pending - satisfies 'at least 2 pending')
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00484-SR-0', 'Regular BYOD classes', '2026-06-28', '2026-06-28', '08:00:00', '17:00:00', 'pending', TRUE, FALSE, NULL, NULL)
    RETURNING request_id INTO v_req7_id;

    INSERT INTO requests (request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('event', '2024-00484-SR-0', 'Sports Fest', 'PUP Org', 'Dr. Adams', 'Covering media', '2026-06-25', '2026-06-30', '08:00:00', '17:00:00', 'pending', TRUE, FALSE, NULL, NULL)
    RETURNING request_id INTO v_req8_id;

    -- Student 5 Requests (one approved normal, one approved event)
    INSERT INTO requests (request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('normal', '2024-00485-SR-0', 'Academic Research', '2026-06-28', '2026-06-28', '08:00:00', '18:00:00', 'approved', TRUE, FALSE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req9_id;

    INSERT INTO requests (request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
    VALUES ('event', '2024-00485-SR-0', 'PUP Seminar', 'Junior Engineers', 'Engr. Santos', 'Training Support', '2026-06-01', '2026-06-05', '08:00:00', '18:00:00', 'approved', TRUE, TRUE, v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_id INTO v_req10_id;

    -- C. Populate Request Devices for all requests
    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req1_id, 'MacBook Air', 'Apple', 'M2 13-inch', 'Personal Computers', 'SN-DEV-0011', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev1_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req2_id, 'MacBook Air Updated', 'Apple', 'M2 13-inch', 'Personal Computers', 'SN-DEV-0012', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev2_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req3_id, 'Dell XPS 15', 'Dell', 'XPS 9520', 'Personal Computers', 'SN-DEV-0021', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev3_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req4_id, 'Dell XPS 15 Updated', 'Dell', 'XPS 9520', 'Personal Computers', 'SN-DEV-0022', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev4_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req5_id, 'Arduino Kit', 'Arduino', 'Uno R3', 'Project Prototypes (Optional SN)', 'SN-DEV-0031', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev5_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req6_id, 'Arduino Kit Updated', 'Arduino', 'Uno R3', 'Project Prototypes (Optional SN)', 'SN-DEV-0032', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev6_id;

    -- Student 4 Devices (linked to pending requests)
    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req7_id, 'Lenovo ThinkPad', 'Lenovo', 'T14 Gen 3', 'Personal Computers', 'SN-DEV-0041', 1, 'pending', NULL, NULL)
    RETURNING request_device_id INTO v_dev7_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req8_id, 'DSLR Camera', 'Canon', 'EOS 80D', 'Other Peripherals', 'SN-DEV-0042', 1, 'pending', NULL, NULL)
    RETURNING request_device_id INTO v_dev8_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req9_id, 'iPad Pro', 'Apple', 'M1 11-inch', 'Mobile Devices (Tablet/Phone)', 'SN-DEV-0051', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev9_id;

    INSERT INTO request_devices (request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at)
    VALUES (v_req10_id, 'iPad Pro Updated', 'Apple', 'M1 11-inch', 'Mobile Devices (Tablet/Phone)', 'SN-DEV-0052', 1, 'approved', v_user_id, CURRENT_TIMESTAMP)
    RETURNING request_device_id INTO v_dev10_id;

    -- D. Populate Logs from June 1 to June 28, 2026 (incorporating missed checkouts and late check-ins/outs)
    
    -- June 1, 2026: Standard logs (On Time)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-01', '2026-06-01 07:45:00+08', v_user_id, '2026-06-01 16:50:00+08', v_user_id, FALSE),
    (v_dev3_id, '2026-06-01', '2026-06-01 08:15:00+08', v_user_id, '2026-06-01 17:02:00+08', v_user_id, FALSE),
    (v_dev10_id, '2026-06-01', '2026-06-01 08:10:00+08', v_user_id, '2026-06-01 17:55:00+08', v_user_id, FALSE);

    -- June 5, 2026: Late Check-in (Device 5 expected at 08:00, checked-in at 09:15 -> 1 hr 15 mins late)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev5_id, '2026-06-05', '2026-06-05 09:15:00+08', v_user_id, '2026-06-05 17:45:00+08', v_user_id, FALSE);

    -- June 8, 2026: Late Check-out (Device 5 expected egress at 18:00, checked-out at 19:15 -> 1 hr 15 mins late)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev5_id, '2026-06-08', '2026-06-08 07:55:00+08', v_user_id, '2026-06-08 19:15:00+08', v_user_id, FALSE);

    -- June 12, 2026: Missed checkout simulation for Device 1 (Ingress logged but Egress is NULL and flagged as missed)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev1_id, '2026-06-12', '2026-06-12 07:50:00+08', v_user_id, NULL, NULL, TRUE);

    -- June 18, 2026: Normal logs
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev2_id, '2026-06-18', '2026-06-18 08:10:00+08', v_user_id, '2026-06-18 17:45:00+08', v_user_id, FALSE),
    (v_dev4_id, '2026-06-18', '2026-06-18 08:20:00+08', v_user_id, '2026-06-18 17:20:00+08', v_user_id, FALSE);

    -- June 26, 2026: Missed checkout simulation on event Device 6
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev6_id, '2026-06-26', '2026-06-26 07:45:00+08', v_user_id, NULL, NULL, TRUE);

    -- June 28, 2026: Ongoing states (active/ongoing logs today)
    -- 1. Device 2 (John Doe) checked in today but hasn't checked out yet (Ongoing / Inside campus)
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev2_id, '2026-06-28', '2026-06-28 07:45:00+08', v_user_id, NULL, NULL, FALSE);

    -- 2. Device 6 (Bob Johnson - Event) checked in and out today
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev6_id, '2026-06-28', '2026-06-28 07:35:00+08', v_user_id, '2026-06-28 12:45:00+08', v_user_id, FALSE);

    -- 3. Device 9 (Charlie Brown) checked in late today (Expected 08:00, Ingress 09:15 -> Late check-in) and egressed at 17:45
    INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
    (v_dev9_id, '2026-06-28', '2026-06-28 09:15:00+08', v_user_id, '2026-06-28 17:45:00+08', v_user_id, FALSE);

END $$;
