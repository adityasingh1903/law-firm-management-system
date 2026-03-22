// src/app/shared/services/document.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentDto } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {

  private base = `${environment.apiUrl}/client`;

  constructor(private http: HttpClient) {}

  /** GET /api/client/documents — all documents across all cases */
  getMyDocuments(): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>(`${this.base}/documents`);
  }

  /** GET /api/client/documents/:id */
  getDocumentDetail(id: number): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`${this.base}/documents/${id}`);
  }

  /** GET /api/client/cases/:caseId/documents */
  getDocumentsForCase(caseId: number): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>(`${this.base}/cases/${caseId}/documents`);
  }

  /** GET /api/client/documents/search?keyword=xyz */
  searchDocuments(keyword: string): Observable<DocumentDto[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<DocumentDto[]>(`${this.base}/documents/search`, { params });
  }

  /** GET /api/client/documents/filter?type=CONTRACT */
  filterByType(type: string): Observable<DocumentDto[]> {
    const params = new HttpParams().set('type', type);
    return this.http.get<DocumentDto[]>(`${this.base}/documents/filter`, { params });
  }

  /**
   * POST /api/client/cases/:caseId/documents/upload
   * Multipart upload with optional metadata.
   */
  uploadDocument(
    caseId: number,
    file: File,
    title?: string,
    description?: string,
    documentType?: string
  ): Observable<DocumentDto> {
    const form = new FormData();
    form.append('file', file);
    if (title)        form.append('title', title);
    if (description)  form.append('description', description);
    if (documentType) form.append('documentType', documentType);
    return this.http.post<DocumentDto>(
      `${this.base}/cases/${caseId}/documents/upload`, form
    );
  }

  /**
   * GET /api/client/documents/:id/download
   * Triggers a browser file download.
   */
  downloadDocument(id: number, fileName: string): void {
    this.http.get(`${this.base}/documents/${id}/download`, { responseType: 'blob' })
      .subscribe(blob => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download  = fileName;
        a.click();
        URL.revokeObjectURL(url);
      });
  }

  /** Format bytes → human-readable string */
  formatFileSize(bytes: number | null): string {
    if (!bytes) return '—';
    if (bytes < 1024)             return `${bytes} B`;
    if (bytes < 1024 * 1024)      return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}