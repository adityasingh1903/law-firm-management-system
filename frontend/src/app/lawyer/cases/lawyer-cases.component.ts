// src/app/lawyer/cases/lawyer-cases.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LawyerCasesService } from '../../shared/services/lawyer-cases.service';
import {
  LawyerCaseDto, CaseRequestDto, AcceptRequestForm,
  CaseStatus, CASE_STATUS_LABELS, CASE_TYPE_LABELS
} from '../../shared/models/lawyer-cases.model';

@Component({
  selector: 'app-lawyer-cases',
  templateUrl: './lawyer-cases.component.html',
  styleUrls: ['./lawyer-cases.component.scss']
})
export class LawyerCasesComponent implements OnInit {

  // ── Tabs ──────────────────────────────────────────────────────────────────
  activeTab: 'cases' | 'requests' | 'handled' = 'cases';

  // ── My Cases ──────────────────────────────────────────────────────────────
  cases:         LawyerCaseDto[] = [];
  filteredCases: LawyerCaseDto[] = [];
  loadingCases   = true;
  filterStatus   = '';
  filterType     = '';
  searchKeyword  = '';

  // ── Case detail panel ─────────────────────────────────────────────────────
  selectedCase:   LawyerCaseDto | null = null;
  detailOpen      = false;

  // Edit status panel
  editStatusMode  = false;
  newStatus       = '';
  savingStatus    = false;

  // Edit notes panel
  editNotesMode   = false;
  notesValue      = '';
  savingNotes     = false;

  // ── Case Requests ─────────────────────────────────────────────────────────
  pendingRequests:  CaseRequestDto[] = [];
  handledRequests:  CaseRequestDto[] = [];
  loadingRequests   = true;
  loadingHandled    = false;
  pendingCount      = 0;
  filterReqType     = '';

  // ── Accept modal ──────────────────────────────────────────────────────────
  acceptModalOpen   = false;
  acceptingRequest: CaseRequestDto | null = null;
  acceptForm!: FormGroup;
  submittingAccept  = false;

  // ── Reject modal ──────────────────────────────────────────────────────────
  rejectModalOpen   = false;
  rejectingRequest: CaseRequestDto | null = null;
  rejectReason      = '';
  submittingReject  = false;

  // ── Reference data ────────────────────────────────────────────────────────
  readonly caseStatuses = ['OPEN','IN_PROGRESS','CLOSED','SETTLED','DISMISSED','APPEALED'];
  readonly caseTypes    = ['CRIMINAL','CIVIL','FAMILY','CORPORATE','REAL_ESTATE',
                           'IMMIGRATION','TAX','LABOR','INTELLECTUAL_PROPERTY','OTHER'];
  readonly statusLabels: Record<string, string> = CASE_STATUS_LABELS;
  readonly typeLabels   = CASE_TYPE_LABELS;

  constructor(
    private svc:      LawyerCasesService,
    private fb:       FormBuilder,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildAcceptForm();
    this.loadCases();
    this.loadPendingRequests();
    this.svc.getPendingCount().subscribe({ next: n => this.pendingCount = n, error: () => {} });
  }

  // ── Load data ─────────────────────────────────────────────────────────────
  loadCases(): void {
    this.loadingCases = true;
    this.svc.getMyCases().subscribe({
      next: cases => {
        this.cases = cases;
        this.applyFilters();
        this.loadingCases = false;
      },
      error: () => this.loadingCases = false
    });
  }

  loadPendingRequests(): void {
    this.loadingRequests = true;
    this.svc.getPendingRequests(this.filterReqType || undefined).subscribe({
      next: reqs => { this.pendingRequests = reqs; this.loadingRequests = false; },
      error: () => this.loadingRequests = false
    });
  }

  loadHandledRequests(): void {
    if (this.handledRequests.length > 0) return; // already loaded
    this.loadingHandled = true;
    this.svc.getHandledRequests().subscribe({
      next: reqs => { this.handledRequests = reqs; this.loadingHandled = false; },
      error: () => this.loadingHandled = false
    });
  }

  setTab(tab: 'cases' | 'requests' | 'handled'): void {
    this.activeTab   = tab;
    this.detailOpen  = false;
    if (tab === 'handled') this.loadHandledRequests();
  }

  // ── Filter cases ──────────────────────────────────────────────────────────
  applyFilters(): void {
    this.filteredCases = this.cases.filter(c => {
      const statusOk = !this.filterStatus || c.status === this.filterStatus;
      const typeOk   = !this.filterType   || c.caseType === this.filterType;
      const searchOk = !this.searchKeyword ||
        c.title.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        c.caseNumber.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        (c.clientName || '').toLowerCase().includes(this.searchKeyword.toLowerCase());
      return statusOk && typeOk && searchOk;
    });
  }

  clearFilters(): void {
    this.filterStatus = '';
    this.filterType   = '';
    this.searchKeyword = '';
    this.applyFilters();
  }

  onReqTypeFilter(): void { this.loadPendingRequests(); }

  // ── Case detail panel ─────────────────────────────────────────────────────
  openDetail(c: LawyerCaseDto): void {
    this.selectedCase   = c;
    this.detailOpen     = true;
    this.editStatusMode = false;
    this.editNotesMode  = false;
    this.newStatus      = c.status;
    this.notesValue     = c.description ?? '';
  }

  closeDetail(): void {
    this.detailOpen   = false;
    this.selectedCase = null;
  }

