-- 1. Truncate target tables and reset their auto-increment sequences
TRUNCATE TABLE device_transactions, request_devices, requests, students RESTART IDENTITY CASCADE;

-- 2. Ensure default admin user exists
INSERT INTO users (username, email, password_hash, full_name, role, status)
VALUES ('admin', 'admin@byod.com', '$2a$10$w821jXkexF8M21mXw7bIcuq0F.d4Uf.bC1.y9/lq/J5uL5c/1/b7u', 'System Admin', 'admin', 'active')
ON CONFLICT (username) DO NOTHING;

-- 3. Populate 5 Students
INSERT INTO students (student_id, first_name, last_name, course_year_level, contact_number, status) VALUES
('2024-00481-SR-0', 'John', 'Doe', 'BSIT 2-2', '09123456789', 'active'),
('2024-00482-SR-0', 'Jane', 'Smith', 'BSECE 3-4', '09187654321', 'active'),
('2024-00483-SR-0', 'Bob', 'Johnson', 'BSCS 1-1', '09223334444', 'active'),
('2024-00484-SR-0', 'Alice', 'Williams', 'BSME 4-2', '09334445555', 'active'),
('2024-00485-SR-0', 'Charlie', 'Brown', 'BSCE 2-1', '09445556666', 'active');

-- 4. Populate 10 Requests (with explicit IDs to guarantee matching sequence)
-- Student 1 (John Doe) - 2 Requests (Ongoing 1 & 2)
INSERT INTO requests (request_id, request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (1, 'normal', '2024-00481-SR-0', 'Regular Lectures', '2026-07-01'::date, '2026-07-01'::date, '08:00:00'::time, '17:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

INSERT INTO requests (request_id, request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (2, 'event', '2024-00481-SR-0', 'Engineering Exhibition', 'Engineering Society', 'Dr. Cruz', 'Capstone Showcase', '2026-06-30'::date, '2026-07-02'::date, '08:30:00'::time, '18:30:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Student 2 (Jane Smith) - 2 Requests (Ongoing 3 & Completed 1)
INSERT INTO requests (request_id, request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (3, 'normal', '2024-00482-SR-0', 'Library Thesis Study', '2026-07-01'::date, '2026-07-01'::date, '09:00:00'::time, '18:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

INSERT INTO requests (request_id, request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (4, 'normal', '2024-00482-SR-0', 'Computer Lab Exam', '2026-06-25'::date, '2026-06-25'::date, '08:00:00'::time, '17:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Student 3 (Bob Johnson) - 2 Requests (Completed 2 & 3)
INSERT INTO requests (request_id, request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (5, 'event', '2024-00483-SR-0', 'Developer BootCamp', 'CS Department', 'Prof. Perez', 'Coding Workshops', '2026-06-20'::date, '2026-06-24'::date, '08:00:00'::time, '17:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

INSERT INTO requests (request_id, request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (6, 'normal', '2024-00483-SR-0', 'Software Lab Work', '2026-06-15'::date, '2026-06-15'::date, '09:00:00'::time, '18:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Student 4 (Alice Williams) - 2 Requests (Completed 4 & Cancelled 1)
INSERT INTO requests (request_id, request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (7, 'event', '2024-00484-SR-0', 'Visual Media Fest', 'Multimedia Club', 'Ms. Santos', 'Event Photography', '2026-06-10'::date, '2026-06-12'::date, '08:00:00'::time, '18:00:00'::time, 'approved', TRUE, TRUE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

INSERT INTO requests (request_id, request_type, student_id, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (8, 'normal', '2024-00484-SR-0', 'Make-up Class Session', '2026-07-01'::date, '2026-07-01'::date, '08:00:00'::time, '17:00:00'::time, 'cancelled', TRUE, FALSE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Student 5 (Charlie Brown) - 2 Requests (Upcoming 1 & Expired 1)
INSERT INTO requests (request_id, request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (9, 'event', '2024-00485-SR-0', 'Robotics Bootcamp', 'PUP Robotics', 'Engr. Rivera', 'Training and Competition', '2026-07-05'::date, '2026-07-08'::date, '09:00:00'::time, '17:00:00'::time, 'approved', TRUE, FALSE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

INSERT INTO requests (request_id, request_type, student_id, event_name, organization, responsible_person, purpose, start_date, end_date, expected_ingress_time, expected_egress_time, status, is_submitted, is_accommodated, reviewed_by, reviewed_at)
VALUES (10, 'event', '2024-00485-SR-0', 'Web Design Hackathon', 'IT Society', 'Mrs. Alcantara', 'UI design challenge', '2026-06-28'::date, '2026-06-29'::date, '08:00:00'::time, '17:00:00'::time, 'approved', TRUE, FALSE, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Reset serial primary key sequence for requests
SELECT setval('requests_request_id_seq', 10);

-- 5. Populate Request Devices
INSERT INTO request_devices (request_device_id, request_id, device_name, brand, model, device_type, serial_number, quantity, device_status, verified_by, verified_at) VALUES
(1, 1, 'MacBook Pro', 'Apple', 'M3 Pro 14"', 'Personal Computers', 'SN-DEV-0001', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(2, 2, 'iPad Air', 'Apple', 'M2 11"', 'Mobile Devices (Tablet/Phone)', 'SN-DEV-0002', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(3, 3, 'Dell Inspiron', 'Dell', 'Inspiron 15', 'Personal Computers', 'SN-DEV-0003', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(4, 4, 'HP Pavilion', 'HP', 'Pavilion 14', 'Personal Computers', 'SN-DEV-0004', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(5, 5, 'Asus ZenBook', 'Asus', 'ZenBook Duo', 'Personal Computers', 'SN-DEV-0005', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(6, 6, 'Lenovo IdeaPad', 'Lenovo', 'IdeaPad 3', 'Personal Computers', 'SN-DEV-0006', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(7, 7, 'Canon DSLR Camera', 'Canon', 'EOS 200D', 'Other Peripherals', 'SN-DEV-0007', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(8, 8, 'Arduino Uno Board', 'Arduino', 'Uno R3', 'Project Prototypes', 'SN-DEV-0008', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(9, 9, 'Raspberry Pi 4 Kit', 'Raspberry Pi', 'Model B', 'Project Prototypes', 'SN-DEV-0009', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP),
(10, 10, 'Fluke Multimeter', 'Fluke', 'Model 17B+', 'Other Peripherals', 'SN-DEV-0010', 1, 'approved', (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), CURRENT_TIMESTAMP);

-- Reset serial primary key sequence for request_devices
SELECT setval('request_devices_request_device_id_seq', 10);

-- 6. Populate Daily Ingress/Egress Transactions
-- Completed 1: Normal on June 25, 2026
INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked)
VALUES (4, '2026-06-25'::date, '2026-06-25 08:05:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), '2026-06-25 16:55:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), FALSE);

-- Completed 2: Event on June 21, 2026
INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked)
VALUES (5, '2026-06-21'::date, '2026-06-21 08:12:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), '2026-06-21 17:05:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), FALSE);

-- Completed 3: Normal on June 15, 2026
INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked)
VALUES (6, '2026-06-15'::date, '2026-06-15 08:45:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), '2026-06-15 17:50:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), FALSE);

-- Completed 4: Event on June 11, 2026
INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked)
VALUES (7, '2026-06-11'::date, '2026-06-11 07:50:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), '2026-06-11 18:15:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), FALSE);

-- Ongoing 1, 2, 3: Active today (July 1, 2026) check-in logs
INSERT INTO device_transactions (request_device_id, log_date, ingress_time, ingress_handled_by, egress_time, egress_handled_by, no_egress_marked) VALUES
(1, '2026-07-01'::date, '2026-07-01 07:55:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), NULL::timestamptz, NULL::integer, FALSE),
(2, '2026-07-01'::date, '2026-07-01 08:20:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), NULL::timestamptz, NULL::integer, FALSE),
(3, '2026-07-01'::date, '2026-07-01 08:45:00+08'::timestamptz, (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), NULL::timestamptz, NULL::integer, FALSE);
