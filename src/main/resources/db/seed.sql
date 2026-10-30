-- =======================================================================
-- Smart Campus Complaint System - Seed Data Script
-- =======================================================================

USE smart_campus_db;

-- 1. SEED USERS
-- Password for all seed users is standard format:
--   Admin:       admin@campus.edu       / Admin@123
--   Staff:       electrician@campus.edu / Staff@123
--   Staff:       plumber@campus.edu     / Staff@123
--   Student:     alice@student.campus.edu / Student@123
--   Student:     bob@student.campus.edu   / Student@123
-- The passwords below are hashed with standard BCrypt (cost factor 12)

INSERT INTO users (user_id, full_name, email, password_hash, role, department, phone, active) VALUES
(1, 'Campus Administrator', 'admin@campus.edu', '$2a$12$6LT7GJOQUYo9RqjI/wqkBO5EjfIfLfNrgeNnTK5ftYHJ9EIMf.ySi', 'ADMIN', 'Administration', '9876543210', TRUE),
(2, 'Rajesh Kumar (Electrician)', 'electrician@campus.edu', '$2a$12$o4zevcy.yGq3TPJ8rKOsweUIKFqkNoTtX04/URvyCNiYQucR57HOC', 'STAFF', 'Electrical Maintenance', '9876543211', TRUE),
(3, 'Suresh Patel (Plumber)', 'plumber@campus.edu', '$2a$12$o4zevcy.yGq3TPJ8rKOsweUIKFqkNoTtX04/URvyCNiYQucR57HOC', 'STAFF', 'Plumbing & Sanitation', '9876543212', TRUE),
(4, 'Alice Sharma', 'alice@student.campus.edu', '$2a$12$hdbbAfxhYEarTxU8Iezf5u/RdeNlsbBhXDd.jX0BakGH5B2sZtnTy', 'STUDENT', 'Computer Science', '9876543213', TRUE),
(5, 'Bob Verghese', 'bob@student.campus.edu', '$2a$12$hdbbAfxhYEarTxU8Iezf5u/RdeNlsbBhXDd.jX0BakGH5B2sZtnTy', 'STUDENT', 'Mechanical Engineering', '9876543214', TRUE)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- 2. SEED SAMPLE COMPLAINTS
INSERT IGNORE INTO complaints (complaint_id, student_id, title, description, category, building, room_number,
    severity, safety_risk, people_affected, operational_impact, priority_score, priority, is_safety_critical, status, created_at, resolved_at)
VALUES
(1, 4, 'Exposed High-Voltage Wiring in Physics Lab', 'Sparking wires hanging near workstation 4 in Physics Lab 204. High risk of electrical shock.',
 'Electrical', 'Science Block A', 'Lab 204', 5, 5, 45, 4, 89.00, 'CRITICAL', TRUE, 'ACKNOWLEDGED', DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL),

(2, 4, 'Main Campus Wi-Fi Router Down in Library', 'The central Wi-Fi access point in the 2nd-floor study hall has failed. Over 150 students cannot access digital resources.',
 'Internet and Wi-Fi', 'Central Library', '2nd Floor Hall', 4, 0, 150, 5, 59.00, 'HIGH', FALSE, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL),

(3, 4, 'Water Leakage in Restroom Basin', 'Water valve under sink #2 in Ground Floor Restroom is dripping constantly.',
 'Plumbing and Water Supply', 'Main Academic Block', 'GF-Washroom-B', 2, 1, 15, 2, 34.00, 'MEDIUM', FALSE, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),

(4, 5, 'Broken Projector in Lecture Hall 3', 'Projector HDMI port does not transmit video output. Morning lectures disrupted.',
 'Classroom Equipment', 'Engineering Block', 'LH 301', 3, 0, 80, 4, 49.00, 'MEDIUM', FALSE, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL);

-- 3. SEED ASSIGNMENTS
INSERT IGNORE INTO complaint_assignments (assignment_id, complaint_id, staff_id, assigned_by, assigned_at, notes, is_active)
VALUES
(1, 1, 2, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'Urgent safety hazard - inspect immediately', TRUE),
(2, 3, 3, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR), 'Fix basin plumbing', FALSE);

-- 4. SEED STATUS HISTORY
INSERT IGNORE INTO complaint_status_history (history_id, complaint_id, changed_by, old_status, new_status, remarks, changed_at)
VALUES
(1, 1, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(2, 1, 1, 'SUBMITTED', 'ACKNOWLEDGED', 'Assigned to Rajesh Kumar (Electrician)', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 2, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(4, 2, 1, 'SUBMITTED', 'IN_PROGRESS', 'Network team dispatch approved.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(5, 3, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 3, 3, 'SUBMITTED', 'RESOLVED', 'Resolution: Replaced Teflon tape and washers. No more leaks.', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(7, 4, 5, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- 5. SEED FEEDBACK
INSERT IGNORE INTO complaint_feedback (feedback_id, complaint_id, student_id, rating, comments, created_at)
VALUES
(1, 3, 4, 5, 'Promptly fixed! The washroom is clean and the leak stopped.', DATE_SUB(NOW(), INTERVAL 1 HOUR));
