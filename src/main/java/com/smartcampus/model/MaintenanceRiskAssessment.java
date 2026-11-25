package com.smartcampus.model;

import com.smartcampus.model.enums.RiskLevel;

import java.sql.Timestamp;

public class MaintenanceRiskAssessment {
    private int id;
    private int analysisId;
    private String building;
    private String location;
    private String category;
    private int incidentCount;
    private double riskScore;
    private RiskLevel riskLevel;
    private String contributingFactors;
    private String recommendation;
    private Timestamp createdAt;

    public MaintenanceRiskAssessment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAnalysisId() { return analysisId; }
    public void setAnalysisId(int analysisId) { this.analysisId = analysisId; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getIncidentCount() { return incidentCount; }
    public void setIncidentCount(int incidentCount) { this.incidentCount = incidentCount; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(String contributingFactors) { this.contributingFactors = contributingFactors; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
