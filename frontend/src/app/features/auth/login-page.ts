import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { AuthApi } from '../../core/auth/auth.api';
import { AuthStore } from '../../core/auth/auth.store';
import { applyApiErrors, controlError } from '../../core/forms/form-errors';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, InputText, Password, Button],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly api = inject(AuthApi);
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  readonly notice = inject(ActivatedRoute).snapshot.queryParamMap.get('notice');
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.maxLength(128)]],
  });

  fieldError(name: 'email' | 'password'): string | null {
    return controlError(this.form.controls[name]);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    this.api.login({ email: value.email.trim(), password: value.password }).subscribe({
      next: (response) => {
        this.store.setSession(response);
        void this.router.navigate(['/overview']);
      },
      error: (error: unknown) => {
        this.error.set(applyApiErrors(this.form, error));
        this.submitting.set(false);
      },
    });
  }
}
