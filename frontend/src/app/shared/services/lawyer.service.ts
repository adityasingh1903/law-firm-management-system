// src/app/shared/services/lawyer.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LawyerDashboardDto } from '../models/lawyer-dashboard.model';

@Injectable({ providedIn: 'root' })
export class LawyerService {

  private base = `${environment.apiUrl}/lawyer`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<LawyerDashboardDto> {
    return this.http.get<LawyerDashboardDto>(`${this.base}/dashboard`);
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/messages/unread-count`);
  }

  formatCurrency(amount: number | null): string {
    if (amount == null) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(amount);
  }
}