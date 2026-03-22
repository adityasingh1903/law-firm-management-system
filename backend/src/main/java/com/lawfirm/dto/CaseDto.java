package com.lawfirm.dto;

import java.time.LocalDateTime;
import com.lawfirm.entity.Case;

public class CaseDto {
    private Long id;
    private String caseNumber;
    private String title;
    private String description;
    private String caseType;
    private String status;
    private LocalDateTime dateOpened;
    private LocalDateTime dateClosed;
    private LocalDateTime nextHearingDate;
    private String courtName;
    private String clientName;
    private Long   clientId;     // ← NEW: needed for lawyer messaging
    private String lawyerName;
    private Long   lawyerId;     // ← NEW: for consistency
    private Double feesCharged;

    public static CaseDto fromEntity(Case c) {
        CaseDto dto = new CaseDto();
        dto.setId(c.getId());
        dto.setCaseNumber(c.getCaseNumber());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCaseType(c.getCaseType() != null ? c.getCaseType().name() : null);
        dto.setStatus(c.getStatus().name());
        dto.setDateOpened(c.getDateOpened());
        dto.setDateClosed(c.getDateClosed());
        dto.setNextHearingDate(c.getNextHearingDate());
        dto.setCourtName(c.getCourtName());
        dto.setFeesCharged(c.getFeesCharged());
        if (c.getClient() != null) {
            dto.setClientId(c.getClient().getId());                  // ← NEW
            dto.setClientName(c.getClient().getFirstName() + " " + c.getClient().getLastName());
        }
        if (c.getAssignedLawyer() != null) {
            dto.setLawyerId(c.getAssignedLawyer().getId());          // ← NEW
            dto.setLawyerName(c.getAssignedLawyer().getFirstName() + " " + c.getAssignedLawyer().getLastName());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDateOpened() { return dateOpened; }
    public void setDateOpened(LocalDateTime dateOpened) { this.dateOpened = dateOpened; }
    public LocalDateTime getDateClosed() { return dateClosed; }
    public void setDateClosed(LocalDateTime dateClosed) { this.dateClosed = dateClosed; }
    public LocalDateTime getNextHearingDate() { return nextHearingDate; }
    public void setNextHearingDate(LocalDateTime nextHearingDate) { this.nextHearingDate = nextHearingDate; }
    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Long getClientId() { return clientId; }                   // ← NEW
    public void setClientId(Long clientId) { this.clientId = clientId; } // ← NEW
    public String getLawyerName() { return lawyerName; }
    public void setLawyerName(String lawyerName) { this.lawyerName = lawyerName; }
    public Long getLawyerId() { return lawyerId; }                   // ← NEW
    public void setLawyerId(Long lawyerId) { this.lawyerId = lawyerId; } // ← NEW
    public Double getFeesCharged() { return feesCharged; }
    public void setFeesCharged(Double feesCharged) { this.feesCharged = feesCharged; }
}