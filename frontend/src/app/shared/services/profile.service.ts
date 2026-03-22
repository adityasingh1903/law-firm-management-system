// src/app/shared/services/profile.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChangePasswordRequest, UpdateProfileRequest, UserDto
} from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {

  private base = `${environment.apiUrl}/client/profile`;

  constructor(private http: HttpClient) {}

  /** GET /api/client/profile */
  getProfile(): Observable<UserDto> {
    return this.http.get<UserDto>(this.base);
  }

  /** PUT /api/client/profile */
  updateProfile(request: UpdateProfileRequest): Observable<UserDto> {
    return this.http.put<UserDto>(this.base, request);
  }

  /** PUT /api/client/profile/change-password */
  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.base}/change-password`, request);
  }

  /** Get initials from full name for avatar */
  getInitials(firstName: string, lastName: string): string {
    return `${firstName?.charAt(0) ?? ''}${lastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  /** Format ISO date → "Member since Jan 2025" */
  formatMemberSince(iso: string): string {
    return 'Member since ' + new Date(iso).toLocaleDateString('en-IN', {
      month: 'short', year: 'numeric'
    });
  }
}