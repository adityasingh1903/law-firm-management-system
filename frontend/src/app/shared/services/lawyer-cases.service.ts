// src/app/shared/services/lawyer-cases.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LawyerCaseDto, CaseRequestDto, AcceptRequestForm
} from '../models/lawyer-cases.model';

@Injectable({ providedIn: 'root' })
export class LawyerCasesService {

  private base = `${environment.apiUrl}/lawyer`;

  constructor(private http: HttpClient) {}

  // ── My Cases ──────────────────────────────────────────────────────────────
  getMyCases(): Observable<LawyerCaseDto[]> {
    return this.http.get<LawyerCaseDto[]>(`${this.base}/cases`);
  }

  getCaseDetail(id: number): Observable<LawyerCaseDto> {
    return this.http.get<LawyerCaseDto>(`${this.base}/cases/${id}`);
  }

  updateStatus(caseId: number, status: string): Observable<LawyerCaseDto> {
    return this.http.patch<LawyerCaseDto>(`${this.base}/cases/${caseId}/status`, { status });
  }

  updateNotes(caseId: number, notes: string): Observable<LawyerCaseDto> {
    return this.http.patch<LawyerCaseDto>(`${this.base}/cases/${caseId}/notes`, { notes });
  }

  // ── Case Requests ─────────────────────────────────────────────────────────
  getPendingRequests(caseType?: string): Observable<CaseRequestDto[]> {
    const params = caseType ? new HttpParams().set('caseType', caseType) : undefined;
    return this.http.get<CaseRequestDto[]>(`${this.base}/requests`, { params });
  }

  getHandledRequests(): Observable<CaseRequestDto[]> {
    return this.http.get<CaseRequestDto[]>(`${this.base}/requests/handled`);
  }

  getPendingCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/requests/count`);
  }

  acceptRequest(requestId: number, form: AcceptRequestForm): Observable<LawyerCaseDto> {
    return this.http.post<LawyerCaseDto>(`${this.base}/requests/${requestId}/accept`, form);
  }

  rejectRequest(requestId: number, reason: string): Observable<CaseRequestDto> {
    return this.http.post<CaseRequestDto>(`${this.base}/requests/${requestId}/reject`, { reason });
  }

  // ── Formatting ────────────────────────────────────────────────────────────
  formatCurrency(n: number | null): string {
    if (!n) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(n);
  }
}