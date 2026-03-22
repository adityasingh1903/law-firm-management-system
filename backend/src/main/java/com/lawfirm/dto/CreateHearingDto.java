package com.lawfirm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateHearingDto {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull(message = "Hearing date is required")
    private LocalDateTime hearingDate;

    @Size(max = 200)
    private String courtName;

    @Size(max = 100)
    private String courtRoom;

    @Size(max = 200)
    private String judgeName;

    @Size(max = 1000)
    private String notes;

    public Long getCaseId()              { return caseId; }
    public void setCaseId(Long v)        { this.caseId = v; }
    public String getTitle()             { return title; }
    public void setTitle(String v)       { this.title = v; }
    public String getDescription()       { return description; }
    public void setDescription(String v) { this.description = v; }
    public LocalDateTime getHearingDate()        { return hearingDate; }
    public void setHearingDate(LocalDateTime v)  { this.hearingDate = v; }
    public String getCourtName()         { return courtName; }
    public void setCourtName(String v)   { this.courtName = v; }
    public String getCourtRoom()         { return courtRoom; }
    public void setCourtRoom(String v)   { this.courtRoom = v; }
    public String getJudgeName()         { return judgeName; }
    public void setJudgeName(String v)   { this.judgeName = v; }
    public String getNotes()             { return notes; }
    public void setNotes(String v)       { this.notes = v; }
}