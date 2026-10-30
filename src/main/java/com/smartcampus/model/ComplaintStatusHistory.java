package com.smartcampus.model;

import com.smartcampus.model.enums.ComplaintStatus;
import java.sql.Timestamp;

public class ComplaintStatusHistory {
    private int historyId;
    private int complaintId;
    private int changedBy;
    private String changedByName;
    private String changedByRole;
    private ComplaintStatus oldStatus;
    private ComplaintStatus newStatus;
    private String remarks;
    private Timestamp changedAt;

    public ComplaintStatusHistory() {}

    public ComplaintStatusHistory(int complaintId, int changedBy, ComplaintStatus oldStatus,
                                  ComplaintStatus newStatus, String remarks) {
        this.complaintId = complaintId;
        this.changedBy = changedBy;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
    }

    public int getHistoryId() { return historyId; }
    public void setHistoryId(int historyId) { this.historyId = historyId; }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }

    public int getChangedBy() { return changedBy; }
    public void setChangedBy(int changedBy) { this.changedBy = changedBy; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public String getChangedByRole() { return changedByRole; }
    public void setChangedByRole(String changedByRole) { this.changedByRole = changedByRole; }

    public ComplaintStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(ComplaintStatus oldStatus) { this.oldStatus = oldStatus; }

    public ComplaintStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ComplaintStatus newStatus) { this.newStatus = newStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Timestamp getChangedAt() { return changedAt; }
    public void setChangedAt(Timestamp changedAt) { this.changedAt = changedAt; }
}
