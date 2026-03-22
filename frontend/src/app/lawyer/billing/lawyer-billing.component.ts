import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-lawyer-billing',
  templateUrl: './lawyer-billing.component.html',
  styleUrls: ['./lawyer-billing.component.scss']
})
export class LawyerBillingComponent implements OnInit {

  // ── Summary ───────────────────────────────────────────────────────────────
  summary: any = null;
  isLoadingSummary = true;

  // ── Invoices ──────────────────────────────────────────────────────────────
  invoices:         any[] = [];
  filteredInvoices: any[] = [];
  isLoadingInvoices = true;
  filterStatus      = '';

  invoiceStatuses  = ['UNPAID','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED','WAIVED'];
  invoiceTypes     = ['FEES','COURT_FEES','CONSULTATION','MISCELLANEOUS'];
  paymentMethods   = ['CASH','BANK_TRANSFER','UPI','CHEQUE'];

  // ── Detail panel ──────────────────────────────────────────────────────────
  selectedInvoice: any = null;
  detailOpen       = false;

  // ── Create modal ──────────────────────────────────────────────────────────
  createModalOpen = false;
  createForm!:    FormGroup;
  isSubmitting    = false;
  cases:          any[] = [];
  isLoadingCases  = true;

  // ── Status modal ──────────────────────────────────────────────────────────
  statusModalOpen  = false;
  statusForm!:     FormGroup;
  updatingInvoice: any = null;
  isUpdatingStatus = false;

  readonly MONTHS = ['Jan','Feb','Mar','Apr','May','Jun',
                     'Jul','Aug','Sep','Oct','Nov','Dec'];

  monthlyData:  { month: string; amount: number }[] = [];
  maxMonthly    = 1;
  currentYear   = new Date().getFullYear();

  constructor(
    private http:     HttpClient,
    private fb:       FormBuilder,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildCreateForm();
    this.buildStatusForm();
    this.loadSummary();
    this.loadInvoices();
    this.loadCases();
  }

