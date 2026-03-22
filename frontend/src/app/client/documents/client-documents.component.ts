// src/app/client/documents/client-documents.component.ts

import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DocumentService } from '../../shared/services/document.service';
import {
  DocumentDto, DocumentType, DocumentStatus,
  DOCUMENT_TYPE_LABELS, DOCUMENT_STATUS_LABELS
} from '../../shared/models/document.model';
import { environment } from '../../../environments/environment';

interface CaseOption {
  id:         number;
  caseNumber: string;
  title:      string;
  status:     string;
}

interface CaseDocGroup {
  caseId:     number;
  caseNumber: string;
  caseTitle:  string;
  caseStatus: string;
  documents:  DocumentDto[];
  docCount:   number;
}

@Component({
  selector: 'app-client-documents',
  templateUrl: './client-documents.component.html',
  styleUrls:  ['./client-documents.component.scss']
})
export class ClientDocumentsComponent implements OnInit {

  // ── View state ────────────────────────────────────────────────────────────
  view: 'cases' | 'documents' = 'cases';   // cases = overview, documents = drilled into a case

  // ── Cases overview ────────────────────────────────────────────────────────
  caseGroups:    CaseDocGroup[] = [];
  loadingCases   = true;
  casesError     = '';

  // ── Document list (after drilling into a case) ────────────────────────────
  activeCaseGroup: CaseDocGroup | null = null;
  documents:  DocumentDto[] = [];
  filtered:   DocumentDto[] = [];
  loadingDocs = false;
  docsError   = '';

  // ── Filters (in document view) ────────────────────────────────────────────
  searchKeyword  = '';
  selectedType   = '';
  selectedStatus = '';
  searchTimeout: any;

  // ── Selected document (detail panel) ─────────────────────────────────────
  selectedDoc: DocumentDto | null = null;

  // ── Upload panel ──────────────────────────────────────────────────────────
  showUploadPanel = false;
  uploadCaseId:   number | null = null;
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

  constructor(
    private docService: DocumentService,
    private http:       HttpClient
  ) {}

  ngOnInit(): void { this.loadCaseGroups(); }

  // ── Load all docs grouped by case ─────────────────────────────────────────
  loadCaseGroups(): void {
    this.loadingCases = true;
    this.docService.getMyDocuments().subscribe({
      next: docs => {
        this.caseGroups   = this.groupByCase(docs);
        this.loadingCases = false;
      },
      error: () => {
        this.casesError   = 'Failed to load documents.';
        this.loadingCases = false;
      }
    });
  }

  private groupByCase(docs: DocumentDto[]): CaseDocGroup[] {
    const map = new Map<number, CaseDocGroup>();
    for (const d of docs) {
      const cid = d.caseId ?? 0;
      if (!map.has(cid)) {
        map.set(cid, {
          caseId:     cid,
          caseNumber: d.caseNumber ?? '—',
          caseTitle:  d.caseTitle  ?? 'Unknown Case',
          caseStatus: '',
          documents:  [],
          docCount:   0
        });
      }
      map.get(cid)!.documents.push(d);
      map.get(cid)!.docCount++;
    }
    return Array.from(map.values())
      .sort((a, b) => a.caseNumber.localeCompare(b.caseNumber));
  }

  // ── Drill into a case ─────────────────────────────────────────────────────
  openCase(group: CaseDocGroup): void {
    this.activeCaseGroup = group;
    this.view            = 'documents';
    this.documents       = [...group.documents];
    this.filtered        = [...group.documents];
    this.selectedDoc     = null;
    this.searchKeyword   = '';
    this.selectedType    = '';
    this.selectedStatus  = '';
    // Pre-select case for upload
    this.uploadCaseId    = group.caseId;
  }

  // ── Back to cases view ────────────────────────────────────────────────────
  backToCases(): void {
    this.view            = 'cases';
    this.activeCaseGroup = null;
    this.selectedDoc     = null;
    this.showUploadPanel = false;
  }

