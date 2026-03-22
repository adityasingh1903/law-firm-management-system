// src/app/shared/models/lawyer-cases.model.ts

export interface LawyerCaseDto {
  id:              number;
  caseNumber:      string;
  title:           string;
  description:     string | null;
  caseType:        string | null;
  status:          CaseStatus;
  dateOpened:      string | null;
  dateClosed:      string | null;
  nextHearingDate: string | null;
  courtName:       string | null;
  judgeName:       string | null;
  opposingCounsel: string | null;
  feesCharged:     number | null;
  settlementAmount:number | null;
  clientId:        number | null;
  clientName:      string | null;
  clientEmail:     string | null;
  updatedAt:       string;
}

export interface CaseRequestDto {
  id:              number;
  title:           string;
  description:     string;
  caseType:        string | null;
  urgency:         UrgencyLevel;
  status:          RequestStatus;
  rejectionReason: string | null;
  createdAt:       string;
  updatedAt:       string;
  resolvedAt:      string | null;
  clientId:        number;
  clientName:      string;
  lawyerId:        number | null;
  lawyerName:      string | null;
  createdCaseId:   number | null;
}

export interface AcceptRequestForm {
  caseNumber:  string;
  courtName:   string;
  judgeName:   string;
  feesCharged: number | null;
  notes:       string;
}

export type CaseStatus    = 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'SETTLED' | 'DISMISSED' | 'APPEALED';
export type UrgencyLevel  = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export const CASE_STATUS_LABELS: Record<CaseStatus, string> = {
  OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Closed',
  SETTLED: 'Settled', DISMISSED: 'Dismissed', APPEALED: 'Appealed'
};

export const CASE_TYPE_LABELS: Record<string, string> = {
  CRIMINAL: 'Criminal', CIVIL: 'Civil', FAMILY: 'Family',
  CORPORATE: 'Corporate', REAL_ESTATE: 'Real Estate',
  IMMIGRATION: 'Immigration', TAX: 'Tax', LABOR: 'Labor',
  INTELLECTUAL_PROPERTY: 'Intellectual Property', OTHER: 'Other'
};