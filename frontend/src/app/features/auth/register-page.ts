import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { AuthApi } from '../../core/auth/auth.api';
import { AuthStore } from '../../core/auth/auth.store';
import { PASSWORD_MESSAGE, PASSWORD_PATTERN, applyApiErrors, controlError, optionalSlug } from '../../core/forms/form-errors';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, InputText, Password, Button],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly api = inject(AuthApi);
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly passwordMessage = PASSWORD_MESSAGE;
  readonly form = inject(FormBuilder).nonNullable.group({
    companyName: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [optionalSlug]],
    firstName: ['', [Validators.required, Validators.maxLength(120)]],
    lastName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128), Validators.pattern(PASSWORD_PATTERN)]],
  });

  fieldError(name: 'companyName' | 'slug' | 'firstName' | 'lastName' | 'email' | 'password'): string | null {
    const message = name === 'slug'
      ? 'Use 3–48 lowercase letters, numbers, and hyphens.'
      : name === 'password' ? PASSWORD_MESSAGE : undefined;
    return controlError(this.form.controls[name], message);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    const slug = value.slug.trim();
    this.api.register({
      companyName: value.companyName.trim(),
      slug: slug ? slug : null,
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim(),
      password: value.password,
    }).subscribe({
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
