package com.lawfirm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body sent by lawyer when accepting a case request.
 * Creates a new Case from the CaseRequest.
 */
public class AcceptCaseRequestDto {

    @NotBlank(message = "Case number is required")
    @Size(max = 50)
    private String caseNumber;   // lawyer assigns the case number

    private String courtName;
    private String judgeName;
    private Double feesCharged;
    private String notes;        // optional initial notes

    public String getCaseNumber()    { return caseNumber; }
    public void setCaseNumber(String v)  { this.caseNumber = v; }
    public String getCourtName()     { return courtName; }
    public void setCourtName(String v)   { this.courtName = v; }
    public String getJudgeName()     { return judgeName; }
    public void setJudgeName(String v)   { this.judgeName = v; }
    public Double getFeesCharged()   { return feesCharged; }
    public void setFeesCharged(Double v) { this.feesCharged = v; }
    public String getNotes()         { return notes; }
    public void setNotes(String v)       { this.notes = v; }
}