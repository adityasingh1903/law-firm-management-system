package com.lawfirm.dto;
 
import com.lawfirm.entity.Invoice;
import java.time.LocalDate;
import java.time.LocalDateTime;
 
public class InvoiceDto {
 
    private Long          id;
    private String        invoiceNumber;
    private String        title;
    private String        description;
    private String        invoiceType;      // FEES | COURT_FEES | CONSULTATION | MISCELLANEOUS
    private String        status;           // UNPAID | PARTIALLY_PAID | PAID | OVERDUE | CANCELLED | WAIVED
    private Double        amount;           // base amount before tax
    private Double        taxAmount;
    private Double        totalAmount;      // amount + taxAmount
    private Double        paidAmount;
    private Double        balanceDue;       // totalAmount - paidAmount
    private LocalDate     dueDate;
    private LocalDate     paidDate;
    private String        paymentMethod;
    private String        paymentReference;
    private String        notes;
    private LocalDateTime createdAt;
 
    // Case info
    private Long   caseId;
    private String caseNumber;
    private String caseTitle;
 
    // Client info
    private Long   clientId;
    private String clientName;
    private String clientEmail;
 
    // Lawyer info
    private Long   lawyerId;
    private String lawyerName;
 
    // ── Static factory ────────────────────────────────────────────────────────
    public static InvoiceDto fromEntity(Invoice inv) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(inv.getId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setTitle(inv.getTitle());
        dto.setDescription(inv.getDescription());
        dto.setInvoiceType(inv.getInvoiceType() != null ? inv.getInvoiceType().name() : null);
        dto.setStatus(inv.getStatus().name());
        dto.setAmount(inv.getAmount());
        dto.setTaxAmount(inv.getTaxAmount());
        dto.setTotalAmount(inv.getTotalAmount());
        dto.setPaidAmount(inv.getPaidAmount());
        dto.setBalanceDue(inv.getBalanceDue());
        dto.setDueDate(inv.getDueDate());
        dto.setPaidDate(inv.getPaidDate());
        dto.setPaymentMethod(inv.getPaymentMethod());
        dto.setPaymentReference(inv.getPaymentReference());
        dto.setNotes(inv.getNotes());
        dto.setCreatedAt(inv.getCreatedAt());
 
        if (inv.getCaseEntity() != null) {
            dto.setCaseId(inv.getCaseEntity().getId());
            dto.setCaseNumber(inv.getCaseEntity().getCaseNumber());
            dto.setCaseTitle(inv.getCaseEntity().getTitle());
        }
        if (inv.getClient() != null) {
            dto.setClientId(inv.getClient().getId());
            dto.setClientName(inv.getClient().getFirstName() + " " + inv.getClient().getLastName());
            dto.setClientEmail(inv.getClient().getEmail());
        }
        if (inv.getLawyer() != null) {
            dto.setLawyerId(inv.getLawyer().getId());
            dto.setLawyerName(inv.getLawyer().getFirstName() + " " + inv.getLawyer().getLastName());
        }
        return dto;
    }
 
    // ── Nested: BillingSummary ────────────────────────────────────────────────
    public static class BillingSummary {
        private double totalDue;
        private double totalPaid;
        private long   unpaidCount;
        private long   overdueCount;
        private long   paidCount;
        private long   partialCount;
 
        public BillingSummary(double totalDue, double totalPaid,
                              long unpaidCount, long overdueCount,
                              long paidCount, long partialCount) {
            this.totalDue     = totalDue;
            this.totalPaid    = totalPaid;
            this.unpaidCount  = unpaidCount;
            this.overdueCount = overdueCount;
            this.paidCount    = paidCount;
            this.partialCount = partialCount;
        }
 
        public double getTotalDue()     { return totalDue; }
        public double getTotalPaid()    { return totalPaid; }
        public long   getUnpaidCount()  { return unpaidCount; }
        public long   getOverdueCount() { return overdueCount; }
        public long   getPaidCount()    { return paidCount; }
        public long   getPartialCount() { return partialCount; }
    }
 
    // ── Getters and Setters ───────────────────────────────────────────────────
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String v) { this.invoiceNumber = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getInvoiceType() { return invoiceType; } public void setInvoiceType(String v) { this.invoiceType = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public Double getAmount() { return amount; } public void setAmount(Double v) { this.amount = v; }
    public Double getTaxAmount() { return taxAmount; } public void setTaxAmount(Double v) { this.taxAmount = v; }
    public Double getTotalAmount() { return totalAmount; } public void setTotalAmount(Double v) { this.totalAmount = v; }
    public Double getPaidAmount() { return paidAmount; } public void setPaidAmount(Double v) { this.paidAmount = v; }
    public Double getBalanceDue() { return balanceDue; } public void setBalanceDue(Double v) { this.balanceDue = v; }
    public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { this.dueDate = v; }
    public LocalDate getPaidDate() { return paidDate; } public void setPaidDate(LocalDate v) { this.paidDate = v; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getPaymentReference() { return paymentReference; } public void setPaymentReference(String v) { this.paymentReference = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public Long getCaseId() { return caseId; } public void setCaseId(Long v) { this.caseId = v; }
    public String getCaseNumber() { return caseNumber; } public void setCaseNumber(String v) { this.caseNumber = v; }
    public String getCaseTitle() { return caseTitle; } public void setCaseTitle(String v) { this.caseTitle = v; }
    public Long getClientId() { return clientId; } public void setClientId(Long v) { this.clientId = v; }
    public String getClientName() { return clientName; } public void setClientName(String v) { this.clientName = v; }
    public String getClientEmail() { return clientEmail; } public void setClientEmail(String v) { this.clientEmail = v; }
    public Long getLawyerId() { return lawyerId; } public void setLawyerId(Long v) { this.lawyerId = v; }
    public String getLawyerName() { return lawyerName; } public void setLawyerName(String v) { this.lawyerName = v; }
}