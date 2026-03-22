// src/app/shared/services/billing.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BillingSummary, InvoiceDto } from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class BillingService {

  private base = `${environment.apiUrl}/client/billing`;

  constructor(private http: HttpClient) {}

  /** GET /api/client/billing/summary */
  getSummary(): Observable<BillingSummary> {
    return this.http.get<BillingSummary>(`${this.base}/summary`);
  }

  /** GET /api/client/billing/invoices */
  getMyInvoices(): Observable<InvoiceDto[]> {
    return this.http.get<InvoiceDto[]>(`${this.base}/invoices`);
  }

  /** GET /api/client/billing/invoices/pending */
  getPendingInvoices(): Observable<InvoiceDto[]> {
    return this.http.get<InvoiceDto[]>(`${this.base}/invoices/pending`);
  }

  /** GET /api/client/billing/invoices/:id */
  getInvoiceDetail(id: number): Observable<InvoiceDto> {
    return this.http.get<InvoiceDto>(`${this.base}/invoices/${id}`);
  }

  /** GET /api/client/cases/:caseId/invoices */
  getInvoicesForCase(caseId: number): Observable<InvoiceDto[]> {
    return this.http.get<InvoiceDto[]>(
      `${environment.apiUrl}/client/cases/${caseId}/invoices`);
  }

  // ── Formatting helpers ────────────────────────────────────────────────────

  formatCurrency(amount: number | null): string {
    if (amount == null) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric'
    });
  }

  isDueSoon(iso: string | null): boolean {
    if (!iso) return false;
    const diff = new Date(iso).getTime() - Date.now();
    return diff > 0 && diff < 7 * 86400000; // within 7 days
  }
}