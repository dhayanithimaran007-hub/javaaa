package com.smartcampus.model;

import com.smartcampus.model.enums.ComplaintStatus;
import com.smartcampus.model.enums.PriorityLevel;
import java.sql.Timestamp;

public class Complaint {
    private int complaintId;
    private int studentId;
    private String studentName;
    private String studentEmail;
    private String title;
    private String description;
    private String category;
    private String building;
    private String roomNumber;
    private int severity;
    private int safetyRisk;
    private int peopleAffected;
    private int operationalImpact;
    private double priorityScore;
    private PriorityLevel priority;
    private boolean safetyCritical;
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp resolvedAt;

    // Optional joined / associated fields
    private Integer assignedStaffId;
    private String assignedStaffName;
    private String latestRemarks;

    public Complaint() {}

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }

    public int getSafetyRisk() { return safetyRisk; }
    public void setSafetyRisk(int safetyRisk) { this.safetyRisk = safetyRisk; }

    public int getPeopleAffected() { return peopleAffected; }
    public void setPeopleAffected(int peopleAffected) { this.peopleAffected = peopleAffected; }

    public int getOperationalImpact() { return operationalImpact; }
    public void setOperationalImpact(int operationalImpact) { this.operationalImpact = operationalImpact; }

    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }

    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }

    public boolean isSafetyCritical() { return safetyCritical; }
    public void setSafetyCritical(boolean safetyCritical) { this.safetyCritical = safetyCritical; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

    public Integer getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Integer assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public String getAssignedStaffName() { return assignedStaffName; }
    public void setAssignedStaffName(String assignedStaffName) { this.assignedStaffName = assignedStaffName; }

    public String getLatestRemarks() { return latestRemarks; }
    public void setLatestRemarks(String latestRemarks) { this.latestRemarks = latestRemarks; }

    @Override
    public String toString() {
        return "Complaint #" + complaintId + " - " + title + " [" + priority + ", " + status + "]";
    }
}
