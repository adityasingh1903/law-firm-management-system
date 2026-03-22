// src/app/shared/models/document.model.ts

export interface DocumentDto {
  id: number;
  title: string;
  description: string | null;
  fileName: string;
  filePath: string;
  fileType: string | null;
  fileSize: number | null;
  documentType: DocumentType | null;
  status: DocumentStatus | null;
  uploadedAt: string;
  uploadedByName: string | null;
  caseId?: number;
  caseTitle?: string;
  caseNumber?: string;
}

export type DocumentType =
  | 'CONTRACT'
  | 'EVIDENCE'
  | 'COURT_FILING'
  | 'CORRESPONDENCE'
  | 'RESEARCH'
  | 'INVOICE'
  | 'OTHER';

export type DocumentStatus =
  | 'UPLOADED'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'ARCHIVED';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  CONTRACT:       'Contract',
  EVIDENCE:       'Evidence',
  COURT_FILING:   'Court Filing',
  CORRESPONDENCE: 'Correspondence',
  RESEARCH:       'Research',
  INVOICE:        'Invoice',
  OTHER:          'Other',
};

export const DOCUMENT_STATUS_LABELS: Record<DocumentStatus, string> = {
  UPLOADED:       'Uploaded',
  PENDING_REVIEW: 'Pending Review',
  APPROVED:       'Approved',
  REJECTED:       'Rejected',
  ARCHIVED:       'Archived',
};