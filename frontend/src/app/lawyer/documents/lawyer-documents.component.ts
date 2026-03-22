// src/app/lawyer/documents/lawyer-documents.component.ts

import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { environment } from '../../../environments/environment';
import {
  DocumentDto, DocumentType, DocumentStatus,
  DOCUMENT_TYPE_LABELS, DOCUMENT_STATUS_LABELS
} from '../../shared/models/document.model';

interface CaseDocGroup {
  caseId:          number;
  caseNumber:      string;
  caseTitle:       string;
  documents:       DocumentDto[];
  docCount:        number;
  pendingReview:   number;   // docs with status UPLOADED
}

@Component({
  selector: 'app-lawyer-documents',
  templateUrl: './lawyer-documents.component.html',
  styleUrls: ['./lawyer-documents.component.scss']
})
export class LawyerDocumentsComponent implements OnInit {

  // ── View state ────────────────────────────────────────────────────────────
  view: 'cases' | 'documents' = 'cases';

  // ── Cases overview ────────────────────────────────────────────────────────
  caseGroups:    CaseDocGroup[] = [];
  loadingCases   = true;
  casesError     = '';

  // ── Document list (drilled into a case) ───────────────────────────────────
  activeCaseGroup: CaseDocGroup | null = null;
  documents:  DocumentDto[] = [];
  filtered:   DocumentDto[] = [];

  // ── Filters ───────────────────────────────────────────────────────────────
  searchKeyword  = '';
  selectedType   = '';
  selectedStatus = '';
  searchTimeout: any;

  // ── Detail panel ──────────────────────────────────────────────────────────
  selectedDoc: DocumentDto | null = null;

  // ── Status update ─────────────────────────────────────────────────────────
  editStatusMode  = false;
  newDocStatus    = '';
  rejectionNote   = '';
  savingStatus    = false;

  // ── Upload panel ──────────────────────────────────────────────────────────
  showUploadPanel = false;
  uploadTitle     = '';
  uploadDesc      = '';
  uploadType      = 'OTHER';
  selectedFile: File | null = null;
  uploading       = false;
  uploadError     = '';
  uploadSuccess   = '';

  // ── Download ──────────────────────────────────────────────────────────────
  downloading: number | null = null;

  // ── Reference data ────────────────────────────────────────────────────────
  readonly typeLabels:   Record<string, string> = DOCUMENT_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = DOCUMENT_STATUS_LABELS;
  readonly allTypes     = Object.keys(DOCUMENT_TYPE_LABELS)  as DocumentType[];
  readonly allStatuses  = Object.keys(DOCUMENT_STATUS_LABELS) as DocumentStatus[];
  readonly reviewableStatuses = ['PENDING_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED'];

  private base = `${environment.apiUrl}/lawyer`;

  constructor(private http: HttpClient, private snackBar: MatSnackBar) {}

  ngOnInit(): void { this.loadCaseGroups(); }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadCaseGroups(): void {
    this.loadingCases = true;
    this.http.get<DocumentDto[]>(`${this.base}/documents`).subscribe({
      next: docs => {
        this.caseGroups   = this.groupByCase(docs);
        this.loadingCases = false;
      },
      error: () => { this.casesError = 'Failed to load documents.'; this.loadingCases = false; }
    });
  }

  private groupByCase(docs: DocumentDto[]): CaseDocGroup[] {
    const map = new Map<number, CaseDocGroup>();
    for (const d of docs) {
      const cid = d.caseId ?? 0;
      if (!map.has(cid)) {
        map.set(cid, {
          caseId: cid, caseNumber: d.caseNumber ?? '—',
          caseTitle: d.caseTitle ?? 'Unknown Case',
          documents: [], docCount: 0, pendingReview: 0
        });
      }
      const g = map.get(cid)!;
      g.documents.push(d);
      g.docCount++;
      if (d.status === 'UPLOADED') g.pendingReview++;
    }
    return Array.from(map.values())
      .sort((a, b) => b.pendingReview - a.pendingReview || a.caseNumber.localeCompare(b.caseNumber));
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  openCase(group: CaseDocGroup): void {
    this.activeCaseGroup = group;
    this.view            = 'documents';
    this.documents       = [...group.documents];
    this.filtered        = [...group.documents];
    this.selectedDoc     = null;
    this.editStatusMode  = false;
    this.searchKeyword   = '';
    this.selectedType    = '';
    this.selectedStatus  = '';
  }

  backToCases(): void {
    this.view            = 'cases';
    this.activeCaseGroup = null;
    this.selectedDoc     = null;
    this.showUploadPanel = false;
  }

  // ── Filters ───────────────────────────────────────────────────────────────
  onSearch(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.applyFilters(), 300);
  }

