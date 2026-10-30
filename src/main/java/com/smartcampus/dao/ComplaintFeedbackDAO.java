package com.smartcampus.dao;

import com.smartcampus.config.DBConnection;
import com.smartcampus.model.ComplaintFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class ComplaintFeedbackDAO {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintFeedbackDAO.class);

    public ComplaintFeedback create(ComplaintFeedback feedback) throws SQLException {
        String sql = "INSERT INTO complaint_feedback (complaint_id, student_id, rating, comments) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, feedback.getComplaintId());
            ps.setInt(2, feedback.getStudentId());
            ps.setInt(3, feedback.getRating());
            ps.setString(4, feedback.getComments());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    feedback.setFeedbackId(rs.getInt(1));
                }
            }
        }
        return feedback;
    }

    public Optional<ComplaintFeedback> findByComplaintId(int complaintId) throws SQLException {
        String sql = "SELECT f.*, u.full_name as student_name FROM complaint_feedback f " +
                     "JOIN users u ON f.student_id = u.user_id " +
                     "WHERE f.complaint_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ComplaintFeedback f = new ComplaintFeedback();
                    f.setFeedbackId(rs.getInt("feedback_id"));
                    f.setComplaintId(rs.getInt("complaint_id"));
                    f.setStudentId(rs.getInt("student_id"));
                    f.setStudentName(rs.getString("student_name"));
                    f.setRating(rs.getInt("rating"));
                    f.setComments(rs.getString("comments"));
                    f.setCreatedAt(rs.getTimestamp("created_at"));
                    return Optional.of(f);
                }
            }
        }
        return Optional.empty();
    }
}
