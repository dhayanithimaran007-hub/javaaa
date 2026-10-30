package com.smartcampus.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardStats {
    private int totalComplaints;
    private int submittedComplaints;
    private int acknowledgedComplaints;
    private int inProgressComplaints;
    private int resolvedComplaints;
    private int rejectedComplaints;
    private int criticalComplaints;
    private int overdueComplaints;
    private double avgResolutionHours;

    // Category breakdown
    private Map<String, Integer> complaintsByCategory = new LinkedHashMap<>();
    // Status breakdown
    private Map<String, Integer> complaintsByStatus = new LinkedHashMap<>();
    // Priority breakdown
    private Map<String, Integer> complaintsByPriority = new LinkedHashMap<>();

    public DashboardStats() {}

    public int getTotalComplaints() { return totalComplaints; }
    public void setTotalComplaints(int totalComplaints) { this.totalComplaints = totalComplaints; }

    public int getSubmittedComplaints() { return submittedComplaints; }
    public void setSubmittedComplaints(int submittedComplaints) { this.submittedComplaints = submittedComplaints; }

    public int getAcknowledgedComplaints() { return acknowledgedComplaints; }
    public void setAcknowledgedComplaints(int acknowledgedComplaints) { this.acknowledgedComplaints = acknowledgedComplaints; }

    public int getInProgressComplaints() { return inProgressComplaints; }
    public void setInProgressComplaints(int inProgressComplaints) { this.inProgressComplaints = inProgressComplaints; }

    public int getResolvedComplaints() { return resolvedComplaints; }
    public void setResolvedComplaints(int resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }

    public int getRejectedComplaints() { return rejectedComplaints; }
    public void setRejectedComplaints(int rejectedComplaints) { this.rejectedComplaints = rejectedComplaints; }

    public int getCriticalComplaints() { return criticalComplaints; }
    public void setCriticalComplaints(int criticalComplaints) { this.criticalComplaints = criticalComplaints; }

    public int getOverdueComplaints() { return overdueComplaints; }
    public void setOverdueComplaints(int overdueComplaints) { this.overdueComplaints = overdueComplaints; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }

    public Map<String, Integer> getComplaintsByCategory() { return complaintsByCategory; }
    public void setComplaintsByCategory(Map<String, Integer> complaintsByCategory) { this.complaintsByCategory = complaintsByCategory; }

    public Map<String, Integer> getComplaintsByStatus() { return complaintsByStatus; }
    public void setComplaintsByStatus(Map<String, Integer> complaintsByStatus) { this.complaintsByStatus = complaintsByStatus; }

    public Map<String, Integer> getComplaintsByPriority() { return complaintsByPriority; }
    public void setComplaintsByPriority(Map<String, Integer> complaintsByPriority) { this.complaintsByPriority = complaintsByPriority; }
}
