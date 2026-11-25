package com.smartcampus.model;

import com.smartcampus.model.enums.EscalationLevel;
import com.smartcampus.model.enums.SlaStatus;

import java.sql.Timestamp;

public class ComplaintEscalation {
    private int id;
    private int complaintId;
    private int escalationLevel;
    private int previousLevel;
    private SlaStatus slaStatus;
    private String escalationReason;
    private Timestamp breachAt;
    private Timestamp createdAt;
    private Timestamp acknowledgedAt;
    private Integer acknowledgedBy;
    private String acknowledgedByName;
    private String interventionNote;

    public ComplaintEscalation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }

    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }

    public EscalationLevel getEscalationLevelEnum() {
        return EscalationLevel.fromLevel(escalationLevel);
    }

    public int getPreviousLevel() { return previousLevel; }
    public void setPreviousLevel(int previousLevel) { this.previousLevel = previousLevel; }

    public SlaStatus getSlaStatus() { return slaStatus; }
    public void setSlaStatus(SlaStatus slaStatus) { this.slaStatus = slaStatus; }

    public String getEscalationReason() { return escalationReason; }
    public void setEscalationReason(String escalationReason) { this.escalationReason = escalationReason; }

    public Timestamp getBreachAt() { return breachAt; }
    public void setBreachAt(Timestamp breachAt) { this.breachAt = breachAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Timestamp acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Integer getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Integer acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public String getAcknowledgedByName() { return acknowledgedByName; }
    public void setAcknowledgedByName(String acknowledgedByName) { this.acknowledgedByName = acknowledgedByName; }

    public String getInterventionNote() { return interventionNote; }
    public void setInterventionNote(String interventionNote) { this.interventionNote = interventionNote; }
}
