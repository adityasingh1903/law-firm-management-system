// src/app/lawyer/hearings/lawyer-hearings.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { GroupedHearingsDto, HearingDto } from '../../shared/models/hearing.model';

interface CaseOption {
  id:         number;
  caseNumber: string;
  title:      string;
  status:     string;
}

@Component({
  selector: 'app-lawyer-hearings',
  templateUrl: './lawyer-hearings.component.html',
  styleUrls: ['./lawyer-hearings.component.scss']
})
export class LawyerHearingsComponent implements OnInit {

  // ── Data ──────────────────────────────────────────────────────────────────
  grouped:  GroupedHearingsDto | null = null;
  cases:    CaseOption[]              = [];
  loading   = true;
  error     = '';

  // ── Tabs & selection ──────────────────────────────────────────────────────
  activeTab:       'upcoming' | 'past' = 'upcoming';
  selectedHearing: HearingDto | null   = null;

  // ── Schedule / Edit modal ─────────────────────────────────────────────────
  scheduleModalOpen = false;
  scheduleForm!:    FormGroup;
  submitting        = false;
  editingHearing:   HearingDto | null = null;

  // ── Status editor ─────────────────────────────────────────────────────────
  editStatusMode = false;
  newStatus      = '';
  savingStatus   = false;

  // ── Reference data ────────────────────────────────────────────────────────
  readonly hearingStatuses = ['SCHEDULED', 'COMPLETED', 'POSTPONED', 'CANCELLED'];

  private base = `${environment.apiUrl}/lawyer`;

