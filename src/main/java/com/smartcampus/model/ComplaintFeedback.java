package com.smartcampus.model;

import java.sql.Timestamp;

public class ComplaintFeedback {
    private int feedbackId;
    private int complaintId;
    private int studentId;
    private String studentName;
    private int rating; // 1 to 5
    private String comments;
    private Timestamp createdAt;

    public ComplaintFeedback() {}

    public ComplaintFeedback(int complaintId, int studentId, int rating, String comments) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.rating = rating;
        this.comments = comments;
    }

    public int getFeedbackId() { return feedbackId; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
