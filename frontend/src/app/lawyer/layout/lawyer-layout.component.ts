// src/app/lawyer/layout/lawyer-layout.component.ts

import {
  Component, OnInit, OnDestroy, HostListener
} from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../shared/services/auth.service';
import { filter } from 'rxjs/operators';
import { Subscription, interval } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-lawyer-layout',
  templateUrl: './lawyer-layout.component.html',
  styleUrls: ['./lawyer-layout.component.scss']
})
export class LawyerLayoutComponent implements OnInit, OnDestroy {

  // ── Sidebar state ─────────────────────────────────────────────────────────
  isExpanded  = true;
  isMobile    = false;
  mobileOpen  = false;
  userMenuOpen = false;

  // ── User info ─────────────────────────────────────────────────────────────
  today         = new Date();
  userName      = '';
  userInitials  = '';
  userEmail     = '';
  currentPageLabel = 'Dashboard';

  // ── Badges ────────────────────────────────────────────────────────────────
  unreadCount      = 0;   // unread messages
  activeCaseCount  = 0;   // open/in-progress cases
  todayHearings    = 0;   // hearings scheduled today

  private navRoutes = [
    { label: 'Dashboard', route: '/lawyer/dashboard'  },
    { label: 'My Cases',  route: '/lawyer/cases'      },
    { label: 'Hearings',  route: '/lawyer/hearings'   },
    { label: 'Documents', route: '/lawyer/documents'  },
    { label: 'Messages',  route: '/lawyer/messages'   },
    { label: 'Clients',   route: '/lawyer/clients'    },
    { label: 'Profile',   route: '/lawyer/profile'    },
  ];

  private routerSub?: Subscription;
  private pollSub?:   Subscription;

  constructor(
    private router:      Router,
    private http:        HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.checkScreen();
    this.loadUserInfo();
    this.fetchBadges();
    this.updateBreadcrumb(this.router.url);

    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.updateBreadcrumb(e.urlAfterRedirects));

    // Poll unread count every 30 seconds
    this.pollSub = interval(30000).subscribe(() => this.fetchUnreadCount());
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.pollSub?.unsubscribe();
  }

  // ── Screen size ───────────────────────────────────────────────────────────
  @HostListener('window:resize')
  onResize(): void { this.checkScreen(); }

  checkScreen(): void {
    this.isMobile = window.innerWidth < 768;
    if (this.isMobile) { this.mobileOpen = false; }
  }

  // ── Sidebar ───────────────────────────────────────────────────────────────
  toggleSidebar(): void {
    if (this.isMobile) this.mobileOpen = !this.mobileOpen;
    else               this.isExpanded = !this.isExpanded;
  }

  openMobile():  void { this.mobileOpen = true; }
  closeMobile(): void { this.mobileOpen = false; }

  // ── User menu ─────────────────────────────────────────────────────────────
  toggleUserMenu(): void { this.userMenuOpen = !this.userMenuOpen; }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('.user-menu-wrap'))
      this.userMenuOpen = false;
  }

  // ── User info ─────────────────────────────────────────────────────────────
  private loadUserInfo(): void {
    const firstName = localStorage.getItem('firstName') || '';
    const lastName  = localStorage.getItem('lastName')  || '';
    this.userName    = firstName ? `${firstName} ${lastName}`.trim() : 'Lawyer';
    this.userInitials = `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase() || 'LW';
    this.userEmail   = localStorage.getItem('email') || '';
  }

  // ── Badges ────────────────────────────────────────────────────────────────
  private fetchBadges(): void {
    this.fetchUnreadCount();
    this.fetchActiveCaseCount();
    this.fetchTodayHearings();
  }

  private get authHeaders(): HttpHeaders {
    const token = localStorage.getItem('authToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  private fetchUnreadCount(): void {
    this.http.get<number>(
      `${environment.apiUrl}/lawyer/messages/unread-count`,
      { headers: this.authHeaders }
    ).subscribe({ next: n => this.unreadCount = n, error: () => {} });
  }

  private fetchActiveCaseCount(): void {
    this.http.get<any[]>(
      `${environment.apiUrl}/lawyer/cases`,
      { headers: this.authHeaders }
    ).subscribe({
      next: cases => {
        this.activeCaseCount = cases.filter(
          c => c.status === 'OPEN' || c.status === 'IN_PROGRESS'
        ).length;
      },
      error: () => {}
    });
  }

  private fetchTodayHearings(): void {
    this.http.get<any[]>(
      `${environment.apiUrl}/lawyer/hearings/today`,
      { headers: this.authHeaders }
    ).subscribe({ next: h => this.todayHearings = h.length, error: () => {} });
  }

  // ── Breadcrumb ────────────────────────────────────────────────────────────
  private updateBreadcrumb(url: string): void {
    const match = this.navRoutes.find(item => url.startsWith(item.route));
    this.currentPageLabel = match ? match.label : 'Dashboard';
  }

  // ── Logout ────────────────────────────────────────────────────────────────
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}