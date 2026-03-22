// src/app/client/billing/client-billing.component.ts

import { Component, OnInit } from '@angular/core';
import { BillingService } from '../../shared/services/billing.service';
import {
  BillingSummary, InvoiceDto, InvoiceStatus,
  INVOICE_STATUS_LABELS, INVOICE_TYPE_LABELS
} from '../../shared/models/invoice.model';

@Component({
  selector: 'app-client-billing',
  templateUrl: './client-billing.component.html',
  styleUrls: ['./client-billing.component.scss']
})
export class ClientBillingComponent implements OnInit {

  // ── State ─────────────────────────────────────────────────────────────────
  summary:  BillingSummary | null = null;
  invoices: InvoiceDto[]          = [];
  filtered: InvoiceDto[]          = [];
  selected: InvoiceDto | null     = null;

  loadingSummary  = true;
  loadingInvoices = true;
  errorSummary    = '';
  errorInvoices   = '';

  // ── Filters ───────────────────────────────────────────────────────────────
  activeTab:      'all' | 'pending' | 'paid' = 'all';
  selectedStatus  = '';
  selectedType    = '';
  searchKeyword   = '';

  // ── Reference data ────────────────────────────────────────────────────────
  readonly statusLabels = INVOICE_STATUS_LABELS;
  readonly typeLabels   = INVOICE_TYPE_LABELS;
  readonly allStatuses  = Object.keys(INVOICE_STATUS_LABELS) as InvoiceStatus[];

  constructor(private billingService: BillingService) {}

  ngOnInit(): void {
    this.loadSummary();
    this.loadInvoices();
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadSummary(): void {
    this.billingService.getSummary().subscribe({
      next:  s  => { this.summary = s; this.loadingSummary = false; },
      error: () => { this.errorSummary = 'Failed to load summary.'; this.loadingSummary = false; }
    });
  }

  loadInvoices(): void {
    this.billingService.getMyInvoices().subscribe({
      next: invs => {
        this.invoices = invs;
        this.applyFilters();
        this.loadingInvoices = false;
      },
      error: () => {
        this.errorInvoices   = 'Failed to load invoices.';
        this.loadingInvoices = false;
      }
    });
  }

  // ── Tabs & filters ────────────────────────────────────────────────────────
  setTab(tab: 'all' | 'pending' | 'paid'): void {
    this.activeTab = tab;
    this.selectedStatus = '';
    this.applyFilters();
  }

  applyFilters(): void {
    let list = [...this.invoices];

    // Tab filter
    if (this.activeTab === 'pending')
      list = list.filter(i => ['UNPAID','PARTIALLY_PAID','OVERDUE'].includes(i.status));
    else if (this.activeTab === 'paid')
      list = list.filter(i => i.status === 'PAID');

    // Status dropdown
    if (this.selectedStatus)
      list = list.filter(i => i.status === this.selectedStatus);

    // Type dropdown
    if (this.selectedType)
      list = list.filter(i => i.invoiceType === this.selectedType);

    // Search
    if (this.searchKeyword.trim()) {
      const kw = this.searchKeyword.toLowerCase();
      list = list.filter(i =>
        i.title.toLowerCase().includes(kw) ||
        i.invoiceNumber.toLowerCase().includes(kw) ||
        i.caseTitle?.toLowerCase().includes(kw)
      );
    }

    this.filtered = list;
  }

  clearFilters(): void {
    this.selectedStatus = '';
    this.selectedType   = '';
    this.searchKeyword  = '';
    this.applyFilters();
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(inv: InvoiceDto): void  { this.selected = inv; }
  closeDetail(): void                 { this.selected = null; }

  // ── Helpers ───────────────────────────────────────────────────────────────
  formatCurrency(n: number | null): string {
    return this.billingService.formatCurrency(n);
  }

  formatDate(iso: string | null): string {
    return this.billingService.formatDate(iso);
  }

  isDueSoon(iso: string | null): boolean {
    return this.billingService.isDueSoon(iso);
  }

  statusLabel(s: string | null): string {
    return s ? (INVOICE_STATUS_LABELS[s as InvoiceStatus] ?? s) : '—';
  }

  typeLabel(t: string | null): string {
    return t ? (INVOICE_TYPE_LABELS[t as keyof typeof INVOICE_TYPE_LABELS] ?? t) : '—';
  }

  statusClass(inv: InvoiceDto): string {
    if (inv.overdue) return 'badge--overdue';
    const map: Record<string, string> = {
      UNPAID:         'badge--unpaid',
      PARTIALLY_PAID: 'badge--partial',
      PAID:           'badge--paid',
      OVERDUE:        'badge--overdue',
      CANCELLED:      'badge--cancelled',
      WAIVED:         'badge--waived',
    };
    return map[inv.status] ?? 'badge--unpaid';
  }

  progressPercent(inv: InvoiceDto): number {
    if (!inv.totalAmount || inv.totalAmount === 0) return 0;
    return Math.min(100, Math.round((inv.paidAmount / inv.totalAmount) * 100));
  }

  get activeFilterCount(): number {
    return [this.selectedStatus, this.selectedType, this.searchKeyword]
      .filter(v => !!v).length;
  }

  get totalDueDisplay(): string {
    return this.formatCurrency(this.summary?.totalDue ?? 0);
  }

  get totalPaidDisplay(): string {
    return this.formatCurrency(this.summary?.totalPaid ?? 0);
  }
}