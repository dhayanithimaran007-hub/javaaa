package com.smartcampus.model;

import java.sql.Timestamp;

public class PredictiveAnalysis {
    private int id;
    private Timestamp analysisPeriodStart;
    private Timestamp analysisPeriodEnd;
    private Timestamp analysisTimestamp;
    private int totalComplaintsAnalyzed;
    private String analysisSummary;
    private Integer generatedBy;
    private String generatedByName;

    public PredictiveAnalysis() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Timestamp getAnalysisPeriodStart() { return analysisPeriodStart; }
    public void setAnalysisPeriodStart(Timestamp analysisPeriodStart) { this.analysisPeriodStart = analysisPeriodStart; }

    public Timestamp getAnalysisPeriodEnd() { return analysisPeriodEnd; }
    public void setAnalysisPeriodEnd(Timestamp analysisPeriodEnd) { this.analysisPeriodEnd = analysisPeriodEnd; }

    public Timestamp getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(Timestamp analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public int getTotalComplaintsAnalyzed() { return totalComplaintsAnalyzed; }
    public void setTotalComplaintsAnalyzed(int totalComplaintsAnalyzed) { this.totalComplaintsAnalyzed = totalComplaintsAnalyzed; }

    public String getAnalysisSummary() { return analysisSummary; }
    public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }

    public Integer getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Integer generatedBy) { this.generatedBy = generatedBy; }

    public String getGeneratedByName() { return generatedByName; }
    public void setGeneratedByName(String generatedByName) { this.generatedByName = generatedByName; }
}
