import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Billing } from '../../core/auth/auth.models';
import { apiMessage } from '../../core/forms/form-errors';
import { WorkspaceApi } from '../../core/api/workspace.api';

@Component({
  selector: 'app-billing-page',
  templateUrl: './billing-page.html',
})
export class BillingPage {
  private readonly api = inject(WorkspaceApi);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly billing = signal<Billing | null>(null);

  constructor() {
    this.api.billing().pipe(takeUntilDestroyed()).subscribe({
      next: (billing) => {
        this.billing.set(billing);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiMessage(error));
        this.loading.set(false);
      },
    });
  }
}
