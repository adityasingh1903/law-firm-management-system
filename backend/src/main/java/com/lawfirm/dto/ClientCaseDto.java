package com.lawfirm.dto;

import com.lawfirm.entity.Case;
import java.time.LocalDateTime;

public class ClientCaseDto {
    private Long id;
    private String caseNumber;
    private String title;
    private String description;
    private String caseType;
    private String status;
    private LocalDateTime dateOpened;
    private LocalDateTime nextHearingDate;
    private String courtName;
    private String judgeName;
    private String opposingCounsel;
    private Double feesCharged;
    private Double settlementAmount;
    private String lawyerName;
    private String lawyerEmail;
    private String lawyerPhone;
    private String lawyerSpecialization;
    private Long   lawyerId;          // ← NEW: needed for messaging
    private int documentCount;
    private long unreadMessages;

    public static ClientCaseDto fromEntity(Case c, long unreadMessages) {
        ClientCaseDto dto = new ClientCaseDto();
        dto.setId(c.getId());
        dto.setCaseNumber(c.getCaseNumber());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCaseType(c.getCaseType() != null ? c.getCaseType().name() : null);
        dto.setStatus(c.getStatus().name());
        dto.setDateOpened(c.getDateOpened());
        dto.setNextHearingDate(c.getNextHearingDate());
        dto.setCourtName(c.getCourtName());
        dto.setJudgeName(c.getJudgeName());
        dto.setOpposingCounsel(c.getOpposingCounsel());
        dto.setFeesCharged(c.getFeesCharged());
        dto.setSettlementAmount(c.getSettlementAmount());
        dto.setDocumentCount(c.getDocuments() != null ? c.getDocuments().size() : 0);
        dto.setUnreadMessages(unreadMessages);

        if (c.getAssignedLawyer() != null) {
            dto.setLawyerId(c.getAssignedLawyer().getId());          // ← NEW
            dto.setLawyerName(c.getAssignedLawyer().getFirstName() + " " + c.getAssignedLawyer().getLastName());
            dto.setLawyerEmail(c.getAssignedLawyer().getEmail());
            dto.setLawyerPhone(c.getAssignedLawyer().getPhoneNumber());
            dto.setLawyerSpecialization(c.getAssignedLawyer().getSpecialization());
        }
        return dto;
    }

    // --- Getters and Setters ---
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
    public LocalDateTime getNextHearingDate() { return nextHearingDate; }
    public void setNextHearingDate(LocalDateTime nextHearingDate) { this.nextHearingDate = nextHearingDate; }
    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }
    public String getJudgeName() { return judgeName; }
    public void setJudgeName(String judgeName) { this.judgeName = judgeName; }
    public String getOpposingCounsel() { return opposingCounsel; }
    public void setOpposingCounsel(String opposingCounsel) { this.opposingCounsel = opposingCounsel; }
    public Double getFeesCharged() { return feesCharged; }
    public void setFeesCharged(Double feesCharged) { this.feesCharged = feesCharged; }
    public Double getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(Double settlementAmount) { this.settlementAmount = settlementAmount; }
    public Long getLawyerId() { return lawyerId; }                   // ← NEW
    public void setLawyerId(Long lawyerId) { this.lawyerId = lawyerId; } // ← NEW
    public String getLawyerName() { return lawyerName; }
    public void setLawyerName(String lawyerName) { this.lawyerName = lawyerName; }
    public String getLawyerEmail() { return lawyerEmail; }
    public void setLawyerEmail(String lawyerEmail) { this.lawyerEmail = lawyerEmail; }
    public String getLawyerPhone() { return lawyerPhone; }
    public void setLawyerPhone(String lawyerPhone) { this.lawyerPhone = lawyerPhone; }
    public String getLawyerSpecialization() { return lawyerSpecialization; }
    public void setLawyerSpecialization(String lawyerSpecialization) { this.lawyerSpecialization = lawyerSpecialization; }
    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
    public long getUnreadMessages() { return unreadMessages; }
    public void setUnreadMessages(long unreadMessages) { this.unreadMessages = unreadMessages; }
}