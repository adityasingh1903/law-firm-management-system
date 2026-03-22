// src/app/lawyer/messages/lawyer-messages.component.ts
// WebSocket version — real-time, no polling

import {
  Component, OnInit, OnDestroy,
  ViewChild, ElementRef, AfterViewChecked, NgZone
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { LawyerMessageService } from '../../shared/services/lawyer-message.service';
import { WebSocketService } from '../../shared/services/websocket.service';
import { ConversationSummary, MessageDto, SendMessageRequest } from '../../shared/models/message.model';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';

interface CaseOption {
  id:         number;
  caseNumber: string;
  title:      string;
  clientId:   number | null;
  clientName: string | null;
}

@Component({
  selector: 'app-lawyer-messages',
  templateUrl: './lawyer-messages.component.html',
  styleUrls: ['./lawyer-messages.component.scss']
})
export class LawyerMessagesComponent implements OnInit, OnDestroy, AfterViewChecked {

  @ViewChild('chatScroll') chatScroll!: ElementRef;

  conversations: ConversationSummary[] = [];
  cases:         CaseOption[]          = [];
  messages:      MessageDto[]          = [];
  activeConv:    ConversationSummary | null = null;
  activeClientId: number | null        = null;

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

  private wsSubscription?: Subscription;  // active case WS subscription
  private shouldScroll = false;
  private base = `${environment.apiUrl}/lawyer`;

  constructor(
    private msgService: LawyerMessageService,
    private wsService:  WebSocketService,
    private http:       HttpClient,
    private route:      ActivatedRoute,
    private ngZone:     NgZone
  ) {}

  ngOnInit(): void {
    this.currentUserId = Number(localStorage.getItem('userId') || 0);

    // Connect WebSocket
    this.wsService.connect();

    this.loadConversations();
    this.loadCases();

    this.route.queryParams.subscribe(params => {
      if (params['caseId']) {
        const cid = Number(params['caseId']);
        setTimeout(() => this.openOrStartConversation(cid), 800);
      }
    });
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
    // Don't disconnect WS here — other components may use it.
    // The service itself manages the connection lifecycle.
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) { this.scrollToBottom(); this.shouldScroll = false; }
  }

  // ── Conversations ─────────────────────────────────────────────────────────
  loadConversations(): void {
    this.msgService.getConversationList().subscribe({
      next: convs => { this.conversations = convs; this.loadingList = false; },
      error: () => { this.errorList = 'Failed to load conversations.'; this.loadingList = false; }
    });
  }

  loadCases(): void {
    this.http.get<any[]>(`${this.base}/cases`).subscribe({
      next: cases => {
        this.cases = cases.map(c => ({
          id:         c.id,
          caseNumber: c.caseNumber,
          title:      c.title,
          clientId:   c.clientId   ?? null,
          clientName: c.clientName ?? null
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
        lawyerName: c.clientName ?? 'Client',
        lastMessage: '', lastMessageType: 'TEXT',
        lastMessageAt: new Date().toISOString(),
        lastMessageIsFromMe: false, unreadCount: 0
      };
      this.activeClientId = c.clientId;
      this.activeConv     = synthetic;
      this.messages       = [];
      this.messageGroups  = [];
      this.showNewConv    = false;
      this.subscribeToCase(caseId);
    }
  }

  selectConversation(conv: ConversationSummary): void {
    this.activeConv     = conv;
    this.messages       = [];
    this.sendError      = '';
    this.errorThread    = '';
    this.showNewConv    = false;
    this.activeClientId = null;

    const c = this.cases.find(c => c.id === conv.caseId);
    if (c?.clientId) this.activeClientId = c.clientId;

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
    // Unsubscribe from previous case
    this.wsSubscription?.unsubscribe();

    this.wsSubscription = this.wsService.subscribeToCase(caseId).subscribe({
      next: msg => {
        // Run inside Angular zone so change detection fires immediately
        this.ngZone.run(() => {
          // Ignore messages we sent ourselves (we add them locally on send)
          if (msg.senderId === this.currentUserId) return;

          // Only add if it's for the active case
          if (this.activeConv?.caseId !== caseId) {
            const conv = this.conversations.find(c => c.caseId === caseId);
            if (conv) conv.unreadCount++;
            return;
          }

          // Add incoming message to thread
          this.messages = [...this.messages, msg];
          this.messageGroups = this.msgService.groupByDate(this.messages);
          this.shouldScroll  = true;

          // Update conversation preview
          this.updateConvPreview(msg, false);
        });
      }
    });
  }

  // ── Thread ────────────────────────────────────────────────────────────────
  loadThread(conv: ConversationSummary, scroll: boolean): void {
    this.loadingThread = true;
    this.msgService.getConversation(conv.caseId).subscribe({
      next: msgs => {
        this.messages      = msgs;
        this.messageGroups = this.msgService.groupByDate(msgs);
        this.loadingThread = false;
        if (scroll) this.shouldScroll = true;

        if (!this.activeClientId)
          this.activeClientId = this.resolveClientIdFromThread();

        const c = this.conversations.find(c => c.caseId === conv.caseId);
        if (c) c.unreadCount = 0;
      },
      error: () => { this.errorThread = 'Failed to load messages.'; this.loadingThread = false; }
    });
  }

  // ── Send ──────────────────────────────────────────────────────────────────
  send(): void {
    if (!this.composeText.trim() || !this.activeConv) return;

    const clientId = this.activeClientId ?? this.resolveClientIdFromThread();
    if (!clientId) {
      this.sendError = 'Cannot determine client. Please refresh.'; return;
    }

    const request: SendMessageRequest = {
      caseId:     this.activeConv.caseId,
      receiverId: clientId,
      content:    this.composeText.trim(),
      type:       'TEXT'
    };

    const content = this.composeText.trim();
    this.composeText = '';
    this.sending     = true;
    this.sendError   = '';

    // Send via WebSocket (instant) — also saved on server via @MessageMapping
    if (this.wsService.isConnected) {
      this.wsService.sendLawyerMessage(request);

      // Optimistically add the message locally
      const optimistic: MessageDto = {
        id:           Date.now(),  // temp ID
        content,
        type:         'TEXT',
        isRead:       false,
        createdAt:    new Date().toISOString(),
        senderId:     this.currentUserId,
        senderName:   '',
        receiverId:   clientId,
        receiverName: '',
        caseId:       this.activeConv.caseId,
        attachmentUrl: null
      };
      this.messages.push(optimistic);
      this.messageGroups = this.msgService.groupByDate(this.messages);
      this.shouldScroll  = true;
      this.sending       = false;
      this.updateConvPreview(optimistic, true);

    } else {
      // Fallback to REST if WebSocket is disconnected
      this.msgService.sendMessage(request).subscribe({
        next: msg => {
          this.messages.push(msg);
          this.messageGroups = this.msgService.groupByDate(this.messages);
          this.shouldScroll  = true;
          this.sending       = false;
          this.updateConvPreview(msg, true);
        },
        error: err => {
          this.composeText = content;  // restore
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
        ...this.activeConv!,
        lastMessage: preview, lastMessageAt: msg.createdAt,
        lastMessageIsFromMe: fromMe, unreadCount: 0
      });
    }
  }

  isMyMessage(msg: MessageDto): boolean { return msg.senderId === this.currentUserId; }

  formatTime(iso: string):      string { return this.msgService.formatMessageTime(iso); }
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

  get otherPartyName(): string { return this.activeConv?.lawyerName ?? ''; }

  get allCasesForNewConv(): CaseOption[] { return this.cases.filter(c => c.clientId); }

  private resolveClientIdFromThread(): number | null {
    const fromClient = this.messages.find(m => m.senderId !== this.currentUserId);
    if (fromClient) return fromClient.senderId;
    const fromMe = this.messages.find(m => m.senderId === this.currentUserId);
    if (fromMe) return fromMe.receiverId;
    return null;
  }

  private scrollToBottom(): void {
    try { const el = this.chatScroll?.nativeElement; if (el) el.scrollTop = el.scrollHeight; } catch {}
  }
}