  // ── Update status ─────────────────────────────────────────────────────────
  saveStatus(): void {
    if (!this.selectedCase || !this.newStatus) return;
    this.savingStatus = true;
    this.svc.updateStatus(this.selectedCase.id, this.newStatus).subscribe({
      next: updated => {
        Object.assign(this.selectedCase!, updated);
        const idx = this.cases.findIndex(c => c.id === updated.id);
        if (idx > -1) this.cases[idx] = updated;
        this.applyFilters();
        this.editStatusMode = false;
        this.savingStatus   = false;
        this.snackBar.open('Case status updated.', 'Close', { duration: 3000 });
      },
      error: err => {
        this.savingStatus = false;
        this.snackBar.open(err?.error?.message ?? 'Failed to update status.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Update notes ──────────────────────────────────────────────────────────
  saveNotes(): void {
    if (!this.selectedCase) return;
    this.savingNotes = true;
    this.svc.updateNotes(this.selectedCase.id, this.notesValue).subscribe({
      next: updated => {
        Object.assign(this.selectedCase!, updated);
        this.editNotesMode = false;
        this.savingNotes   = false;
        this.snackBar.open('Notes saved.', 'Close', { duration: 3000 });
      },
      error: () => {
        this.savingNotes = false;
        this.snackBar.open('Failed to save notes.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Accept request ────────────────────────────────────────────────────────
  private buildAcceptForm(): void {
    this.acceptForm = this.fb.group({
      caseNumber:  ['', [Validators.required, Validators.maxLength(50)]],
      courtName:   [''],
      judgeName:   [''],
      feesCharged: [null],
      notes:       ['']
    });
  }

  openAcceptModal(req: CaseRequestDto): void {
    this.acceptingRequest = req;
    this.acceptForm.reset();
    this.acceptModalOpen  = true;
  }

  closeAcceptModal(): void { this.acceptModalOpen = false; this.acceptingRequest = null; }

  submitAccept(): void {
    if (this.acceptForm.invalid || !this.acceptingRequest) {
      this.acceptForm.markAllAsTouched(); return;
    }
    this.submittingAccept = true;
    const form: AcceptRequestForm = this.acceptForm.value;

    this.svc.acceptRequest(this.acceptingRequest.id, form).subscribe({
      next: newCase => {
        this.pendingRequests = this.pendingRequests.filter(r => r.id !== this.acceptingRequest!.id);
        this.pendingCount    = Math.max(0, this.pendingCount - 1);
        this.cases.unshift(newCase as any);
        this.applyFilters();
        this.submittingAccept = false;
        this.acceptModalOpen  = false;
        this.acceptingRequest = null;
        this.snackBar.open('Request accepted! Case created successfully.', 'Close', { duration: 4000 });
        this.activeTab = 'cases';
      },
      error: err => {
        this.submittingAccept = false;
        this.snackBar.open(err?.error?.message ?? 'Failed to accept request.', 'Close', { duration: 4000 });
      }
    });
  }

  // ── Reject request ────────────────────────────────────────────────────────
  openRejectModal(req: CaseRequestDto): void {
    this.rejectingRequest = req;
    this.rejectReason     = '';
    this.rejectModalOpen  = true;
  }

  closeRejectModal(): void { this.rejectModalOpen = false; this.rejectingRequest = null; }

  submitReject(): void {
    if (!this.rejectReason.trim() || !this.rejectingRequest) return;
    this.submittingReject = true;

    this.svc.rejectRequest(this.rejectingRequest.id, this.rejectReason).subscribe({
      next: () => {
        this.pendingRequests  = this.pendingRequests.filter(r => r.id !== this.rejectingRequest!.id);
        this.pendingCount     = Math.max(0, this.pendingCount - 1);
        this.handledRequests  = []; // force reload next time
        this.submittingReject = false;
        this.rejectModalOpen  = false;
        this.rejectingRequest = null;
        this.snackBar.open('Request rejected.', 'Close', { duration: 3000 });
      },
      error: () => {
        this.submittingReject = false;
        this.snackBar.open('Failed to reject request.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  statusClass(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'badge--open', IN_PROGRESS: 'badge--progress',
      CLOSED: 'badge--closed', SETTLED: 'badge--settled',
      DISMISSED: 'badge--dismissed', APPEALED: 'badge--appealed'
    };
    return map[status] ?? 'badge--open';
  }

  urgencyClass(u: string): string {
    const map: Record<string, string> = {
      LOW: 'urg--low', MEDIUM: 'urg--medium',
      HIGH: 'urg--high', CRITICAL: 'urg--critical'
    };
    return map[u] ?? 'urg--medium';
  }

  reqStatusClass(s: string): string {
    return s === 'ACCEPTED' ? 'badge--open'
         : s === 'REJECTED' ? 'badge--dismissed'
         : 'badge--progress';
  }

  typeLabel(t: string | null): string {
    return t ? (CASE_TYPE_LABELS[t] ?? t) : '—';
  }

  formatCurrency(n: number | null): string { return this.svc.formatCurrency(n); }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatRelative(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60)  return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24)   return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    if (days < 7)   return `${days}d ago`;
    return this.formatDate(iso);
  }

  getInitials(name: string): string {
    return (name || '?').split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }

  getTimelineSteps(): { label: string; done: boolean; active: boolean }[] {
    const order = ['OPEN', 'IN_PROGRESS', 'CLOSED'];
    const status = this.selectedCase?.status ?? 'OPEN';
    const idx = order.indexOf(status);
    return order.map((s, i) => ({
      label: CASE_STATUS_LABELS[s as CaseStatus] ?? s,
      done:   i < idx,
      active: i === idx
    }));
  }

  get activeCaseCount(): number {
    return this.cases.filter(c => c.status === 'OPEN' || c.status === 'IN_PROGRESS').length;
  }

  get activeFilterCount(): number {
    return [this.filterStatus, this.filterType, this.searchKeyword].filter(v => !!v).length;
  }
}