  private get headers(): HttpHeaders {
    const token = localStorage.getItem('authToken');
    return new HttpHeaders({ 'Authorization': `Bearer ${token}` });
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadSummary(): void {
    this.isLoadingSummary = true;
    this.http.get<any>(`${environment.apiUrl}/lawyer/billing/summary`, { headers: this.headers })
      .subscribe({
        next: (d) => { this.summary = d; this.isLoadingSummary = false; },
        error: (err) => {
          console.error('Summary load error:', err);
          this.isLoadingSummary = false;
        }
      });
  }

  loadInvoices(): void {
    this.isLoadingInvoices = true;
    this.http.get<any[]>(`${environment.apiUrl}/lawyer/billing/invoices`, { headers: this.headers })
      .subscribe({
        next: (d) => {
          this.invoices          = d;
          this.filteredInvoices  = d;
          this.isLoadingInvoices = false;
          this.buildMonthlyChart(d);
        },
        error: (err) => {
          console.error('Invoices load error:', err);
          this.isLoadingInvoices = false;
        }
      });
  }

  loadCases(): void {
    this.isLoadingCases = true;
    this.http.get<any[]>(`${environment.apiUrl}/lawyer/cases`, { headers: this.headers })
      .subscribe({
        next: (d) => {
          // Only show OPEN or IN_PROGRESS cases for billing
          this.cases = d.filter(c =>
            c.status === 'OPEN' || c.status === 'IN_PROGRESS'
          );
          this.isLoadingCases = false;
          console.log('Cases loaded for billing:', this.cases.length, this.cases);
        },
        error: (err) => {
          console.error('Cases load error:', err);
          // Load all cases as fallback (don't filter)
          this.cases = [];
          this.isLoadingCases = false;
        }
      });
  }

  // ── Monthly chart ─────────────────────────────────────────────────────────
  private buildMonthlyChart(invoices: any[]): void {
    const map: Record<number, number> = {};
    invoices
      .filter(i => i.status === 'PAID' && i.paidDate)
      .forEach(i => {
        const month = new Date(i.paidDate).getMonth();
        map[month] = (map[month] ?? 0) + (i.paidAmount ?? 0);
      });
    this.monthlyData = this.MONTHS.map((m, i) => ({ month: m, amount: map[i] ?? 0 }));
    this.maxMonthly  = Math.max(...this.monthlyData.map(d => d.amount), 1);
  }

  getBarWidth(amount: number): number {
    return Math.round((amount / this.maxMonthly) * 100);
  }

  // ── Filter ────────────────────────────────────────────────────────────────
  applyFilter(): void {
    this.filteredInvoices = this.filterStatus
      ? this.invoices.filter(i => i.status === this.filterStatus)
      : [...this.invoices];
  }

  clearFilter(): void {
    this.filterStatus     = '';
    this.filteredInvoices = [...this.invoices];
  }

  // ── Status helpers ────────────────────────────────────────────────────────
  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      UNPAID: 'inv--unpaid', PARTIALLY_PAID: 'inv--partial',
      PAID: 'inv--paid', OVERDUE: 'inv--overdue',
      CANCELLED: 'inv--cancelled', WAIVED: 'inv--waived'
    };
    return map[status] || '';
  }

  isOverdue(inv: any): boolean {
    return inv.status === 'UNPAID' && inv.dueDate &&
           new Date(inv.dueDate) < new Date();
  }

  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      FEES: 'Legal Fees', COURT_FEES: 'Court Fees',
      CONSULTATION: 'Consultation', MISCELLANEOUS: 'Miscellaneous'
    };
    return map[type] || type;
  }

  get showPaidFields(): boolean {
    const s = this.statusForm.get('status')?.value;
    return s === 'PAID' || s === 'PARTIALLY_PAID';
  }

  get requirePartialAmount(): boolean {
    return this.statusForm.get('status')?.value === 'PARTIALLY_PAID';
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(inv: any): void {
    this.http.get<any>(
      `${environment.apiUrl}/lawyer/billing/invoices/${inv.id}`,
      { headers: this.headers }
    ).subscribe({
      next: (d) => { this.selectedInvoice = d; this.detailOpen = true; },
      error: (err) => { console.error('Detail load error:', err); }
    });
  }

  closeDetail(): void { this.detailOpen = false; this.selectedInvoice = null; }

  // ── Create modal ──────────────────────────────────────────────────────────
  private buildCreateForm(): void {
    this.createForm = this.fb.group({
      caseId:      [null, Validators.required],   // null not '' so Number cast works
      title:       ['', [Validators.required, Validators.maxLength(200)]],
      description: [''],
      invoiceType: ['FEES', Validators.required],
      amount:      [null, [Validators.required, Validators.min(1)]],
      taxAmount:   [0],
      dueDate:     [''],
      notes:       ['']
    });
  }

  get previewTotal(): number {
    const amt = Number(this.createForm.get('amount')?.value) || 0;
    const tax = Number(this.createForm.get('taxAmount')?.value) || 0;
    return amt + tax;
  }

  openCreateModal(): void {
    this.createForm.reset({ invoiceType: 'FEES', taxAmount: 0, caseId: null });
    this.createModalOpen = true;
  }

  closeCreateModal(): void { this.createModalOpen = false; }

  submitInvoice(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      console.warn('Form invalid:', this.createForm.value, this.createForm.errors);
      return;
    }

    this.isSubmitting = true;

    const formVal = this.createForm.value;

    // Explicitly coerce types — select gives string, backend needs Long/Double
    const payload: any = {
      caseId:      Number(formVal.caseId),       // string → number (Long)
      title:       formVal.title?.trim(),
      invoiceType: formVal.invoiceType || 'FEES',
      amount:      Number(formVal.amount),        // string → number (Double)
      taxAmount:   Number(formVal.taxAmount) || 0,
    };

    if (formVal.description?.trim()) payload.description = formVal.description.trim();
    if (formVal.notes?.trim())       payload.notes        = formVal.notes.trim();
    if (formVal.dueDate)             payload.dueDate      = formVal.dueDate;

    console.log('Creating invoice with payload:', payload);

    this.http.post<any>(
      `${environment.apiUrl}/lawyer/billing/invoices`,
      payload,
      { headers: this.headers }
    ).subscribe({
      next: (inv) => {
        console.log('Invoice created:', inv);
        this.invoices.unshift(inv);
        this.applyFilter();
        this.buildMonthlyChart(this.invoices);
        this.isSubmitting    = false;
        this.createModalOpen = false;
        this.loadSummary();
        this.snackBar.open(`${inv.invoiceNumber} created successfully!`, 'Close', { duration: 4000 });
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Invoice create error:', err);
        console.error('Error status:', err?.status);
        console.error('Error body:', err?.error);

        // Try to get the most useful message
        const msg = err?.error?.message
                 || err?.error?.error
                 || (typeof err?.error === 'string' ? err.error : null)
                 || `Error ${err?.status}: Failed to create invoice.`;

        this.snackBar.open(msg, 'Close', { duration: 6000 });
      }
    });
  }

  // ── Status modal ──────────────────────────────────────────────────────────
  private buildStatusForm(): void {
    this.statusForm = this.fb.group({
      status:           ['', Validators.required],
      paidAmount:       [''],
      paidDate:         [''],
      paymentMethod:    [''],
      paymentReference: ['']
    });
  }

  openStatusModal(inv: any, event?: Event): void {
    event?.stopPropagation();
    this.updatingInvoice = inv;
    this.statusForm.reset({
      status:      inv.status,
      paidAmount:  inv.paidAmount || '',
      paidDate:    inv.paidDate   || '',
      paymentMethod: inv.paymentMethod || ''
    });
    this.statusModalOpen = true;
  }

  closeStatusModal(): void { this.statusModalOpen = false; this.updatingInvoice = null; }

  submitStatus(): void {
    if (this.statusForm.invalid) { this.statusForm.markAllAsTouched(); return; }

    const val = this.statusForm.value;
    if (val.status === 'PARTIALLY_PAID' && (!val.paidAmount || val.paidAmount <= 0)) {
      this.snackBar.open('Enter the amount paid for partial payment.', 'Close', { duration: 3000 });
      return;
    }

    this.isUpdatingStatus = true;
    const payload: any = { status: val.status };
    if (val.paidAmount)       payload.paidAmount       = Number(val.paidAmount);
    if (val.paidDate)         payload.paidDate         = val.paidDate;
    if (val.paymentMethod)    payload.paymentMethod    = val.paymentMethod;
    if (val.paymentReference) payload.paymentReference = val.paymentReference;

    this.http.patch<any>(
      `${environment.apiUrl}/lawyer/billing/invoices/${this.updatingInvoice.id}/status`,
      payload,
      { headers: this.headers }
    ).subscribe({
      next: (updated) => {
        const idx = this.invoices.findIndex(i => i.id === updated.id);
        if (idx !== -1) this.invoices[idx] = updated;
        this.applyFilter();
        this.buildMonthlyChart(this.invoices);
        if (this.selectedInvoice?.id === updated.id) this.selectedInvoice = updated;
        this.isUpdatingStatus = false;
        this.statusModalOpen  = false;
        this.loadSummary();
        this.snackBar.open('Invoice updated.', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.isUpdatingStatus = false;
        console.error('Status update error:', err);
        this.snackBar.open(err?.error?.message || 'Update failed.', 'Close', { duration: 4000 });
      }
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  deleteInvoice(inv: any, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Delete ${inv.invoiceNumber}? This cannot be undone.`)) return;
    this.http.delete(
      `${environment.apiUrl}/lawyer/billing/invoices/${inv.id}`,
      { headers: this.headers }
    ).subscribe({
      next: () => {
        this.invoices         = this.invoices.filter(i => i.id !== inv.id);
        this.filteredInvoices = this.filteredInvoices.filter(i => i.id !== inv.id);
        this.buildMonthlyChart(this.invoices);
        if (this.selectedInvoice?.id === inv.id) this.closeDetail();
        this.loadSummary();
        this.snackBar.open('Invoice deleted.', 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Delete error:', err);
        this.snackBar.open(err?.error?.message || 'Cannot delete this invoice.', 'Close', { duration: 4000 });
      }
    });
  }
}