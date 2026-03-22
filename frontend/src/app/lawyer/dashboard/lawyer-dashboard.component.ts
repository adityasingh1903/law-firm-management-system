// src/app/lawyer/dashboard/lawyer-dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LawyerService } from '../../shared/services/lawyer.service';
import { LawyerDashboardDto, CaseSummary } from '../../shared/models/lawyer-dashboard.model';
import { HearingDto } from '../../shared/models/hearing.model';
import { ConversationSummary } from '../../shared/models/message.model';

@Component({
  selector: 'app-lawyer-dashboard',
  templateUrl: './lawyer-dashboard.component.html',
  styleUrls: ['./lawyer-dashboard.component.scss']
})
export class LawyerDashboardComponent implements OnInit {

  dashboard: LawyerDashboardDto | null = null;
  loading = true;
  error   = '';

  userName    = '';
  userInitials = '';

  constructor(
    private lawyerService: LawyerService,
    public router: Router
  ) {}

  ngOnInit(): void {
    const firstName = localStorage.getItem('firstName') || '';
    const lastName  = localStorage.getItem('lastName')  || '';
    this.userName    = firstName ? `${firstName} ${lastName}`.trim() : 'Lawyer';
    this.userInitials = `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase() || 'LW';

    this.lawyerService.getDashboard().subscribe({
      next:  d  => { this.dashboard = d; this.loading = false; },
      error: () => { this.error = 'Failed to load dashboard.'; this.loading = false; }
    });
  }

  // ── Navigation helpers ────────────────────────────────────────────────────
  goToCases()    { this.router.navigate(['/lawyer/cases']); }
  goToHearings() { this.router.navigate(['/lawyer/hearings']); }
  goToMessages() { this.router.navigate(['/lawyer/messages']); }
  goToDocuments() { this.router.navigate(['/lawyer/documents']); }
  goToClients()  { this.router.navigate(['/lawyer/clients']); }
  goToBilling()  { this.router.navigate(['/lawyer/billing']); }

  openCase(id: number)        { this.router.navigate(['/lawyer/cases', id]); }
  openConversation(caseId: number) { this.router.navigate(['/lawyer/messages'], { queryParams: { caseId } }); }

  // ── Formatting ────────────────────────────────────────────────────────────
  formatCurrency(n: number | null): string {
    return this.lawyerService.formatCurrency(n);
  }

  getDay(iso: string): string {
    return new Date(iso).getDate().toString().padStart(2, '0');
  }

  getMonth(iso: string): string {
    return new Date(iso).toLocaleString('en-IN', { month: 'short' }).toUpperCase();
  }

  getTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }

  daysUntil(iso: string): number {
    return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000);
  }

  formatRelative(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1)   return 'Just now';
    if (mins < 60)  return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24)   return `${hrs}h ago`;
    return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      OPEN:        'badge--open',
      IN_PROGRESS: 'badge--progress',
      CLOSED:      'badge--closed',
      SETTLED:     'badge--settled',
      DISMISSED:   'badge--dismissed',
    };
    return map[status] ?? 'badge--open';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'Open', IN_PROGRESS: 'In Progress',
      CLOSED: 'Closed', SETTLED: 'Settled', DISMISSED: 'Dismissed'
    };
    return map[status] ?? status;
  }

  hearingStatusClass(s: string): string {
    return s === 'SCHEDULED' ? 'hs--scheduled'
         : s === 'COMPLETED' ? 'hs--completed'
         : s === 'POSTPONED' ? 'hs--postponed'
         : 'hs--cancelled';
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  }
}