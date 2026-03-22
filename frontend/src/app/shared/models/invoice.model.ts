// src/app/shared/models/invoice.model.ts

export interface InvoiceDto {
  id: number;
  invoiceNumber: string;
  caseId: number;
  caseTitle: string;
  caseNumber: string;
  lawyerName: string | null;
  title: string;
  description: string | null;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  status: InvoiceStatus;
  invoiceType: InvoiceType | null;
  dueDate: string | null;       // ISO date "2026-03-24"
  paidDate: string | null;
  paymentMethod: string | null;
  paymentReference: string | null;
  notes: string | null;
  createdAt: string;
  overdue: boolean;
}

export interface BillingSummary {
  totalDue: number;
  totalPaid: number;
  unpaidCount: number;
  overdueCount: number;
  paidCount: number;
}

export type InvoiceStatus =
  | 'UNPAID'
  | 'PARTIALLY_PAID'
  | 'PAID'
  | 'OVERDUE'
  | 'CANCELLED'
  | 'WAIVED';

export type InvoiceType =
  | 'FEES'
  | 'COURT_FEES'
  | 'CONSULTATION'
  | 'MISCELLANEOUS';

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  UNPAID:          'Unpaid',
  PARTIALLY_PAID:  'Partially Paid',
  PAID:            'Paid',
  OVERDUE:         'Overdue',
  CANCELLED:       'Cancelled',
  WAIVED:          'Waived',
};

export const INVOICE_TYPE_LABELS: Record<InvoiceType, string> = {
  FEES:           'Legal Fees',
  COURT_FEES:     'Court Fees',
  CONSULTATION:   'Consultation',
  MISCELLANEOUS:  'Miscellaneous',
};