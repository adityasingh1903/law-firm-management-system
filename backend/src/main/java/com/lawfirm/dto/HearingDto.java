package com.lawfirm.dto;

import com.lawfirm.entity.Hearing;
import java.time.LocalDateTime;
import java.util.List;

public class HearingDto {

    private Long   id;
    private String title;
    private String description;
    private String hearingDate;   // ISO string for easy JSON serialisation
    private String courtName;
    private String courtRoom;
    private String judgeName;
    private String status;
    private String notes;
    private Long   caseId;
    private String caseNumber;
    private String caseTitle;

    // ── Static factory ────────────────────────────────────────────────────────
    public static HearingDto fromEntity(Hearing h) {
        HearingDto dto = new HearingDto();
        dto.setId(h.getId());
        dto.setTitle(h.getTitle());
        dto.setDescription(h.getDescription());
        dto.setHearingDate(h.getHearingDate() != null ? h.getHearingDate().toString() : null);
        dto.setCourtName(h.getCourtName());
        dto.setCourtRoom(h.getCourtRoom());
        dto.setJudgeName(h.getJudgeName());
        dto.setStatus(h.getStatus() != null ? h.getStatus().name() : null);
        dto.setNotes(h.getNotes());
        if (h.getCaseEntity() != null) {
            dto.setCaseId(h.getCaseEntity().getId());
            dto.setCaseNumber(h.getCaseEntity().getCaseNumber());
            dto.setCaseTitle(h.getCaseEntity().getTitle());
        }
        return dto;
    }

    // ── Inner: grouped response ───────────────────────────────────────────────
    public static class GroupedHearingsDto {
        private List<HearingDto> upcoming;
        private List<HearingDto> past;
        private int totalUpcoming;
        private int totalPast;

        public GroupedHearingsDto(List<HearingDto> upcoming, List<HearingDto> past) {
            this.upcoming      = upcoming;
            this.past          = past;
            this.totalUpcoming = upcoming != null ? upcoming.size() : 0;
            this.totalPast     = past     != null ? past.size()     : 0;
        }

        public List<HearingDto> getUpcoming()   { return upcoming; }
        public List<HearingDto> getPast()        { return past; }
        public int getTotalUpcoming()            { return totalUpcoming; }
        public int getTotalPast()                { return totalPast; }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long   getId()              { return id; }
    public void   setId(Long v)        { this.id = v; }
    public String getTitle()           { return title; }
    public void   setTitle(String v)   { this.title = v; }
    public String getDescription()     { return description; }
    public void   setDescription(String v) { this.description = v; }
    public String getHearingDate()     { return hearingDate; }
    public void   setHearingDate(String v) { this.hearingDate = v; }
    public String getCourtName()       { return courtName; }
    public void   setCourtName(String v)   { this.courtName = v; }
    public String getCourtRoom()       { return courtRoom; }
    public void   setCourtRoom(String v)   { this.courtRoom = v; }
    public String getJudgeName()       { return judgeName; }
    public void   setJudgeName(String v)   { this.judgeName = v; }
    public String getStatus()          { return status; }
    public void   setStatus(String v)  { this.status = v; }
    public String getNotes()           { return notes; }
    public void   setNotes(String v)   { this.notes = v; }
    public Long   getCaseId()          { return caseId; }
    public void   setCaseId(Long v)    { this.caseId = v; }
    public String getCaseNumber()      { return caseNumber; }
    public void   setCaseNumber(String v)  { this.caseNumber = v; }
    public String getCaseTitle()       { return caseTitle; }
    public void   setCaseTitle(String v)   { this.caseTitle = v; }
}