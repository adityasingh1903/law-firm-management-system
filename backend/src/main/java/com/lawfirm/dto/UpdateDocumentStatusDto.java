package com.lawfirm.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDocumentStatusDto {

    @NotBlank(message = "Status is required")
    private String status;   // PENDING_REVIEW | APPROVED | REJECTED | ARCHIVED

    private String rejectionNote;  // optional note when rejecting

    public String getStatus()            { return status; }
    public void setStatus(String v)      { this.status = v; }
    public String getRejectionNote()     { return rejectionNote; }
    public void setRejectionNote(String v) { this.rejectionNote = v; }
}