// src/app/shared/services/websocket.service.ts

import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
declare var SockJS: any;  // loaded via angular.json scripts[]
import { Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { MessageDto } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

  private rxStomp: RxStomp = new RxStomp();
  private connected = false;

  // Emits whenever the connection state changes
  readonly connected$ = new Subject<boolean>();

  connect(): void {
    if (this.connected) return;

    const token = localStorage.getItem('token') ?? '';

    const config: RxStompConfig = {
      // SockJS factory — points to Spring Boot /ws endpoint
      webSocketFactory: () => new SockJS(`${environment.apiUrl.replace('/api', '')}/ws`),

      // Send JWT on the STOMP CONNECT frame
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },

      // Heartbeat — keep connection alive
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      // Reconnect automatically after 5s on disconnect
      reconnectDelay: 5000,

      // Uncomment to see STOMP frames in browser console:
      // debug: (msg) => console.log('[STOMP]', msg),
    };

    this.rxStomp.configure(config);
    this.rxStomp.activate();
    this.connected = true;

    this.rxStomp.connected$.subscribe(() => this.connected$.next(true));
    this.rxStomp.connectionState$.subscribe(state => { if (state === 0) this.connected$.next(false); });
  }

  disconnect(): void {
    this.rxStomp.deactivate();
    this.connected = false;
  }

  /**
   * Subscribe to real-time messages for a specific case.
   * Both lawyer and client subscribe to /topic/case.{caseId}
   */
  subscribeToCase(caseId: number): Observable<MessageDto> {
    return this.rxStomp
      .watch(`/topic/case.${caseId}`)
      .pipe(map(frame => JSON.parse(frame.body) as MessageDto));
  }

  /**
   * Send a message via WebSocket (client → /app/chat.client.send)
   */
  sendClientMessage(payload: object): void {
    this.rxStomp.publish({
      destination: '/app/chat.client.send',
      body: JSON.stringify(payload)
    });
  }

  /**
   * Send a message via WebSocket (lawyer → /app/chat.lawyer.send)
   */
  sendLawyerMessage(payload: object): void {
    this.rxStomp.publish({
      destination: '/app/chat.lawyer.send',
      body: JSON.stringify(payload)
    });
  }

  get isConnected(): boolean {
    return this.rxStomp.connected();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}