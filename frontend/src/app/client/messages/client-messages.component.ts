// src/app/client/messages/client-messages.component.ts
// WebSocket version — real-time, no polling

import {
  Component, OnInit, OnDestroy,
  ViewChild, ElementRef, AfterViewChecked, NgZone
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MessageService } from '../../shared/services/message.service';
import { WebSocketService } from '../../shared/services/websocket.service';
import { ConversationSummary, MessageDto, SendMessageRequest } from '../../shared/models/message.model';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';

interface CaseOption {
  id:         number;
  caseNumber: string;
  title:      string;
  lawyerId:   number | null;
  lawyerName: string | null;
}

@Component({
  selector: 'app-client-messages',
  templateUrl: './client-messages.component.html',
  styleUrls: ['./client-messages.component.scss']
})
export class ClientMessagesComponent implements OnInit, OnDestroy, AfterViewChecked {

  @ViewChild('chatScroll') chatScroll!: ElementRef;

  conversations: ConversationSummary[] = [];
  cases:         CaseOption[]          = [];
  messages:      MessageDto[]          = [];
  activeConv:    ConversationSummary | null = null;
  activeLawyerId: number | null        = null;

  loadingList   = true;
  loadingThread = false;
  sending       = false;
  errorList     = '';
  errorThread   = '';
  sendError     = '';

  composeText   = '';
  currentUserId = 0;

  messageGroups: { date: string; messages: MessageDto[] }[] = [];

  showNewConv     = false;
  selectedCaseId: number | null = null;

  private wsSubscription?: Subscription;
  private shouldScroll = false;
  private base = `${environment.apiUrl}/client`;

  constructor(
    private messageService: MessageService,
    private wsService:      WebSocketService,
    private http:           HttpClient,
    private ngZone:         NgZone
  ) {}

