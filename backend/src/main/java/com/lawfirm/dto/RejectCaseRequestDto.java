package com.lawfirm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectCaseRequestDto {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500)
    private String reason;

    public String getReason()        { return reason; }
    public void setReason(String v)  { this.reason = v; }
}