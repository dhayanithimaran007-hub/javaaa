-- =======================================================================
-- Smart Campus Complaint Prioritization and Tracking System
-- MySQL 8.0+ Database Creation and Initialization Script
-- Location: database/schema.sql
-- =======================================================================
-- This script is idempotent and safe to rerun without dropping data.
-- It creates the database, creates all 6 tables, constraints, indexes,
-- and provisions the default seed accounts and demo records.
-- =======================================================================

-- -----------------------------------------------------------------------
-- 1. DATABASE CREATION
-- -----------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS smart_campus_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_campus_db;

-- -----------------------------------------------------------------------
-- 2. TABLE DEFINITIONS
-- -----------------------------------------------------------------------

-- 2.1 USERS TABLE
-- Stores campus accounts (Students, Administrators, Maintenance Staff)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN', 'STAFF') NOT NULL,
    department VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 COMPLAINTS TABLE
-- Core entity storing complaints, urgency parameters, and computed priority
CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    building VARCHAR(100) NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    severity INT NOT NULL,
    safety_risk INT NOT NULL,
    people_affected INT NOT NULL,
    operational_impact INT NOT NULL,
    priority_score DECIMAL(5,2) NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    is_safety_critical BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_complaints_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_severity CHECK (severity BETWEEN 1 AND 5),
    CONSTRAINT chk_safety_risk CHECK (safety_risk BETWEEN 0 AND 5),
    CONSTRAINT chk_people_affected CHECK (people_affected >= 1),
    CONSTRAINT chk_operational_impact CHECK (operational_impact BETWEEN 1 AND 5),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_priority (priority),
    INDEX idx_complaints_student (student_id),
    INDEX idx_complaints_created (created_at),
    INDEX idx_complaints_category (category),
    INDEX idx_complaints_building (building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.3 COMPLAINT ASSIGNMENTS TABLE
-- Tracks technician work order assignments by administrators
CREATE TABLE IF NOT EXISTS complaint_assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL,
    staff_id INT NOT NULL,
    assigned_by INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_assignments_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_staff FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignments_admin FOREIGN KEY (assigned_by) REFERENCES users(user_id) ON DELETE RESTRICT,
    INDEX idx_assignments_lookup (complaint_id, staff_id, is_active),
    INDEX idx_assignments_staff_active (staff_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.4 COMPLAINT STATUS HISTORY TABLE
-- Immutable audit ledger recording each lifecycle transition
CREATE TABLE IF NOT EXISTS complaint_status_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL,
    changed_by INT NOT NULL,
    old_status VARCHAR(30) DEFAULT NULL,
    new_status VARCHAR(30) NOT NULL,
    remarks TEXT DEFAULT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(user_id) ON DELETE RESTRICT,
    INDEX idx_history_complaint (complaint_id),
    INDEX idx_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.5 COMPLAINT FEEDBACK TABLE
-- Student satisfaction rating (1-5 stars) and review upon resolution
CREATE TABLE IF NOT EXISTS complaint_feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL UNIQUE,
    student_id INT NOT NULL,
    rating INT NOT NULL,
    comments TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.6 AUDIT LOGS TABLE
-- System security and administrative event trail
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id INT NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INT DEFAULT NULL,
    details TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    INDEX idx_audit_actor (actor_user_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------
-- 3. INITIAL SEED DATA
-- -----------------------------------------------------------------------
-- Passwords below are pre-computed BCrypt hashes (cost factor 12):
--   Admin:   admin@campus.edu         / Admin@123
--   Staff:   electrician@campus.edu   / Staff@123
--   Staff:   plumber@campus.edu       / Staff@123
--   Student: alice@student.campus.edu / Student@123
--   Student: bob@student.campus.edu   / Student@123

INSERT INTO users (user_id, full_name, email, password_hash, role, department, phone, active) VALUES
(1, 'Campus Administrator', 'admin@campus.edu', '$2a$12$6LT7GJOQUYo9RqjI/wqkBO5EjfIfLfNrgeNnTK5ftYHJ9EIMf.ySi', 'ADMIN', 'Administration', '9876543210', TRUE),
(2, 'Rajesh Kumar (Electrician)', 'electrician@campus.edu', '$2a$12$o4zevcy.yGq3TPJ8rKOsweUIKFqkNoTtX04/URvyCNiYQucR57HOC', 'STAFF', 'Electrical Maintenance', '9876543211', TRUE),
(3, 'Suresh Patel (Plumber)', 'plumber@campus.edu', '$2a$12$o4zevcy.yGq3TPJ8rKOsweUIKFqkNoTtX04/URvyCNiYQucR57HOC', 'STAFF', 'Plumbing & Sanitation', '9876543212', TRUE),
(4, 'Alice Sharma', 'alice@student.campus.edu', '$2a$12$hdbbAfxhYEarTxU8Iezf5u/RdeNlsbBhXDd.jX0BakGH5B2sZtnTy', 'STUDENT', 'Computer Science', '9876543213', TRUE),
(5, 'Bob Verghese', 'bob@student.campus.edu', '$2a$12$hdbbAfxhYEarTxU8Iezf5u/RdeNlsbBhXDd.jX0BakGH5B2sZtnTy', 'STUDENT', 'Mechanical Engineering', '9876543214', TRUE)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- Sample complaints for immediate evaluation
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

-- Sample assignments
INSERT IGNORE INTO complaint_assignments (assignment_id, complaint_id, staff_id, assigned_by, assigned_at, notes, is_active)
VALUES
(1, 1, 2, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'Urgent safety hazard - inspect immediately', TRUE),
(2, 3, 3, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR), 'Fix basin plumbing', FALSE);

-- Sample status history
INSERT IGNORE INTO complaint_status_history (history_id, complaint_id, changed_by, old_status, new_status, remarks, changed_at)
VALUES
(1, 1, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(2, 1, 1, 'SUBMITTED', 'ACKNOWLEDGED', 'Assigned to Rajesh Kumar (Electrician)', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 2, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(4, 2, 1, 'SUBMITTED', 'IN_PROGRESS', 'Network team dispatch approved.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(5, 3, 4, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 3, 3, 'SUBMITTED', 'RESOLVED', 'Resolution: Replaced Teflon tape and washers. No more leaks.', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(7, 4, 5, NULL, 'SUBMITTED', 'Complaint submitted.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- Sample feedback
INSERT IGNORE INTO complaint_feedback (feedback_id, complaint_id, student_id, rating, comments, created_at)
VALUES
(1, 3, 4, 5, 'Promptly fixed! The washroom is clean and the leak stopped.', DATE_SUB(NOW(), INTERVAL 1 HOUR));
