// src/app/client/profile/client-profile.component.ts

import { Component, OnInit } from '@angular/core';
import { ProfileService } from '../../shared/services/profile.service';
import {
  ChangePasswordRequest, UpdateProfileRequest, UserDto
} from '../../shared/models/profile.model';

@Component({
  selector: 'app-client-profile',
  templateUrl: './client-profile.component.html',
  styleUrls: ['./client-profile.component.scss']
})
export class ClientProfileComponent implements OnInit {

  // ── State ─────────────────────────────────────────────────────────────────
  profile: UserDto | null = null;
  loading  = true;
  error    = '';

  // ── Active section ────────────────────────────────────────────────────────
  activeSection: 'info' | 'security' = 'info';

  // ── Edit profile form ─────────────────────────────────────────────────────
  editMode      = false;
  saving        = false;
  saveSuccess   = '';
  saveError     = '';

  editForm: UpdateProfileRequest = {
    firstName: '', lastName: '', email: '', phoneNumber: '', address: ''
  };

  // ── Change password form ──────────────────────────────────────────────────
  pwForm: ChangePasswordRequest = {
    currentPassword: '', newPassword: '', confirmPassword: ''
  };
  pwSaving   = false;
  pwSuccess  = '';
  pwError    = '';
  showCurrent = false;
  showNew     = false;
  showConfirm = false;

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  loadProfile(): void {
    this.profileService.getProfile().subscribe({
      next: p => {
        this.profile = p;
        this.loading = false;
        this.populateForm(p);
      },
      error: () => {
        this.error   = 'Failed to load profile.';
        this.loading = false;
      }
    });
  }

  private populateForm(p: UserDto): void {
    this.editForm = {
      firstName:   p.firstName,
      lastName:    p.lastName,
      email:       p.email,
      phoneNumber: p.phoneNumber ?? '',
      address:     p.address ?? '',
    };
  }

  // ── Edit profile ──────────────────────────────────────────────────────────
  startEdit(): void {
    if (this.profile) this.populateForm(this.profile);
    this.saveSuccess = '';
    this.saveError   = '';
    this.editMode    = true;
  }

  cancelEdit(): void {
    this.editMode  = false;
    this.saveError = '';
    if (this.profile) this.populateForm(this.profile);
  }

  saveProfile(): void {
    if (!this.editForm.firstName.trim() || !this.editForm.lastName.trim() || !this.editForm.email.trim()) {
      this.saveError = 'First name, last name and email are required.';
      return;
    }

    this.saving    = true;
    this.saveError = '';

    this.profileService.updateProfile(this.editForm).subscribe({
      next: updated => {
        this.profile     = updated;
        this.saving      = false;
        this.editMode    = false;
        this.saveSuccess = 'Profile updated successfully.';

        // Update localStorage so topbar reflects name change
        localStorage.setItem('firstName', updated.firstName);
        localStorage.setItem('lastName',  updated.lastName);

        setTimeout(() => this.saveSuccess = '', 4000);
      },
      error: err => {
        this.saveError = err?.error?.message ?? 'Failed to update profile.';
        this.saving    = false;
      }
    });
  }

  // ── Change password ───────────────────────────────────────────────────────
  submitPasswordChange(): void {
    if (!this.pwForm.currentPassword || !this.pwForm.newPassword || !this.pwForm.confirmPassword) {
      this.pwError = 'All password fields are required.';
      return;
    }
    if (this.pwForm.newPassword !== this.pwForm.confirmPassword) {
      this.pwError = 'New password and confirm password do not match.';
      return;
    }
    if (this.pwForm.newPassword.length < 6) {
      this.pwError = 'New password must be at least 6 characters.';
      return;
    }

    this.pwSaving = true;
    this.pwError  = '';
    this.pwSuccess = '';

    this.profileService.changePassword(this.pwForm).subscribe({
      next: () => {
        this.pwSaving  = false;
        this.pwSuccess = 'Password changed successfully.';
        this.resetPwForm();
        setTimeout(() => this.pwSuccess = '', 4000);
      },
      error: err => {
        this.pwError  = err?.error?.message ?? 'Failed to change password.';
        this.pwSaving = false;
      }
    });
  }

  private resetPwForm(): void {
    this.pwForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
    this.showCurrent = false;
    this.showNew     = false;
    this.showConfirm = false;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get initials(): string {
    if (!this.profile) return '?';
    return this.profileService.getInitials(this.profile.firstName, this.profile.lastName);
  }

  get fullName(): string {
    if (!this.profile) return '';
    return `${this.profile.firstName} ${this.profile.lastName}`;
  }

  get memberSince(): string {
    if (!this.profile?.createdAt) return '';
    return this.profileService.formatMemberSince(this.profile.createdAt);
  }

  get passwordStrength(): { label: string; level: number } {
    const pw = this.pwForm.newPassword;
    if (!pw) return { label: '', level: 0 };
    let score = 0;
    if (pw.length >= 8)             score++;
    if (/[A-Z]/.test(pw))           score++;
    if (/[0-9]/.test(pw))           score++;
    if (/[^A-Za-z0-9]/.test(pw))    score++;
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    return { label: labels[score] ?? 'Strong', level: score };
  }
}