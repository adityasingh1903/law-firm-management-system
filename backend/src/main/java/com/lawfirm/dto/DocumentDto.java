package com.lawfirm.dto;

import com.lawfirm.entity.Document;
import java.time.LocalDateTime;

public class DocumentDto {

    private Long   id;
    private String title;
    private String description;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long   fileSize;
    private String documentType;
    private String status;
    private LocalDateTime uploadedAt;
    private String uploadedByName;

    // Case info — populated for context
    private Long   caseId;
    private String caseTitle;
    private String caseNumber;

    // ── Static factory ────────────────────────────────────────────────────────
    public static DocumentDto fromEntity(Document d) {
        DocumentDto dto = new DocumentDto();
        dto.setId(d.getId());
        dto.setTitle(d.getTitle());
        dto.setDescription(d.getDescription());
        dto.setFileName(d.getFileName());
        dto.setFilePath(d.getFilePath());
        dto.setFileType(d.getFileType());
        dto.setFileSize(d.getFileSize());
        dto.setDocumentType(d.getDocumentType() != null ? d.getDocumentType().name() : null);
        dto.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        dto.setUploadedAt(d.getUploadedAt());

        if (d.getUploadedByUser() != null)
            dto.setUploadedByName(
                d.getUploadedByUser().getFirstName() + " " + d.getUploadedByUser().getLastName());

        if (d.getCaseEntity() != null) {
            dto.setCaseId(d.getCaseEntity().getId());
            dto.setCaseTitle(d.getCaseEntity().getTitle());
            dto.setCaseNumber(d.getCaseEntity().getCaseNumber());
        }
        return dto;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long   getId()                      { return id; }
    public void   setId(Long id)               { this.id = id; }
    public String getTitle()                   { return title; }
    public void   setTitle(String v)           { this.title = v; }
    public String getDescription()             { return description; }
    public void   setDescription(String v)     { this.description = v; }
    public String getFileName()                { return fileName; }
    public void   setFileName(String v)        { this.fileName = v; }
    public String getFilePath()                { return filePath; }
    public void   setFilePath(String v)        { this.filePath = v; }
    public String getFileType()                { return fileType; }
    public void   setFileType(String v)        { this.fileType = v; }
    public Long   getFileSize()                { return fileSize; }
    public void   setFileSize(Long v)          { this.fileSize = v; }
    public String getDocumentType()            { return documentType; }
    public void   setDocumentType(String v)    { this.documentType = v; }
    public String getStatus()                  { return status; }
    public void   setStatus(String v)          { this.status = v; }
    public LocalDateTime getUploadedAt()       { return uploadedAt; }
    public void   setUploadedAt(LocalDateTime v) { this.uploadedAt = v; }
    public String getUploadedByName()          { return uploadedByName; }
    public void   setUploadedByName(String v)  { this.uploadedByName = v; }
    public Long   getCaseId()                  { return caseId; }
    public void   setCaseId(Long v)            { this.caseId = v; }
    public String getCaseTitle()               { return caseTitle; }
    public void   setCaseTitle(String v)       { this.caseTitle = v; }
    public String getCaseNumber()              { return caseNumber; }
    public void   setCaseNumber(String v)      { this.caseNumber = v; }
}