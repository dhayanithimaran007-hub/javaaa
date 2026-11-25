package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash, role, department, phone, active, created_at, updated_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email != null ? email.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash, role, department, phone, active, created_at, updated_at " +
                     "FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, department, phone, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getDepartment());
            ps.setString(6, user.getPhone());
            ps.setBoolean(7, user.isActive());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
        return user;
    }

    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, department = ?, phone = ?, active = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getDepartment());
            ps.setString(3, user.getPhone());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getUserId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> findAll(String roleFilter, Boolean activeFilter, String searchQuery) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT user_id, full_name, email, password_hash, role, department, phone, active, created_at, updated_at FROM users WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (roleFilter != null && !roleFilter.isBlank() && !roleFilter.equalsIgnoreCase("ALL")) {
            sql.append(" AND role = ?");
            params.add(roleFilter);
        }
        if (activeFilter != null) {
            sql.append(" AND active = ?");
            params.add(activeFilter);
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(department) LIKE ?)");
            String queryParam = "%" + searchQuery.trim().toLowerCase() + "%";
            params.add(queryParam);
            params.add(queryParam);
            params.add(queryParam);
        }
        sql.append(" ORDER BY user_id DESC");

        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    public List<User> findActiveStaff() throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash, role, department, phone, active, created_at, updated_at " +
                     "FROM users WHERE role = 'STAFF' AND active = TRUE ORDER BY full_name ASC";
        List<User> staffList = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                staffList.add(mapRowToUser(rs));
            }
        }
        return staffList;
    }

    public java.util.Map<Integer, Integer> getStaffWorkloadMap() throws SQLException {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        String sql = "SELECT ca.staff_id, COUNT(DISTINCT ca.complaint_id) as active_cnt " +
                     "FROM complaint_assignments ca " +
                     "JOIN complaints c ON ca.complaint_id = c.complaint_id " +
                     "WHERE ca.is_active = TRUE AND c.status NOT IN ('RESOLVED', 'REJECTED') " +
                     "GROUP BY ca.staff_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt("staff_id"), rs.getInt("active_cnt"));
            }
        }
        return map;
    }

    public com.smartcampus.model.StudentProfileDTO getStudentProfile(int studentId) throws SQLException {
        User student = findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for ID: " + studentId));

        int total = 0;
        int resolved = 0;
        int inProgress = 0;
        double avgRating = 0.0;

        String statsSql = "SELECT " +
                          "COUNT(*) as total, " +
                          "COUNT(CASE WHEN status = 'RESOLVED' THEN 1 END) as resolved_cnt, " +
                          "COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as prog_cnt " +
                          "FROM complaints WHERE student_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(statsSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                    resolved = rs.getInt("resolved_cnt");
                    inProgress = rs.getInt("prog_cnt");
                }
            }
        }

        String ratingSql = "SELECT AVG(rating) FROM complaint_feedback WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(ratingSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avgRating = rs.getDouble(1);
                }
            }
        }

        ComplaintDAO complaintDAO = new ComplaintDAO();
        List<com.smartcampus.model.Complaint> complaints = complaintDAO.findComplaints(
                studentId, null, null, null, null, null, null, null, "date", false);

        return new com.smartcampus.model.StudentProfileDTO(student, total, resolved, inProgress, avgRating, complaints);
    }

    public List<User> findActiveDemoAccounts() throws SQLException {
        String sql = "SELECT user_id, full_name, email, role, department " +
                     "FROM users WHERE active = TRUE AND role IN ('ADMIN', 'STAFF', 'STUDENT') " +
                     "ORDER BY FIELD(role, 'ADMIN', 'STAFF', 'STUDENT'), user_id";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setFullName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setRole(Role.fromString(rs.getString("role")));
                u.setDepartment(rs.getString("department"));
                u.setActive(true);
                list.add(u);
            }
        }
        return list;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.fromString(rs.getString("role")));
        u.setDepartment(rs.getString("department"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