  constructor(
    private http:     HttpClient,
    private fb:       FormBuilder,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadGrouped();
    this.loadCases();
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadGrouped(): void {
    this.loading = true;
    this.http.get<GroupedHearingsDto>(`${this.base}/hearings/grouped`).subscribe({
      next:  g  => { this.grouped = g; this.loading = false; },
      error: () => { this.error = 'Failed to load hearings. Please try again.'; this.loading = false; }
    });
  }

  loadCases(): void {
    this.http.get<CaseOption[]>(`${this.base}/cases`).subscribe({
      next: all => this.cases = all.filter(
        c => c.status === 'OPEN' || c.status === 'IN_PROGRESS'),
      error: () => {}
    });
  }

  // ── Tabs ──────────────────────────────────────────────────────────────────
  get displayList(): HearingDto[] {
    if (!this.grouped) return [];
    return this.activeTab === 'upcoming' ? this.grouped.upcoming : this.grouped.past;
  }

  setTab(tab: 'upcoming' | 'past'): void {
    this.activeTab       = tab;
    this.selectedHearing = null;
    this.editStatusMode  = false;
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(h: HearingDto): void {
    this.selectedHearing = h;
    this.editStatusMode  = false;
    this.newStatus       = h.status;
  }

  closeDetail(): void {
    this.selectedHearing = null;
    this.editStatusMode  = false;
  }

  // ── Schedule / Edit modal ─────────────────────────────────────────────────
  private buildForm(): void {
    this.scheduleForm = this.fb.group({
      caseId:      [null,  Validators.required],
      title:       ['',   [Validators.required, Validators.maxLength(200)]],
      description: [''],
      hearingDate: ['',    Validators.required],
      hearingTime: ['',    Validators.required],
      courtName:   [''],
      courtRoom:   [''],
      judgeName:   [''],
      notes:       ['']
    });
  }

  openScheduleModal(): void {
    this.editingHearing = null;
    this.scheduleForm.reset();
    this.scheduleModalOpen = true;
  }

  openEditModal(h: HearingDto): void {
    this.editingHearing = h;
    const d   = new Date(h.hearingDate);
    const pad = (n: number) => n.toString().padStart(2, '0');
    this.scheduleForm.patchValue({
      caseId:      h.caseId,
      title:       h.title,
      description: h.description ?? '',
      hearingDate: `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`,
      hearingTime: `${pad(d.getHours())}:${pad(d.getMinutes())}`,
      courtName:   h.courtName  ?? '',
      courtRoom:   h.courtRoom  ?? '',
      judgeName:   h.judgeName  ?? '',
      notes:       h.notes      ?? ''
    });
    this.scheduleModalOpen = true;
  }

  closeScheduleModal(): void {
    this.scheduleModalOpen = false;
    this.editingHearing    = null;
  }

  submitSchedule(): void {
    if (this.scheduleForm.invalid) { this.scheduleForm.markAllAsTouched(); return; }
    this.submitting = true;

    const v = this.scheduleForm.value;
    const payload = {
      caseId:      v.caseId,
      title:       v.title,
      description: v.description || null,
      hearingDate: `${v.hearingDate}T${v.hearingTime}:00`,
      courtName:   v.courtName  || null,
      courtRoom:   v.courtRoom  || null,
      judgeName:   v.judgeName  || null,
      notes:       v.notes      || null
    };

    const req = this.editingHearing
      ? this.http.put<HearingDto>(`${this.base}/hearings/${this.editingHearing.id}`, payload)
      : this.http.post<HearingDto>(`${this.base}/hearings`, payload);

    req.subscribe({
      next: saved => {
        this.submitting       = false;
        this.scheduleModalOpen = false;
        this.snackBar.open(
          this.editingHearing ? 'Hearing updated.' : 'Hearing scheduled.',
          'Close', { duration: 3000 });
        this.editingHearing   = null;
        this.selectedHearing  = saved;
        this.loadGrouped();
      },
      error: err => {
        this.submitting = false;
        this.snackBar.open(err?.error?.message ?? 'Failed to save hearing.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Status update ─────────────────────────────────────────────────────────
  saveStatus(): void {
    if (!this.selectedHearing || !this.newStatus) return;
    this.savingStatus = true;
    this.http.patch<HearingDto>(
      `${this.base}/hearings/${this.selectedHearing.id}/status`, { status: this.newStatus }
    ).subscribe({
      next: updated => {
        this.selectedHearing = updated;
        this.editStatusMode  = false;
        this.savingStatus    = false;
        this.loadGrouped();
        this.snackBar.open('Status updated.', 'Close', { duration: 3000 });
      },
      error: () => {
        this.savingStatus = false;
        this.snackBar.open('Failed to update status.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  deleteHearing(h: HearingDto): void {
    if (!confirm(`Delete "${h.title}"? This cannot be undone.`)) return;
    this.http.delete(`${this.base}/hearings/${h.id}`).subscribe({
      next: () => {
        this.snackBar.open('Hearing deleted.', 'Close', { duration: 3000 });
        if (this.selectedHearing?.id === h.id) this.selectedHearing = null;
        this.loadGrouped();
      },
      error: err => this.snackBar.open(
        err?.error?.message ?? 'Cannot delete this hearing.', 'Close', { duration: 3000 })
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  statusClass(s: string): string {
    const map: Record<string, string> = {
      SCHEDULED: 'badge--scheduled', COMPLETED: 'badge--completed',
      POSTPONED: 'badge--postponed', CANCELLED: 'badge--cancelled'
    };
    return map[s] ?? 'badge--scheduled';
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      SCHEDULED: 'Scheduled', COMPLETED: 'Completed',
      POSTPONED: 'Postponed', CANCELLED: 'Cancelled'
    };
    return map[s] ?? s;
  }

  getDay(iso: string): string {
    return new Date(iso).getDate().toString().padStart(2, '0');
  }

  getMonth(iso: string): string {
    return new Date(iso).toLocaleString('en-IN', { month: 'short' }).toUpperCase();
  }

  getYear(iso: string): string {
    return new Date(iso).getFullYear().toString();
  }

  getTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }

  getFullDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-IN', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  daysUntil(iso: string): number {
    return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000);
  }

  get isEditing(): boolean { return !!this.editingHearing; }
}