package com.lawfirm.dto;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
 
public class CreateInvoiceDto {
 
    @NotNull(message = "Case ID is required")
    private Long caseId;
 
    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;
 
    private String description;
 
    private String invoiceType = "FEES";   // FEES | COURT_FEES | CONSULTATION | MISCELLANEOUS
 
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
 
    private Double taxAmount = 0.0;        // flat tax amount in ₹ (not %)
 
    private LocalDate dueDate;             // defaults to today + 30 days if null
 
    @Size(max = 500)
    private String notes;
 
    public Long getCaseId() { return caseId; } public void setCaseId(Long v) { this.caseId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getInvoiceType() { return invoiceType; } public void setInvoiceType(String v) { this.invoiceType = v; }
    public Double getAmount() { return amount; } public void setAmount(Double v) { this.amount = v; }
    public Double getTaxAmount() { return taxAmount; } public void setTaxAmount(Double v) { this.taxAmount = v; }
    public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
}