import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';
import { ApiErrorBody } from '../auth/auth.models';

export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{10,128}$/;
export const PASSWORD_MESSAGE = 'Use at least 10 characters with a letter and a number.';

export function optionalSlug(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '').trim();
  if (!value) {
    return null;
  }
  if (value.length < 3 || value.length > 48 || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(value)) {
    return { pattern: true };
  }
  return null;
}

export function apiMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Something went wrong.';
  }
  const body = error.error as ApiErrorBody | null;
  if (typeof body?.message === 'string' && body.message) {
    return body.message;
  }
  if (error.status === 0) {
    return 'The service is unreachable.';
  }
  return 'Something went wrong.';
}

export function controlError(control: AbstractControl | null, patternMessage = 'Use the requested format.'): string | null {
  if (!control || !control.touched || !control.errors) {
    return null;
  }
  if (control.errors['required']) return 'This field is required.';
  if (control.errors['email']) return 'Enter a valid email address.';
  if (control.errors['minlength']) return `Use at least ${control.errors['minlength'].requiredLength} characters.`;
  if (control.errors['maxlength']) return `Use at most ${control.errors['maxlength'].requiredLength} characters.`;
  if (control.errors['pattern']) return patternMessage;
  if (control.errors['server']) return String(control.errors['server']);
  return 'Check this field.';
}

export function applyApiErrors(form: FormGroup, error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiErrorBody | null;
    body?.fields?.forEach((field) => {
      form.get(field.field)?.setErrors({ server: field.message });
      form.get(field.field)?.markAsTouched();
    });
  }
  return apiMessage(error);
}
