// src/app/shared/models/message.model.ts

export interface MessageDto {
  id: number;
  content: string;
  type: MessageType;
  isRead: boolean;
  createdAt: string;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  caseId: number;
  attachmentUrl: string | null;
}

export interface ConversationSummary {
  caseId: number;
  caseTitle: string;
  caseNumber: string;
  lawyerName: string;
  lastMessage: string;
  lastMessageType: MessageType;
  lastMessageAt: string;
  lastMessageIsFromMe: boolean;
  unreadCount: number;
}

export interface SendMessageRequest {
  caseId: number;
  receiverId: number;
  content: string;
  type?: string;
  attachmentUrl?: string;
}

export type MessageType = 'TEXT' | 'FILE' | 'IMAGE' | 'SYSTEM';