  onTypeFilter():   void { this.applyFilters(); }
  onStatusFilter(): void { this.applyFilters(); }

  clearFilters(): void {
    this.searchKeyword = ''; this.selectedType = ''; this.selectedStatus = '';
    this.applyFilters();
  }

  private applyFilters(): void {
    this.filtered = this.documents.filter(d => {
      const kw     = this.searchKeyword.toLowerCase();
      const searchOk = !kw || d.title.toLowerCase().includes(kw) || d.fileName.toLowerCase().includes(kw);
      const typeOk   = !this.selectedType   || d.documentType === this.selectedType;
      const statusOk = !this.selectedStatus || d.status       === this.selectedStatus;
      return searchOk && typeOk && statusOk;
    });
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(doc: DocumentDto): void {
    this.selectedDoc    = doc;
    this.editStatusMode = false;
    this.newDocStatus   = doc.status ?? '';
    this.rejectionNote  = '';
  }

  closeDetail(): void { this.selectedDoc = null; this.editStatusMode = false; }

  // ── Status update ─────────────────────────────────────────────────────────
  saveDocumentStatus(): void {
    if (!this.selectedDoc || !this.newDocStatus) return;
    this.savingStatus = true;

    const body: any = { status: this.newDocStatus };
    if (this.newDocStatus === 'REJECTED' && this.rejectionNote.trim())
      body.rejectionNote = this.rejectionNote.trim();

    this.http.patch<DocumentDto>(
      `${this.base}/documents/${this.selectedDoc.id}/status`, body
    ).subscribe({
      next: updated => {
        // Update in all views
        Object.assign(this.selectedDoc!, updated);
        const di = this.documents.findIndex(d => d.id === updated.id);
        if (di > -1) this.documents[di] = updated;
        this.applyFilters();
        // Update case group pending count
        if (this.activeCaseGroup) {
          this.activeCaseGroup.pendingReview =
            this.documents.filter(d => d.status === 'UPLOADED').length;
          const cg = this.caseGroups.find(g => g.caseId === this.activeCaseGroup!.caseId);
          if (cg) cg.pendingReview = this.activeCaseGroup.pendingReview;
        }
        this.editStatusMode = false;
        this.savingStatus   = false;
        this.rejectionNote  = '';
        this.snackBar.open(`Document ${this.newDocStatus.toLowerCase()}.`, 'Close', { duration: 3000 });
      },
      error: err => {
        this.savingStatus = false;
        this.snackBar.open(err?.error?.message ?? 'Failed to update status.', 'Close', { duration: 3000 });
      }
    });
  }

  // ── Upload ────────────────────────────────────────────────────────────────
  toggleUpload(): void {
    this.showUploadPanel = !this.showUploadPanel;
    if (!this.showUploadPanel) this.resetUploadForm();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    if (this.selectedFile && !this.uploadTitle)
      this.uploadTitle = this.selectedFile.name.replace(/\.[^/.]+$/, '');
  }

  submitUpload(): void {
    if (!this.selectedFile || !this.activeCaseGroup) { this.uploadError = 'Please select a file.'; return; }
    this.uploading = true; this.uploadError = ''; this.uploadSuccess = '';

    const fd = new FormData();
    fd.append('file', this.selectedFile);
    if (this.uploadTitle) fd.append('title', this.uploadTitle);
    if (this.uploadDesc)  fd.append('description', this.uploadDesc);
    fd.append('documentType', this.uploadType);

    this.http.post<DocumentDto>(
      `${this.base}/cases/${this.activeCaseGroup.caseId}/documents/upload`, fd
    ).subscribe({
      next: doc => {
        this.documents.unshift(doc);
        this.applyFilters();
        if (this.activeCaseGroup) {
          this.activeCaseGroup.documents.unshift(doc);
          this.activeCaseGroup.docCount++;
        }
        this.uploadSuccess = `"${doc.title}" uploaded successfully.`;
        this.uploading = false;
        setTimeout(() => { this.showUploadPanel = false; this.resetUploadForm(); }, 2000);
      },
      error: err => { this.uploadError = err?.error?.message ?? 'Upload failed.'; this.uploading = false; }
    });
  }

  private resetUploadForm(): void {
    this.uploadTitle = ''; this.uploadDesc = ''; this.uploadType = 'OTHER';
    this.selectedFile = null; this.uploadError = ''; this.uploadSuccess = '';
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  deleteDocument(doc: DocumentDto): void {
    if (!confirm(`Delete "${doc.title}"?`)) return;
    this.http.delete(`${this.base}/documents/${doc.id}`).subscribe({
      next: () => {
        this.documents = this.documents.filter(d => d.id !== doc.id);
        this.applyFilters();
        if (this.activeCaseGroup) {
          this.activeCaseGroup.documents = this.activeCaseGroup.documents.filter(d => d.id !== doc.id);
          this.activeCaseGroup.docCount--;
        }
        if (this.selectedDoc?.id === doc.id) this.selectedDoc = null;
        this.snackBar.open('Document deleted.', 'Close', { duration: 3000 });
      },
      error: err => this.snackBar.open(err?.error?.message ?? 'Cannot delete.', 'Close', { duration: 3000 })
    });
  }

  // ── Download ──────────────────────────────────────────────────────────────
  download(doc: DocumentDto): void {
    this.downloading = doc.id;
    const a = document.createElement('a');
    a.href = `${this.base}/documents/${doc.id}/download`;
    a.download = doc.fileName; a.click();
    setTimeout(() => this.downloading = null, 2500);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  typeLabel(t: string | null): string   { return t ? (DOCUMENT_TYPE_LABELS[t as DocumentType] ?? t) : '—'; }
  statusLabel(s: string | null): string { return s ? (DOCUMENT_STATUS_LABELS[s as DocumentStatus] ?? s) : '—'; }

  statusClass(s: string | null): string {
    const map: Record<string, string> = {
      UPLOADED: 'badge--uploaded', PENDING_REVIEW: 'badge--pending',
      APPROVED: 'badge--approved', REJECTED: 'badge--rejected', ARCHIVED: 'badge--archived'
    };
    return map[s ?? ''] ?? 'badge--uploaded';
  }

  fileIconClass(ft: string | null): string {
    switch (ft?.toUpperCase()) {
      case 'PDF': return 'icon--pdf';
      case 'DOC': case 'DOCX': return 'icon--doc';
      case 'XLS': case 'XLSX': return 'icon--xls';
      case 'JPG': case 'JPEG': case 'PNG': return 'icon--img';
      default: return 'icon--file';
    }
  }

  formatSize(bytes: number | null): string {
    if (!bytes) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes/1024).toFixed(1)} KB`;
    return `${(bytes/1048576).toFixed(1)} MB`;
  }

  canDelete(doc: DocumentDto): boolean {
    return doc.status === 'UPLOADED' || doc.status === 'REJECTED';
  }

  get activeFilterCount(): number {
    return [this.searchKeyword, this.selectedType, this.selectedStatus].filter(v => !!v).length;
  }

  get totalDocCount(): number {
    return this.caseGroups.reduce((sum, g) => sum + g.docCount, 0);
  }

  docsByStatus(g: CaseDocGroup, status: string): number {
    return g.documents.filter(d => d.status === status).length;
  }

  get totalPendingReview(): number {
    return this.caseGroups.reduce((sum, g) => sum + g.pendingReview, 0);
  }
}