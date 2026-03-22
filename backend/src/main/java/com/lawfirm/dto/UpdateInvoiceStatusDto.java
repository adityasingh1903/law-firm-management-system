package com.lawfirm.dto;
 
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
 
public class UpdateInvoiceStatusDto {
 
    @NotBlank(message = "Status is required")
    private String status;         // PAID | UNPAID | PARTIALLY_PAID | OVERDUE | CANCELLED | WAIVED
 
    private Double paidAmount;     // required when status = PARTIALLY_PAID or PAID
    private LocalDate paidDate;    // defaults to today when status = PAID
    private String paymentMethod;  // CASH | BANK_TRANSFER | UPI | CHEQUE
    private String paymentReference;
 
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public Double getPaidAmount() { return paidAmount; } public void setPaidAmount(Double v) { this.paidAmount = v; }
    public LocalDate getPaidDate() { return paidDate; } public void setPaidDate(LocalDate v) { this.paidDate = v; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getPaymentReference() { return paymentReference; } public void setPaymentReference(String v) { this.paymentReference = v; }
}