  // ── Filtering (inside a case) ─────────────────────────────────────────────
  onSearch(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.applyFilters(), 300);
  }

  onTypeFilter():   void { this.applyFilters(); }
  onStatusFilter(): void { this.applyFilters(); }

  clearFilters(): void {
    this.searchKeyword  = '';
    this.selectedType   = '';
    this.selectedStatus = '';
    this.applyFilters();
  }

  private applyFilters(): void {
    this.filtered = this.documents.filter(d => {
      const kw     = this.searchKeyword.toLowerCase();
      const searchOk = !kw || d.title.toLowerCase().includes(kw)
                            || d.fileName.toLowerCase().includes(kw);
      const typeOk   = !this.selectedType   || d.documentType === this.selectedType;
      const statusOk = !this.selectedStatus || d.status       === this.selectedStatus;
      return searchOk && typeOk && statusOk;
    });
  }

  // ── Detail panel ──────────────────────────────────────────────────────────
  openDetail(doc: DocumentDto): void  { this.selectedDoc = doc; }
  closeDetail(): void                  { this.selectedDoc = null; }

  // ── Upload ────────────────────────────────────────────────────────────────
  toggleUpload(): void {
    this.showUploadPanel = !this.showUploadPanel;
    if (!this.showUploadPanel) this.resetUploadForm();
    else this.uploadCaseId = this.activeCaseGroup?.caseId ?? null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    if (this.selectedFile && !this.uploadTitle)
      this.uploadTitle = this.selectedFile.name.replace(/\.[^/.]+$/, '');
  }

  submitUpload(): void {
    if (!this.selectedFile || !this.uploadCaseId) {
      this.uploadError = 'Please select a file.'; return;
    }
    this.uploading    = true;
    this.uploadError  = '';
    this.uploadSuccess = '';

    this.docService.uploadDocument(
      this.uploadCaseId, this.selectedFile,
      this.uploadTitle, this.uploadDesc, this.uploadType
    ).subscribe({
      next: doc => {
        // Add to current view
        this.documents.unshift(doc);
        this.applyFilters();
        // Update case group count
        if (this.activeCaseGroup) {
          this.activeCaseGroup.documents.unshift(doc);
          this.activeCaseGroup.docCount++;
        }
        this.uploadSuccess = `"${doc.title}" uploaded successfully.`;
        this.uploading = false;
        setTimeout(() => { this.showUploadPanel = false; this.resetUploadForm(); }, 2000);
      },
      error: err => {
        this.uploadError = err?.error?.message ?? 'Upload failed.';
        this.uploading   = false;
      }
    });
  }

  private resetUploadForm(): void {
    this.uploadTitle   = '';
    this.uploadDesc    = '';
    this.uploadType    = 'OTHER';
    this.selectedFile  = null;
    this.uploadError   = '';
    this.uploadSuccess = '';
  }

  // ── Download ──────────────────────────────────────────────────────────────
  download(doc: DocumentDto): void {
    this.downloading = doc.id;
    this.docService.downloadDocument(doc.id, doc.fileName);
    setTimeout(() => this.downloading = null, 2500);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  formatSize(bytes: number | null): string {
    return this.docService.formatFileSize(bytes);
  }

  typeLabel(t: string | null): string {
    return t ? (DOCUMENT_TYPE_LABELS[t as DocumentType] ?? t) : '—';
  }

  statusLabel(s: string | null): string {
    return s ? (DOCUMENT_STATUS_LABELS[s as DocumentStatus] ?? s) : '—';
  }

  statusClass(s: string | null): string {
    const map: Record<string, string> = {
      UPLOADED: 'badge--uploaded', PENDING_REVIEW: 'badge--pending',
      APPROVED: 'badge--approved', REJECTED: 'badge--rejected', ARCHIVED: 'badge--archived'
    };
    return map[s ?? ''] ?? 'badge--uploaded';
  }

  caseStatusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'cs--open', IN_PROGRESS: 'cs--progress',
      CLOSED: 'cs--closed', SETTLED: 'cs--closed'
    };
    return map[s] ?? 'cs--open';
  }

  fileIconClass(ft: string | null): string {
    switch (ft?.toUpperCase()) {
      case 'PDF':  return 'icon--pdf';
      case 'DOC': case 'DOCX': return 'icon--doc';
      case 'XLS': case 'XLSX': return 'icon--xls';
      case 'JPG': case 'JPEG': case 'PNG': return 'icon--img';
      default: return 'icon--file';
    }
  }

  get activeFilterCount(): number {
    return [this.searchKeyword, this.selectedType, this.selectedStatus].filter(v => !!v).length;
  }

  docsByStatus(g: CaseDocGroup, status: string): number {
    return g.documents.filter(d => d.status === status).length;
  }

  get totalDocCount(): number {
    return this.caseGroups.reduce((sum, g) => sum + g.docCount, 0);
  }
}