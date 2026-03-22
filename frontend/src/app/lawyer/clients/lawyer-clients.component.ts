// src/app/lawyer/clients/lawyer-clients.component.ts

import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface ClientDto {
  id:          number;
  username:    string;
  email:       string;
  firstName:   string;
  lastName:    string;
  phoneNumber: string | null;
  address:     string | null;
  status:      string;
  roles:       string[];
}

interface CaseDto {
  id:          number;
  caseNumber:  string;
  title:       string;
  status:      string;
  caseType:    string | null;
  dateOpened:  string | null;
  courtName:   string | null;
  feesCharged: number | null;
  clientName:  string | null;
  clientId:    number | null;
}

@Component({
  selector: 'app-lawyer-clients',
  templateUrl: './lawyer-clients.component.html',
  styleUrls: ['./lawyer-clients.component.scss']
})
export class LawyerClientsComponent implements OnInit {

  // ── Data ──────────────────────────────────────────────────────────────────
  clients:         ClientDto[] = [];
  filteredClients: ClientDto[] = [];
  allCases:        CaseDto[]   = [];
  loading  = true;
  error    = '';

  // ── Detail panel ──────────────────────────────────────────────────────────
  selectedClient: ClientDto | null = null;
  clientCases:    CaseDto[]        = [];

  // ── Filters ───────────────────────────────────────────────────────────────
  searchKeyword = '';

  private base = `${environment.apiUrl}/lawyer`;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.loadClients();
    this.loadCases();
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadClients(): void {
    this.loading = true;
    this.http.get<ClientDto[]>(`${this.base}/clients`).subscribe({
      next: clients => {
        this.clients         = clients;
        this.filteredClients = clients;
        this.loading         = false;
      },
      error: () => { this.error = 'Failed to load clients.'; this.loading = false; }
    });
  }

  loadCases(): void {
    this.http.get<CaseDto[]>(`${this.base}/cases`).subscribe({
      next: cases => this.allCases = cases,
      error: () => {}
    });
  }

  // ── Filter ────────────────────────────────────────────────────────────────
  onSearch(): void {
    const kw = this.searchKeyword.toLowerCase();
    this.filteredClients = this.clients.filter(c =>
      !kw ||
      (c.firstName + ' ' + c.lastName).toLowerCase().includes(kw) ||
      c.email.toLowerCase().includes(kw) ||
      (c.phoneNumber ?? '').includes(kw)
    );
  }

  clearSearch(): void {
    this.searchKeyword   = '';
    this.filteredClients = this.clients;
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(client: ClientDto): void {
    this.selectedClient = client;
    this.clientCases    = this.allCases.filter(c => c.clientId === client.id);
  }

  closeDetail(): void {
    this.selectedClient = null;
    this.clientCases    = [];
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  openCase(caseId: number): void {
    this.router.navigate(['/lawyer/cases'], { queryParams: { id: caseId } });
  }

  messageClient(clientId: number): void {
    // Find a case for this client and open the messages page with it
    const c = this.allCases.find(c => c.clientId === clientId);
    if (c) this.router.navigate(['/lawyer/messages'], { queryParams: { caseId: c.id } });
    else   this.router.navigate(['/lawyer/messages']);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  getFullName(c: ClientDto): string {
    return `${c.firstName} ${c.lastName}`.trim();
  }

  getInitials(c: ClientDto): string {
    return `${c.firstName.charAt(0)}${c.lastName.charAt(0)}`.toUpperCase();
  }

  getCaseCountForClient(clientId: number): number {
    return this.allCases.filter(c => c.clientId === clientId).length;
  }

  getActiveCaseCount(clientId: number): number {
    return this.allCases.filter(c =>
      c.clientId === clientId &&
      (c.status === 'OPEN' || c.status === 'IN_PROGRESS')
    ).length;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'badge--open', IN_PROGRESS: 'badge--progress',
      CLOSED: 'badge--closed', SETTLED: 'badge--settled', DISMISSED: 'badge--dismissed'
    };
    return map[s] ?? 'badge--open';
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Closed',
      SETTLED: 'Settled', DISMISSED: 'Dismissed'
    };
    return map[s] ?? s;
  }

  formatCurrency(n: number | null): string {
    if (!n) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(n);
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric'
    });
  }
}