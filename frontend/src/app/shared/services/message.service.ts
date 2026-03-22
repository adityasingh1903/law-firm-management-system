// src/app/shared/services/message.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConversationSummary, MessageDto, SendMessageRequest } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class MessageService {

  private base = `${environment.apiUrl}/client`;

  constructor(private http: HttpClient) {}

  /** GET /api/client/messages — conversation list (one per case) */
  getConversationList(): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(`${this.base}/messages`);
  }

  /** GET /api/client/cases/:caseId/messages — full thread */
  getConversation(caseId: number): Observable<MessageDto[]> {
    return this.http.get<MessageDto[]>(`${this.base}/cases/${caseId}/messages`);
  }

  /** POST /api/client/messages/send */
  sendMessage(request: SendMessageRequest): Observable<MessageDto> {
    return this.http.post<MessageDto>(`${this.base}/messages/send`, request);
  }

  /** GET /api/client/messages/unread-count */
  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/messages/unread-count`);
  }

  /** Format ISO date → "Today 9:42 AM" / "Mon 9:42 AM" / "24 Mar" */
  formatMessageTime(iso: string): string {
    const date = new Date(iso);
    const now  = new Date();
    const isToday = date.toDateString() === now.toDateString();
    const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1);
    const isYesterday = date.toDateString() === yesterday.toDateString();
    const time = date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });

    if (isToday)     return time;
    if (isYesterday) return `Yesterday ${time}`;

    const diffDays = Math.floor((now.getTime() - date.getTime()) / 86400000);
    if (diffDays < 7) {
      return date.toLocaleDateString('en-IN', { weekday: 'short' }) + ' ' + time;
    }
    return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  }

  /** Group messages by date for dividers */
  groupByDate(messages: MessageDto[]): { date: string; messages: MessageDto[] }[] {
    const groups: Record<string, MessageDto[]> = {};
    for (const m of messages) {
      const d    = new Date(m.createdAt);
      const now  = new Date();
      const isToday     = d.toDateString() === now.toDateString();
      const yesterday   = new Date(now); yesterday.setDate(now.getDate() - 1);
      const isYesterday = d.toDateString() === yesterday.toDateString();

      const label = isToday ? 'Today'
                 : isYesterday ? 'Yesterday'
                 : d.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });

      if (!groups[label]) groups[label] = [];
      groups[label].push(m);
    }
    return Object.entries(groups).map(([date, messages]) => ({ date, messages }));
  }
}