  ngOnInit(): void {
    this.currentUserId = Number(localStorage.getItem('userId') || 0);

    // Connect WebSocket
    this.wsService.connect();

    this.loadConversations();
    this.loadCases();
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) { this.scrollToBottom(); this.shouldScroll = false; }
  }

  // ── Conversations ─────────────────────────────────────────────────────────
  loadConversations(): void {
    this.messageService.getConversationList().subscribe({
      next: convs => { this.conversations = convs; this.loadingList = false; },
      error: () => { this.errorList = 'Failed to load conversations.'; this.loadingList = false; }
    });
  }

  loadCases(): void {
    this.http.get<any[]>(`${this.base}/cases`).subscribe({
      next: cases => {
        this.cases = cases
          .filter(c => c.status === 'OPEN' || c.status === 'IN_PROGRESS')
          .map(c => ({
            id: c.id, caseNumber: c.caseNumber, title: c.title,
            lawyerId: c.lawyerId ?? null, lawyerName: c.lawyerName ?? null
          }));
      },
      error: () => {}
    });
  }

  openOrStartConversation(caseId: number): void {
    const existing = this.conversations.find(c => c.caseId === caseId);
    if (existing) { this.selectConversation(existing); return; }

    const c = this.cases.find(c => c.id === caseId);
    if (c) {
      const synthetic: ConversationSummary = {
        caseId: c.id, caseTitle: c.title, caseNumber: c.caseNumber,
        lawyerName: c.lawyerName ?? 'Lawyer',
        lastMessage: '', lastMessageType: 'TEXT',
        lastMessageAt: new Date().toISOString(),
        lastMessageIsFromMe: false, unreadCount: 0
      };
      this.activeLawyerId = c.lawyerId;
      this.activeConv     = synthetic;
      this.messages       = [];
      this.messageGroups  = [];
      this.showNewConv    = false;
      this.subscribeToCase(caseId);
    }
  }

  selectConversation(conv: ConversationSummary): void {
    this.activeConv      = conv;
    this.messages        = [];
    this.sendError       = '';
    this.errorThread     = '';
    this.showNewConv     = false;
    this.activeLawyerId  = null;

    const c = this.cases.find(c => c.id === conv.caseId);
    if (c?.lawyerId) this.activeLawyerId = c.lawyerId;

    this.loadThread(conv, true);
    this.subscribeToCase(conv.caseId);
  }

  startNewConversation(): void {
    this.showNewConv = true; this.selectedCaseId = null; this.activeConv = null;
  }

  confirmNewConversation(): void {
    if (!this.selectedCaseId) return;
    this.showNewConv = false;
    this.openOrStartConversation(this.selectedCaseId);
  }

  // ── Subscribe to WebSocket for this case ──────────────────────────────────
  private subscribeToCase(caseId: number): void {
    this.wsSubscription?.unsubscribe();

    this.wsSubscription = this.wsService.subscribeToCase(caseId).subscribe({
      next: msg => {
        this.ngZone.run(() => {
          if (msg.senderId === this.currentUserId) return;

          if (this.activeConv?.caseId !== caseId) {
            const conv = this.conversations.find(c => c.caseId === caseId);
            if (conv) conv.unreadCount++;
            return;
          }

          this.messages = [...this.messages, msg];
          this.messageGroups = this.messageService.groupByDate(this.messages);
          this.shouldScroll  = true;
          this.updateConvPreview(msg, false);
        });
      }
    });
  }

  // ── Thread ────────────────────────────────────────────────────────────────
  loadThread(conv: ConversationSummary, scroll: boolean): void {
    this.loadingThread = true;
    this.messageService.getConversation(conv.caseId).subscribe({
      next: msgs => {
        this.messages      = msgs;
        this.messageGroups = this.messageService.groupByDate(msgs);
        this.loadingThread = false;
        if (scroll) this.shouldScroll = true;

        if (!this.activeLawyerId)
          this.activeLawyerId = this.resolveLawyerIdFromThread();

        const c = this.conversations.find(c => c.caseId === conv.caseId);
        if (c) c.unreadCount = 0;
      },
      error: () => { this.errorThread = 'Failed to load messages.'; this.loadingThread = false; }
    });
  }

  // ── Send ──────────────────────────────────────────────────────────────────
  send(): void {
    if (!this.composeText.trim() || !this.activeConv) return;

    const lawyerId = this.activeLawyerId ?? this.resolveLawyerIdFromThread();
    if (!lawyerId) {
      this.sendError = 'Cannot determine lawyer. Please refresh.'; return;
    }

    const request: SendMessageRequest = {
      caseId: this.activeConv.caseId, receiverId: lawyerId,
      content: this.composeText.trim(), type: 'TEXT'
    };

    const content    = this.composeText.trim();
    this.composeText = '';
    this.sending     = true;
    this.sendError   = '';

    if (this.wsService.isConnected) {
      this.wsService.sendClientMessage(request);

      // Optimistic update
      const optimistic: MessageDto = {
        id: Date.now(), content, type: 'TEXT', isRead: false,
        createdAt: new Date().toISOString(),
        senderId: this.currentUserId, senderName: '',
        receiverId: lawyerId, receiverName: '',
        caseId: this.activeConv.caseId, attachmentUrl: null
      };
      this.messages.push(optimistic);
      this.messageGroups = this.messageService.groupByDate(this.messages);
      this.shouldScroll  = true;
      this.sending       = false;
      this.updateConvPreview(optimistic, true);

    } else {
      // REST fallback
      this.messageService.sendMessage(request).subscribe({
        next: msg => {
          this.messages.push(msg);
          this.messageGroups = this.messageService.groupByDate(this.messages);
          this.shouldScroll  = true;
          this.sending       = false;
          this.updateConvPreview(msg, true);
        },
        error: err => {
          this.composeText = content;
          this.sendError   = err?.error?.message ?? 'Failed to send.';
          this.sending     = false;
        }
      });
    }
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault(); this.send();
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  private updateConvPreview(msg: MessageDto, fromMe: boolean): void {
    const preview = msg.content.length > 60 ? msg.content.substring(0, 60) + '…' : msg.content;
    const existing = this.conversations.find(c => c.caseId === this.activeConv!.caseId);
    if (existing) {
      existing.lastMessage = preview; existing.lastMessageAt = msg.createdAt;
      existing.lastMessageIsFromMe = fromMe;
    } else {
      this.conversations.unshift({
        ...this.activeConv!, lastMessage: preview,
        lastMessageAt: msg.createdAt, lastMessageIsFromMe: fromMe, unreadCount: 0
      });
    }
  }

  isMyMessage(msg: MessageDto): boolean { return msg.senderId === this.currentUserId; }

  formatTime(iso: string): string      { return this.messageService.formatMessageTime(iso); }
  formatTimeShort(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }

  totalUnread(): number {
    return this.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
  }

  get allCasesForNewConv(): CaseOption[] { return this.cases.filter(c => c.lawyerId); }

  private resolveLawyerIdFromThread(): number | null {
    const fromLawyer = this.messages.find(m => m.senderId !== this.currentUserId);
    if (fromLawyer) return fromLawyer.senderId;
    const fromMe = this.messages.find(m => m.senderId === this.currentUserId);
    if (fromMe) return fromMe.receiverId;
    return null;
  }

  private scrollToBottom(): void {
    try { const el = this.chatScroll?.nativeElement; if (el) el.scrollTop = el.scrollHeight; } catch {}
  }
}