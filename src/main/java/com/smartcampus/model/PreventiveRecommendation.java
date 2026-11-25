package com.smartcampus.model;

import com.smartcampus.model.enums.RecommendationStatus;

import java.sql.Timestamp;

public class PreventiveRecommendation {
    private int id;
    private int riskAssessmentId;
    private String building;
    private String location;
    private String category;
    private double riskScore;
    private int supportingComplaintCount;
    private int analysisPeriodDays;
    private String urgency;
    private String recommendation;
    private RecommendationStatus status = RecommendationStatus.NEW;
    private Timestamp scheduledDate;
    private Timestamp completedAt;
    private Integer createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public PreventiveRecommendation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRiskAssessmentId() { return riskAssessmentId; }
    public void setRiskAssessmentId(int riskAssessmentId) { this.riskAssessmentId = riskAssessmentId; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public int getSupportingComplaintCount() { return supportingComplaintCount; }
    public void setSupportingComplaintCount(int supportingComplaintCount) { this.supportingComplaintCount = supportingComplaintCount; }

    public int getAnalysisPeriodDays() { return analysisPeriodDays; }
    public void setAnalysisPeriodDays(int analysisPeriodDays) { this.analysisPeriodDays = analysisPeriodDays; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }

    public Timestamp getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(Timestamp scheduledDate) { this.scheduledDate = scheduledDate; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
