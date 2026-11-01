import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { AuthApi } from '../../core/auth/auth.api';
import { AuthStore } from '../../core/auth/auth.store';
import { WorkspaceApi } from '../../core/api/workspace.api';
import { PASSWORD_MESSAGE, PASSWORD_PATTERN, applyApiErrors, controlError } from '../../core/forms/form-errors';

@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule, InputText, Password, Button],
  templateUrl: './settings-page.html',
})
export class SettingsPage {
  private readonly auth = inject(AuthApi);
  private readonly workspace = inject(WorkspaceApi);
  private readonly router = inject(Router);
  readonly store = inject(AuthStore);
  readonly passwordMessage = PASSWORD_MESSAGE;
  readonly companyMessage = signal<string | null>(null);
  readonly companyError = signal<string | null>(null);
  readonly profileMessage = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);
  readonly savingCompany = signal(false);
  readonly savingProfile = signal(false);
  readonly savingPassword = signal(false);
  readonly companyForm = inject(FormBuilder).nonNullable.group({
    name: [this.store.tenant()?.name ?? '', [Validators.required, Validators.maxLength(200)]],
  });
  readonly profileForm = inject(FormBuilder).nonNullable.group({
    firstName: [this.store.user()?.firstName ?? '', [Validators.required, Validators.maxLength(120)]],
    lastName: [this.store.user()?.lastName ?? '', [Validators.required, Validators.maxLength(120)]],
  });
  readonly passwordForm = inject(FormBuilder).nonNullable.group({
    currentPassword: ['', [Validators.required, Validators.maxLength(128)]],
    newPassword: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128), Validators.pattern(PASSWORD_PATTERN)]],
  });

  canRename(): boolean {
    return this.store.has('tenant:write');
  }

  companyFieldError(): string | null {
    return controlError(this.companyForm.controls.name);
  }

  profileFieldError(name: 'firstName' | 'lastName'): string | null {
    return controlError(this.profileForm.controls[name]);
  }

  passwordFieldError(name: 'currentPassword' | 'newPassword'): string | null {
    return controlError(this.passwordForm.controls[name], name === 'newPassword' ? PASSWORD_MESSAGE : undefined);
  }

  saveCompany(): void {
    if (this.companyForm.invalid) {
      this.companyForm.markAllAsTouched();
      return;
    }
    this.savingCompany.set(true);
    this.companyError.set(null);
    this.companyMessage.set(null);
    this.workspace.updateTenant(this.companyForm.getRawValue().name.trim()).subscribe({
      next: (tenant) => {
        this.store.tenant.set(tenant);
        this.companyMessage.set('Company name saved.');
        this.savingCompany.set(false);
      },
      error: (error: unknown) => {
        this.companyError.set(applyApiErrors(this.companyForm, error));
        this.savingCompany.set(false);
      },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileMessage.set(null);
    const value = this.profileForm.getRawValue();
    this.auth.updateProfile({ firstName: value.firstName.trim(), lastName: value.lastName.trim() }).subscribe({
      next: (user) => {
        this.store.setUser(user);
        this.profileMessage.set('Profile saved.');
        this.savingProfile.set(false);
      },
      error: (error: unknown) => {
        this.profileError.set(applyApiErrors(this.profileForm, error));
        this.savingProfile.set(false);
      },
    });
  }

  savePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.passwordError.set(null);
    const value = this.passwordForm.getRawValue();
    this.auth.changePassword({ currentPassword: value.currentPassword, newPassword: value.newPassword }).subscribe({
      next: () => {
        this.store.clear();
        void this.router.navigate(['/login'], { queryParams: { notice: 'password' } });
      },
      error: (error: unknown) => {
        this.passwordError.set(applyApiErrors(this.passwordForm, error));
        this.savingPassword.set(false);
      },
    });
  }
}
