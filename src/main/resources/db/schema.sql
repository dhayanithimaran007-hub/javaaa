CREATE DATABASE IF NOT EXISTS smart_campus_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smart_campus_db;

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

CREATE TABLE IF NOT EXISTS complaint_escalations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT NOT NULL,
    escalation_level INT NOT NULL,
    previous_level INT NOT NULL DEFAULT 0,
    sla_status VARCHAR(30) NOT NULL DEFAULT 'ON_TRACK',
    escalation_reason TEXT NOT NULL,
    breach_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL DEFAULT NULL,
    acknowledged_by INT NULL DEFAULT NULL,
    intervention_note TEXT DEFAULT NULL,
    CONSTRAINT fk_esc_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_esc_ack_user FOREIGN KEY (acknowledged_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_esc_level CHECK (escalation_level BETWEEN 0 AND 3),
    UNIQUE KEY uk_complaint_esc_level (complaint_id, escalation_level),
    INDEX idx_esc_complaint (complaint_id),
    INDEX idx_esc_level (escalation_level),
    INDEX idx_esc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS predictive_analysis (
    id INT AUTO_INCREMENT PRIMARY KEY,
    analysis_period_start TIMESTAMP NOT NULL,
    analysis_period_end TIMESTAMP NOT NULL,
    analysis_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_complaints_analyzed INT NOT NULL DEFAULT 0,
    analysis_summary TEXT NOT NULL,
    generated_by INT NULL DEFAULT NULL,
    CONSTRAINT fk_pred_user FOREIGN KEY (generated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_pred_timestamp (analysis_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenance_risk_assessments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    analysis_id INT NOT NULL,
    building VARCHAR(100) NOT NULL,
    location VARCHAR(120) NOT NULL,
    category VARCHAR(50) NOT NULL,
    incident_count INT NOT NULL,
    risk_score DECIMAL(5,2) NOT NULL,
    risk_level ENUM('LOW', 'MODERATE', 'HIGH', 'CRITICAL') NOT NULL,
    contributing_factors TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_analysis FOREIGN KEY (analysis_id) REFERENCES predictive_analysis(id) ON DELETE CASCADE,
    CONSTRAINT chk_risk_score CHECK (risk_score BETWEEN 0.00 AND 100.00),
    INDEX idx_risk_analysis (analysis_id),
    INDEX idx_risk_building_cat (building, category),
    INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS preventive_maintenance_recommendations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    risk_assessment_id INT NOT NULL,
    building VARCHAR(100) NOT NULL,
    location VARCHAR(120) NOT NULL,
    category VARCHAR(50) NOT NULL,
    risk_score DECIMAL(5,2) NOT NULL,
    supporting_complaint_count INT NOT NULL DEFAULT 0,
    analysis_period_days INT NOT NULL DEFAULT 30,
    urgency VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    recommendation TEXT NOT NULL,
    status ENUM('NEW', 'UNDER_REVIEW', 'APPROVED', 'SCHEDULED', 'COMPLETED', 'DISMISSED') NOT NULL DEFAULT 'NEW',
    scheduled_date TIMESTAMP NULL DEFAULT NULL,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    created_by INT NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rec_assessment FOREIGN KEY (risk_assessment_id) REFERENCES maintenance_risk_assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_creator FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_rec_status (status),
    INDEX idx_rec_building_cat (building, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
