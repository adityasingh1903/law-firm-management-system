package com.lawfirm.dto;

import com.lawfirm.entity.Message;
import java.time.LocalDateTime;

public class MessageDto {

    private Long id;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private Long caseId;
    private String attachmentUrl;

    // ── Static factory ────────────────────────────────────────────────────────
    public static MessageDto fromEntity(Message m) {
        MessageDto dto = new MessageDto();
        dto.setId(m.getId());
        dto.setContent(m.getContent());
        dto.setType(m.getType().name());
        dto.setIsRead(m.getIsRead());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setAttachmentUrl(m.getAttachmentUrl());
        if (m.getSender() != null) {
            dto.setSenderId(m.getSender().getId());
            dto.setSenderName(m.getSender().getFirstName() + " " + m.getSender().getLastName());
        }
        if (m.getReceiver() != null) {
            dto.setReceiverId(m.getReceiver().getId());
            dto.setReceiverName(m.getReceiver().getFirstName() + " " + m.getReceiver().getLastName());
        }
        if (m.getCaseEntity() != null) {
            dto.setCaseId(m.getCaseEntity().getId());
        }
        return dto;
    }

    // ── Conversation list summary (one per case) ──────────────────────────────
    public static class ConversationSummary {
        private Long caseId;
        private String caseTitle;
        private String caseNumber;
        private String lawyerName;      // the other participant
        private String lastMessage;     // content preview (truncated)
        private String lastMessageType;
        private LocalDateTime lastMessageAt;
        private boolean lastMessageIsFromMe;
        private long unreadCount;

        public Long getCaseId()                    { return caseId; }
        public void setCaseId(Long caseId)         { this.caseId = caseId; }
        public String getCaseTitle()               { return caseTitle; }
        public void setCaseTitle(String v)         { this.caseTitle = v; }
        public String getCaseNumber()              { return caseNumber; }
        public void setCaseNumber(String v)        { this.caseNumber = v; }
        public String getLawyerName()              { return lawyerName; }
        public void setLawyerName(String v)        { this.lawyerName = v; }
        public String getLastMessage()             { return lastMessage; }
        public void setLastMessage(String v)       { this.lastMessage = v; }
        public String getLastMessageType()         { return lastMessageType; }
        public void setLastMessageType(String v)   { this.lastMessageType = v; }
        public LocalDateTime getLastMessageAt()    { return lastMessageAt; }
        public void setLastMessageAt(LocalDateTime v) { this.lastMessageAt = v; }
        public boolean isLastMessageIsFromMe()     { return lastMessageIsFromMe; }
        public void setLastMessageIsFromMe(boolean v) { this.lastMessageIsFromMe = v; }
        public long getUnreadCount()               { return unreadCount; }
        public void setUnreadCount(long v)         { this.unreadCount = v; }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public String getContent()                 { return content; }
    public void setContent(String content)     { this.content = content; }
    public String getType()                    { return type; }
    public void setType(String type)           { this.type = type; }
    public Boolean getIsRead()                 { return isRead; }
    public void setIsRead(Boolean isRead)      { this.isRead = isRead; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)  { this.createdAt = v; }
    public Long getSenderId()                  { return senderId; }
    public void setSenderId(Long senderId)     { this.senderId = senderId; }
    public String getSenderName()              { return senderName; }
    public void setSenderName(String v)        { this.senderName = v; }
    public Long getReceiverId()                { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getReceiverName()            { return receiverName; }
    public void setReceiverName(String v)      { this.receiverName = v; }
    public Long getCaseId()                    { return caseId; }
    public void setCaseId(Long caseId)         { this.caseId = caseId; }
    public String getAttachmentUrl()           { return attachmentUrl; }
    public void setAttachmentUrl(String v)     { this.attachmentUrl = v; }
}